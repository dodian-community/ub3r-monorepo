package net.dodian.uber.game.engine.systems.skills.herblore

import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

/** Per-player selection state for the herb-cleaning and unfinished-potion menus. */
class HerbloreDialogueState {
    var page: Int = -1
    val options = ArrayList<PartyRoomRewardItem>()

    fun clear() {
        page = -1
        options.clear()
    }
}

object HerbloreDialogueService {
    @JvmStatic
    fun clear(client: Client) {
        client.herbloreDialogueState.clear()
    }

    @JvmStatic
    fun showOptions(client: Client) {
        val state = client.herbloreDialogueState
        if (state.options.isEmpty()) {
            state.page = -1
            DialogueService.setNextDialogueId(client, -1)
            return
        }
        val page = state.page.coerceAtLeast(0)
        val text = Array(if (state.options.size < 4) state.options.size + 2 else if (state.options.size - page <= 3) state.options.size - page + 2 else 6) { "" }
        text[0] = "What do you wish me to do?"
        val shown = minOf(3, state.options.size - page)
        repeat(shown) { index -> text[index + 1] = client.getItemName(state.options[page + index].getId()) }
        text[shown + 1] = if (text.size < 6 && page == 0) "Close" else if (text.size == 6) "Next" else "Previous"
        if (text.size == 6) text[shown + 2] = if (page == 0) "Close" else "Previous"
        client.showPlayerOption(text)
    }
}
