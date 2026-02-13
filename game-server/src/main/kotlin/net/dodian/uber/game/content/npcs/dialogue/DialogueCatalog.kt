package net.dodian.uber.game.content.npcs.dialogue

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds

data class DialogueCatalogEntry(
    val module: String,
    val entryDialogueId: Int,
    val npcIds: Set<Int>
)

object DialogueCatalog {
    val entries: List<DialogueCatalogEntry> = listOf(
        DialogueCatalogEntry("MageArenaDialogueModule", DialogueIds.Legacy.MAGE_ARENA_OPTIONS, setOf(1597)),
        DialogueCatalogEntry("WatcherDialogueModule", DialogueIds.Legacy.WATCHER_MESSAGE, setOf(804)),
        DialogueCatalogEntry("GamblerDialogueModule", DialogueIds.Legacy.GAMBLER_GREETING, setOf(659)),
        DialogueCatalogEntry("SettingsDialogueModule", DialogueIds.Legacy.TOGGLE_SPECIALS, emptySet()),
        DialogueCatalogEntry("BrimhavenEntryDialogueModule", DialogueIds.Misc.BRIMHAVEN_ENTRY, emptySet()),
        DialogueCatalogEntry("HolidayEventDialogueModule", DialogueIds.Misc.HOLIDAY_GREETING, setOf(8051)),
        DialogueCatalogEntry("RockshellDialogueModule", DialogueIds.Misc.ROCKSHELL_MENU, emptySet()),
        DialogueCatalogEntry("PyramidPlunderDialogueModule", DialogueIds.Misc.PYRAMID_EXIT, emptySet())
    )
}
