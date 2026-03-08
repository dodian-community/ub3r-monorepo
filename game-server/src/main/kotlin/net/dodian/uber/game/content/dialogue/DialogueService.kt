package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.content.dialogue.text.DialoguePagingService
import net.dodian.uber.game.content.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
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

    private data class LegacyDialogueState(
        var dialogueId: Int = 0,
        var npcId: Int = 0,
        var nextDialogueId: Int = -1,
        var sent: Boolean = false,
    )

    private val sessions = Collections.synchronizedMap(WeakHashMap<Client, DialogueSession>())
    private val legacyStates = Collections.synchronizedMap(WeakHashMap<Client, LegacyDialogueState>())

    @JvmStatic
    fun hasActiveSession(client: Client): Boolean = sessions.containsKey(client)

    @JvmStatic
    fun hasBlockingDialogue(client: Client): Boolean {
        syncLegacyStateFromClient(client)
        return hasActiveSession(client) ||
            DialoguePagingService.hasActivePaging(client) ||
            hasActiveLegacyDialogue(client)
    }

    @JvmStatic
    fun flushLegacyIfNeeded(client: Client) {
        if (hasActiveSession(client)) {
            return
        }
        syncLegacyStateFromClient(client)
        if (legacyDialogueId(client) > 0 && !isLegacyDialogueSent(client)) {
            DialogueDisplayService.updateNpcChat(client)
        }
    }

    @JvmStatic
    fun start(client: Client, builder: DialogueFactory.() -> Unit) {
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.DIALOGUE_OPENED,
            fullResetAnimation = false,
            resetLegacyState = true,
        )
        clear(client, closeInterfaces = false)
        clearLegacyDialogueState(client)

        val factory = DialogueFactory().apply(builder)
        val session = DialogueSession(queue = ArrayDeque(factory.steps))
        sessions[client] = session
        execute(client, session)
    }

    @JvmStatic
    fun clear(client: Client, closeInterfaces: Boolean = true) {
        sessions.remove(client)
        DialoguePagingService.clear(client)
        clearLegacyDialogueState(client)
        if (closeInterfaces) {
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun closeBlockingDialogue(client: Client, closeInterfaces: Boolean = true) {
        val legacyId = legacyDialogueId(client)
        sessions.remove(client)
        DialoguePagingService.clear(client)
        clearLegacyDialogueState(client)
        if (legacyId == 1001) {
            client.clearWalkableInterface()
        }
        if (closeInterfaces) {
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun startLegacy(client: Client, dialogueId: Int, npcId: Int) {
        sessions.remove(client)
        DialoguePagingService.clear(client)
        setLegacyDialogueId(client, dialogueId)
        setLegacyNpcTalkTo(client, npcId)
        setLegacyDialogueSent(client, false)
        setLegacyNextDialogueId(client, -1)
        DialogueDisplayService.updateNpcChat(client)
    }

    @JvmStatic
    fun captureLegacyBridgeState(client: Client) {
        syncLegacyStateFromClient(client)
    }

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
    fun onLegacyContinue(client: Client): Boolean {
        syncLegacyStateFromClient(client)
        val dialogueId = legacyDialogueId(client)
        if (dialogueId <= 0 && legacyNextDialogueId(client) <= 0) {
            return false
        }

        when (dialogueId) {
            1, 3, 5, 21 -> {
                setLegacyDialogueId(client, dialogueId + 1)
                setLegacyDialogueSent(client, false)
            }

            6, 7 -> {
                clearLegacyDialogueState(client)
                client.send(RemoveInterfaces())
            }

            23 -> {
                setLegacyDialogueId(client, dialogueId + 2)
                setLegacyDialogueSent(client, false)
            }

            else -> {
                val nextDialogueId = legacyNextDialogueId(client)
                if (nextDialogueId > 0) {
                    setLegacyDialogueId(client, nextDialogueId)
                    setLegacyDialogueSent(client, false)
                    setLegacyNextDialogueId(client, -1)
                } else if (dialogueId != 48054) {
                    clearLegacyDialogueState(client)
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
    fun legacyDialogueId(client: Client): Int {
        syncLegacyStateFromClient(client)
        return legacyStates[client]?.dialogueId ?: 0
    }

    @JvmStatic
    fun setLegacyDialogueId(client: Client, dialogueId: Int) {
        legacyState(client).dialogueId = dialogueId
        client.NpcDialogue = dialogueId
    }

    @JvmStatic
    fun legacyNpcTalkTo(client: Client): Int {
        syncLegacyStateFromClient(client)
        return legacyStates[client]?.npcId ?: 0
    }

    @JvmStatic
    fun setLegacyNpcTalkTo(client: Client, npcId: Int) {
        legacyState(client).npcId = npcId
        client.NpcTalkTo = npcId
    }

    @JvmStatic
    fun legacyNextDialogueId(client: Client): Int {
        syncLegacyStateFromClient(client)
        return legacyStates[client]?.nextDialogueId ?: -1
    }

    @JvmStatic
    fun setLegacyNextDialogueId(client: Client, dialogueId: Int) {
        legacyState(client).nextDialogueId = dialogueId
        client.nextDiag = dialogueId
    }

    @JvmStatic
    fun isLegacyDialogueSent(client: Client): Boolean {
        syncLegacyStateFromClient(client)
        return legacyStates[client]?.sent == true
    }

    @JvmStatic
    fun setLegacyDialogueSent(client: Client, sent: Boolean) {
        legacyState(client).sent = sent
        client.NpcDialogueSend = sent
    }

    @JvmStatic
    fun hasActiveLegacyDialogue(client: Client): Boolean {
        syncLegacyStateFromClient(client)
        val state = legacyStates[client] ?: return false
        return state.dialogueId > 0 || state.nextDialogueId > 0 || state.sent
    }

    private fun clearLegacyDialogueState(client: Client) {
        legacyStates.remove(client)
        client.NpcDialogue = 0
        client.NpcTalkTo = 0
        client.NpcDialogueSend = false
        client.nextDiag = -1
    }

    private fun legacyState(client: Client): LegacyDialogueState {
        syncLegacyStateFromClient(client)
        return legacyStates.getOrPut(client) { LegacyDialogueState() }
    }

    private fun syncLegacyStateFromClient(client: Client) {
        val hasClientState =
            client.NpcDialogue != 0 ||
                client.NpcTalkTo != 0 ||
                client.nextDiag != -1 ||
                client.NpcDialogueSend
        if (!hasClientState) {
            if ((legacyStates[client]?.dialogueId ?: 0) == 0 &&
                (legacyStates[client]?.nextDialogueId ?: -1) <= 0 &&
                legacyStates[client]?.sent != true
            ) {
                legacyStates.remove(client)
            }
            return
        }

        val state = legacyStates.getOrPut(client) { LegacyDialogueState() }
        state.dialogueId = client.NpcDialogue
        state.npcId = client.NpcTalkTo
        state.nextDialogueId = client.nextDiag
        state.sent = client.NpcDialogueSend
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
