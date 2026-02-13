package net.dodian.uber.game.content.dialogue.text

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client
import java.util.ArrayDeque
import java.util.Collections
import java.util.WeakHashMap

object DialoguePagingService {

    private sealed interface PagingState {
        val remainingPages: ArrayDeque<List<String>>
        fun renderPage(client: Client, page: List<String>)
    }

    private data class NpcPagingState(
        val npcId: Int,
        val emote: Int,
        override val remainingPages: ArrayDeque<List<String>>,
    ) : PagingState {
        override fun renderPage(client: Client, page: List<String>) {
            DialogueUi.showNpcChat(client, npcId, emote, page.toTypedArray())
        }
    }

    private data class PlayerPagingState(
        val emote: Int,
        override val remainingPages: ArrayDeque<List<String>>,
    ) : PagingState {
        override fun renderPage(client: Client, page: List<String>) {
            DialogueUi.showPlayerChat(client, page.toTypedArray(), emote)
        }
    }

    private val states = Collections.synchronizedMap(WeakHashMap<Client, PagingState>())

    @JvmStatic
    fun clear(client: Client) {
        states.remove(client)
    }

    @JvmStatic
    fun showNpcChat(client: Client, npcId: Int, emote: Int, text: String) {
        val pages = DialogueTextPager.paginate(text)
        val first = pages.firstOrNull() ?: listOf("")
        DialogueUi.showNpcChat(client, npcId, emote, first.toTypedArray())

        if (pages.size <= 1) {
            clear(client)
            return
        }

        val remaining = ArrayDeque<List<String>>(pages.drop(1))
        states[client] = NpcPagingState(npcId, emote, remaining)
    }

    @JvmStatic
    fun showPlayerChat(client: Client, emote: Int, text: String) {
        val pages = DialogueTextPager.paginate(text)
        val first = pages.firstOrNull() ?: listOf("")
        DialogueUi.showPlayerChat(client, first.toTypedArray(), emote)

        if (pages.size <= 1) {
            clear(client)
            return
        }

        val remaining = ArrayDeque<List<String>>(pages.drop(1))
        states[client] = PlayerPagingState(emote, remaining)
    }

    /**
     * @return true if a paged dialogue was active and advanced.
     */
    @JvmStatic
    fun advance(client: Client): Boolean {
        val state = states[client] ?: return false
        val next = state.remainingPages.pollFirst()
        if (next == null) {
            clear(client)
            return false
        }
        state.renderPage(client, next)
        if (state.remainingPages.isEmpty()) {
            clear(client)
        }
        return true
    }
}
