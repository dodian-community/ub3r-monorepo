package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.content.dialogue.text.DialoguePagingService
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
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

    private val sessions = Collections.synchronizedMap(WeakHashMap<Client, DialogueSession>())

    @JvmStatic
    fun start(client: Client, builder: DialogueFactory.() -> Unit) {
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
        if (closeInterfaces) {
            client.send(RemoveInterfaces())
        }
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

    private fun clearLegacyDialogueState(client: Client) {
        client.NpcDialogue = 0
        client.NpcDialogueSend = false
        client.nextDiag = -1
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
