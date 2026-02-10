package net.dodian.uber.game.content.npcs.dialogue

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds

data class DialogueCatalogEntry(
    val module: String,
    val entryDialogueId: Int,
    val npcIds: Set<Int>
)

object DialogueCatalog {
    val entries: List<DialogueCatalogEntry> = listOf(
        DialogueCatalogEntry("BankingDialogueModule", DialogueIds.Legacy.BANK_GREETING, setOf(394, 395, 7677)),
        DialogueCatalogEntry("RuneShopDialogueModule", DialogueIds.Legacy.RUNE_SHOP_GREETING, setOf(637)),
        DialogueCatalogEntry("MageArenaDialogueModule", DialogueIds.Legacy.MAGE_ARENA_OPTIONS, setOf(1597)),
        DialogueCatalogEntry("WatcherDialogueModule", DialogueIds.Legacy.WATCHER_MESSAGE, setOf(804)),
        DialogueCatalogEntry("GamblerDialogueModule", DialogueIds.Legacy.GAMBLER_GREETING, setOf(659)),
        DialogueCatalogEntry("SettingsDialogueModule", DialogueIds.Legacy.TOGGLE_SPECIALS, emptySet()),
        DialogueCatalogEntry("CarpetDialogueModule", DialogueIds.Carpet.GREETING, setOf(17, 19, 20, 22)),
        DialogueCatalogEntry("AppearanceDialogueModule", DialogueIds.Appearance.MAKEOVER_GREETING, setOf(1306, 1307)),
        DialogueCatalogEntry("SlayerDialogueModule", DialogueIds.Slayer.INTRO, setOf(402, 403, 405)),
        DialogueCatalogEntry("AgilityDialogueModule", DialogueIds.Agility.TICKET_GREETING, setOf(6080)),
        DialogueCatalogEntry("HerbloreDialogueModule", DialogueIds.Herblore.DECANT_GREETING, setOf(1174)),
        DialogueCatalogEntry("HerbloreDialogueModule", DialogueIds.Herblore.HERBALIST_GREETING, setOf(4753)),
        DialogueCatalogEntry("DungeonAccessDialogueModule", DialogueIds.Dungeon.DUNGEON_ENTRY_GREETING, setOf(2345)),
        DialogueCatalogEntry("DungeonAccessDialogueModule", DialogueIds.Dungeon.CAVE_ENTRY_GREETING, setOf(2180)),
        DialogueCatalogEntry("BrimhavenEntryDialogueModule", DialogueIds.Misc.BRIMHAVEN_ENTRY, emptySet()),
        DialogueCatalogEntry("BoatTravelDialogueModule", DialogueIds.Misc.BOAT_GREETING, setOf(3648)),
        DialogueCatalogEntry("BattlestaffDialogueModule", DialogueIds.Misc.BATTLESTAFF_GREETING, setOf(3837)),
        DialogueCatalogEntry("MaxCapeDialogueModule", DialogueIds.Misc.MAX_CAPE_GREETING, setOf(6481)),
        DialogueCatalogEntry("HolidayEventDialogueModule", DialogueIds.Misc.HOLIDAY_GREETING, setOf(8051)),
        DialogueCatalogEntry("RockshellDialogueModule", DialogueIds.Misc.ROCKSHELL_MENU, emptySet()),
        DialogueCatalogEntry("PyramidPlunderDialogueModule", DialogueIds.Misc.PYRAMID_EXIT, emptySet())
    )
}
