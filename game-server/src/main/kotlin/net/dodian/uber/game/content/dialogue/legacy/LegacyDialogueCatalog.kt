package net.dodian.uber.game.content.dialogue.legacy

import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds

data class LegacyDialogueCatalogEntry(
    val module: String,
    val entryDialogueId: Int,
    val npcIds: Set<Int>
)

object LegacyDialogueCatalog {
    val entries: List<LegacyDialogueCatalogEntry> = listOf(
        LegacyDialogueCatalogEntry("UnknownNpc1597", DialogueIds.Legacy.MAGE_ARENA_OPTIONS, setOf(1597)),
        LegacyDialogueCatalogEntry("Watcher", DialogueIds.Legacy.WATCHER_MESSAGE, setOf(804)),
        LegacyDialogueCatalogEntry("PartyPete", DialogueIds.Legacy.GAMBLER_GREETING, setOf(659)),
        LegacyDialogueCatalogEntry("SettingsDialogueModule", DialogueIds.Legacy.TOGGLE_SPECIALS, emptySet()),
        LegacyDialogueCatalogEntry("BrimhavenEntryDialogueModule", DialogueIds.Misc.BRIMHAVEN_ENTRY, emptySet()),
        LegacyDialogueCatalogEntry("DukeHoracio", DialogueIds.Misc.HOLIDAY_GREETING, setOf(8051)),
        LegacyDialogueCatalogEntry("RockshellDialogueModule", DialogueIds.Misc.ROCKSHELL_MENU, emptySet()),
        LegacyDialogueCatalogEntry("PyramidPlunderDialogueModule", DialogueIds.Misc.PYRAMID_EXIT, emptySet())
    )
}
