package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.content.dialogue.text.DialoguePagingService
import net.dodian.uber.game.content.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.interaction.PlayerInteractionGuardService
import java.util.ArrayDeque
import java.util.Collections
import java.util.WeakHashMap

object DialogueService {

    private enum class Awaiting {
        NONE,
        CONTINUE,
        OPTIONS,
    }

    private data class DialogueSession(
        val queue: ArrayDeque<DialogueStep>,
        var awaiting: Awaiting = Awaiting.NONE,
        var currentOptions: DialogueStep.Options? = null,
    )

    private data class IndexedDialogueState(
        var dialogueId: Int = 0,
        var npcId: Int = 0,
        var nextDialogueId: Int = -1,
        var sent: Boolean = false,
    )

    private val sessions = Collections.synchronizedMap(WeakHashMap<Client, DialogueSession>())
    private val indexedStates = Collections.synchronizedMap(WeakHashMap<Client, IndexedDialogueState>())

    @JvmStatic
    fun hasActiveSession(client: Client): Boolean = sessions.containsKey(client)

    @JvmStatic
    fun hasBlockingDialogue(client: Client): Boolean {
        return hasActiveSession(client) ||
            DialoguePagingService.hasActivePaging(client) ||
            hasIndexedDialogue(client)
    }

    @JvmStatic
    fun flushIndexedIfNeeded(client: Client) {
        if (hasActiveSession(client)) {
            return
        }
        if (currentDialogueId(client) > 0 && !isDialogueSent(client)) {
            DialogueDisplayService.updateNpcChat(client)
        }
    }

    @JvmStatic
    fun start(client: Client, builder: DialogueFactory.() -> Unit) {
        if (!PlayerInteractionGuardService.canStartDialogue(client)) {
            PlayerInteractionGuardService.blockingInteractionMessage(client)?.let { client.send(SendMessage(it)) }
            return
        }
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.DIALOGUE_OPENED,
            fullResetAnimation = false,
            resetCompatibilityState = true,
        )
        clear(client, closeInterfaces = false)
        clearIndexedDialogueState(client)

        val factory = DialogueFactory().apply(builder)
        val session = DialogueSession(queue = ArrayDeque(factory.steps))
        sessions[client] = session
        execute(client, session)
    }

    @JvmStatic
    fun clear(client: Client, closeInterfaces: Boolean = true) {
        sessions.remove(client)
        DialoguePagingService.clear(client)
        clearIndexedDialogueState(client)
        if (closeInterfaces) {
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun closeBlockingDialogue(client: Client, closeInterfaces: Boolean = true) {
        val dialogueId = currentDialogueId(client)
        sessions.remove(client)
        DialoguePagingService.clear(client)
        clearIndexedDialogueState(client)
        if (dialogueId == 1001) {
            client.clearWalkableInterface()
        }
        if (closeInterfaces) {
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun startDialogueId(client: Client, dialogueId: Int, npcId: Int) {
        if (!PlayerInteractionGuardService.canStartDialogue(client)) {
            PlayerInteractionGuardService.blockingInteractionMessage(client)?.let { client.send(SendMessage(it)) }
            return
        }
        sessions.remove(client)
        DialoguePagingService.clear(client)
        setDialogueId(client, dialogueId)
        setActiveNpcId(client, npcId)
        setDialogueSent(client, false)
        setNextDialogueId(client, -1)
        DialogueDisplayService.updateNpcChat(client)
    }

    @JvmStatic
    fun showNpcChat(
        client: Client,
        npcId: Int,
        emote: Int,
        text: Array<String>,
        nextDialogueId: Int? = null,
    ) {
        setActiveNpcId(client, npcId)
        client.showNPCChat(npcId, emote, text)
        setDialogueSent(client, true)
        if (nextDialogueId != null) {
            setNextDialogueId(client, nextDialogueId)
        }
    }

    @JvmStatic
    fun showPlayerChat(
        client: Client,
        text: Array<String>,
        emote: Int,
        nextDialogueId: Int? = null,
    ) {
        client.showPlayerChat(text, emote)
        setDialogueSent(client, true)
        if (nextDialogueId != null) {
            setNextDialogueId(client, nextDialogueId)
        }
    }

    @JvmStatic
    fun resetDialogueState(client: Client) {
        setDialogueId(client, -1)
        setDialogueSent(client, false)
        setNextDialogueId(client, -1)
    }

    @JvmStatic
    fun showCompatNpcChat(
        client: Client,
        npcId: Int,
        emote: Int,
        text: Array<String>,
        nextDialogueId: Int? = null,
    ) = showNpcChat(client, npcId, emote, text, nextDialogueId)

    @JvmStatic
    fun showCompatPlayerChat(
        client: Client,
        text: Array<String>,
        emote: Int,
        nextDialogueId: Int? = null,
    ) = showPlayerChat(client, text, emote, nextDialogueId)

    @JvmStatic
    fun captureCompatibilityState(client: Client) = Unit

    /**
     * @return true if the continue click was consumed (paging or active session).
     */
    @JvmStatic
    fun onContinue(client: Client): Boolean {
        if (DialoguePagingService.advance(client)) {
            return true
        }

        val session = sessions[client] ?: return false
        if (session.awaiting != Awaiting.CONTINUE) return false

        session.awaiting = Awaiting.NONE
        execute(client, session)
        return true
    }

    @JvmStatic
    fun onIndexedContinue(client: Client): Boolean {
        val dialogueId = currentDialogueId(client)
        if (dialogueId <= 0 && nextDialogueId(client) <= 0) {
            return false
        }

        when (dialogueId) {
            1, 3, 5, 21 -> {
                setDialogueId(client, dialogueId + 1)
                setDialogueSent(client, false)
            }

            6, 7 -> {
                clearIndexedDialogueState(client)
                client.send(RemoveInterfaces())
            }

            23 -> {
                setDialogueId(client, dialogueId + 2)
                setDialogueSent(client, false)
            }

            else -> {
                val nextDialogueId = nextDialogueId(client)
                if (nextDialogueId > 0) {
                    setDialogueId(client, nextDialogueId)
                    setDialogueSent(client, false)
                    setNextDialogueId(client, -1)
                } else if (dialogueId != 48054) {
                    clearIndexedDialogueState(client)
                    client.send(RemoveInterfaces())
                }
            }
        }
        return true
    }

    /**
     * @param optionIndex 1-based option index (1..5).
     * @return true if the option click was consumed by an active session.
     */
    @JvmStatic
    fun onOption(client: Client, optionIndex: Int): Boolean {
        val session = sessions[client] ?: return false
        if (session.awaiting != Awaiting.OPTIONS) return false

        val optionsStep = session.currentOptions ?: return false
        val idx = optionIndex - 1
        if (idx !in optionsStep.options.indices) return true // consume invalid clicks

        val chosen = optionsStep.options[idx]
        session.currentOptions = null
        session.awaiting = Awaiting.NONE

        // Prepend chosen branch steps so they execute immediately.
        for (step in chosen.steps.asReversed()) {
            session.queue.addFirst(step)
        }
        execute(client, session)
        return true
    }

    private fun execute(client: Client, session: DialogueSession) {
        while (true) {
            val step = session.queue.pollFirst()
            if (step == null) {
                clear(client, closeInterfaces = true)
                return
            }

            when (step) {
                is DialogueStep.NpcChat -> {
                    DialoguePagingService.showNpcChat(client, step.npcId, step.emote, step.text)
                    session.awaiting = Awaiting.CONTINUE
                    return
                }

                is DialogueStep.PlayerChat -> {
                    DialoguePagingService.showPlayerChat(client, step.emote, step.text)
                    session.awaiting = Awaiting.CONTINUE
                    return
                }

                is DialogueStep.Options -> {
                    val payload = ArrayList<String>(1 + step.options.size)
                    payload.add(step.title)
                    payload.addAll(step.options.map { it.text })
                    DialogueUi.showPlayerOption(client, payload.toTypedArray())
                    session.currentOptions = step
                    session.awaiting = Awaiting.OPTIONS
                    return
                }

                is DialogueStep.Action -> {
                    step.action(client)
                }

                is DialogueStep.Restart -> {
                    session.queue.clear()
                    session.awaiting = Awaiting.NONE
                    session.currentOptions = null
                    for (s in step.steps.asReversed()) {
                        session.queue.addFirst(s)
                    }
                }

                is DialogueStep.Finish -> {
                    step.action(client)
                    clear(client, closeInterfaces = step.closeInterfaces)
                    return
                }
            }
        }
    }

    @JvmStatic
    fun currentDialogueId(client: Client): Int {
        return indexedStates[client]?.dialogueId ?: 0
    }

    @JvmStatic
    fun setDialogueId(client: Client, dialogueId: Int) {
        indexedState(client).dialogueId = dialogueId
        client.NpcDialogue = dialogueId
    }

    @JvmStatic
    fun activeNpcId(client: Client): Int {
        return indexedStates[client]?.npcId ?: 0
    }

    @JvmStatic
    fun setActiveNpcId(client: Client, npcId: Int) {
        indexedState(client).npcId = npcId
        client.NpcTalkTo = npcId
    }

    @JvmStatic
    fun nextDialogueId(client: Client): Int {
        return indexedStates[client]?.nextDialogueId ?: -1
    }

    @JvmStatic
    fun setNextDialogueId(client: Client, dialogueId: Int) {
        indexedState(client).nextDialogueId = dialogueId
        client.nextDiag = dialogueId
    }

    @JvmStatic
    fun isDialogueSent(client: Client): Boolean {
        return indexedStates[client]?.sent == true
    }

    @JvmStatic
    fun setDialogueSent(client: Client, sent: Boolean) {
        indexedState(client).sent = sent
        client.NpcDialogueSend = sent
    }

    @JvmStatic
    fun setCompatDialogueSent(client: Client, sent: Boolean) = setDialogueSent(client, sent)

    @JvmStatic
    fun compatDialogueId(client: Client): Int = currentDialogueId(client)

    @JvmStatic
    fun setCompatDialogueId(client: Client, dialogueId: Int) = setDialogueId(client, dialogueId)

    @JvmStatic
    fun compatNpcId(client: Client): Int = activeNpcId(client)

    @JvmStatic
    fun setCompatNpcId(client: Client, npcId: Int) = setActiveNpcId(client, npcId)

    @JvmStatic
    fun compatNextDialogueId(client: Client): Int = nextDialogueId(client)

    @JvmStatic
    fun setCompatNextDialogueId(client: Client, dialogueId: Int) = setNextDialogueId(client, dialogueId)

    @JvmStatic
    fun isCompatDialogueSent(client: Client): Boolean = isDialogueSent(client)

    @JvmStatic
    fun hasIndexedDialogue(client: Client): Boolean {
        val state = indexedStates[client] ?: return false
        return state.dialogueId > 0 || state.nextDialogueId > 0 || state.sent
    }

    private fun clearIndexedDialogueState(client: Client) {
        indexedStates.remove(client)
        client.NpcDialogue = 0
        client.NpcTalkTo = 0
        client.NpcDialogueSend = false
        client.nextDiag = -1
    }

    private fun indexedState(client: Client): IndexedDialogueState {
        return indexedStates.getOrPut(client) { IndexedDialogueState() }
    }
}

sealed interface DialogueStep {
    data class NpcChat(val npcId: Int, val emote: Int, val text: String) : DialogueStep
    data class PlayerChat(val emote: Int, val text: String) : DialogueStep

    data class Options(
        val title: String,
        val options: List<Option>,
    ) : DialogueStep

    data class Option(
        val text: String,
        val steps: List<DialogueStep>,
    )

    data class Action(val action: (Client) -> Unit) : DialogueStep

    data class Restart(val steps: List<DialogueStep>) : DialogueStep

    data class Finish(
        val closeInterfaces: Boolean,
        val action: (Client) -> Unit,
    ) : DialogueStep
}
