package net.dodian.uber.game.content.skills.slayer

import net.dodian.utilities.Range

enum class SlayerTaskDefinition(
    val textRepresentation: String,
    val slayerOnly: Boolean,
    val assignedLevelRange: Range,
    val assignedAmountRange: Range,
    vararg val npcId: Int,
) {
    CRAWLING_HAND("Crawling Hands", true, Range(1, 40), Range(20, 100), 448, 449),
    PYREFIENDS("Pyrefiends", true, Range(20, 60), Range(20, 60), 433),
    DEATH_SPAWN("Death Spawns", true, Range(30, 60), Range(20, 60), 10),
    JELLY("Jellies", true, Range(40, 75), Range(30, 70), 437),
    HEAD_MOURNER("Head Mourners", true, Range(45, 200), Range(10, 30), 5311),
    HILL_GIANT("Hill Giants", false, Range(1, 50), Range(20, 40), 2098),
    CHAOS_DWARF("Chaos Dwarves", true, Range(50, 200), Range(20, 40), 291),
    LESSER_DEMON("Lesser Demon", true, Range(50, 200), Range(30, 80), 2005),
    FIRE_GIANTS("Fire Giants", false, Range(1, 200), Range(30, 80), 2075),
    MUMMY("Mummy", true, Range(1, 200), Range(30, 80), 950),
    ICE_GIANT("Ice Giants", false, Range(30, 60), Range(30, 50), 2085),
    DRUID("Druids", false, Range(1, 30), Range(20, 35), 3098),
    GREATER_DEMON("Greater Demon", true, Range(55, 200), Range(30, 60), 2025),
    BERSERK_BARBARIAN_SPIRIT("Berserk Barbarian Spirits", true, Range(70, 200), Range(25, 60), 5565),
    MITHRIL_DRAGON("Mithril Dragons", true, Range(83, 200), Range(10, 25), 2919),
    BLOODVELD("Bloodveld", true, Range(53, 93), Range(25, 60), 484),
    GARGOYLES("Gargoyles", true, Range(63, 200), Range(25, 60), 412),
    ABERRANT_SPECTRE("Aberrant spectre", true, Range(73, 200), Range(25, 60), 2),
    SKELE_HELLHOUNDS("Skeleton HellHound", true, Range(50, 200), Range(30, 80), 5054),
    TZHAAR("TzHaar-Ket", true, Range(80, 200), Range(30, 60), 2173),
    ABYSSAL_DEMONS("Abyssal Demons", true, Range(85, 200), Range(30, 60), 415),
    GREEN_DRAGONS("Green Dragons", false, Range(50, 200), Range(30, 60), 260),
    BLUE_DRAGONS("Blue Dragons", false, Range(50, 200), Range(30, 60), 265),
    DAD("Dad", false, Range(1, 200), Range(10, 30), 4130),
    SAN_TOJALON("San Tojalon", false, Range(1, 200), Range(10, 30), 3964),
    BLACK_KNIGHT_TITAN("Black knight titan", false, Range(1, 200), Range(10, 30), 4067),
    JUNGLE_DEMON("Jungle demon", false, Range(1, 200), Range(10, 30), 1443),
    BLACK_DEMON("Black Demon", true, Range(60, 200), Range(10, 30), 1432),
    DAGANNOTH_PRIME("Dagannoth prime", false, Range(86, 200), Range(12, 24), 2266),
    UNGADULU("Ungadulu", false, Range(1, 200), Range(10, 30), 3957),
    ICE_QUEEN("Ice queen", false, Range(1, 200), Range(10, 30), 4922),
    NECHRYAEL("Nechryael", false, Range(1, 200), Range(10, 30), 8),
    KING_BLACK_DRAGON("King black dragon", false, Range(1, 200), Range(10, 30), 239),
    ABYSSAL_GUARDIAN("Abyssal guardian", false, Range(1, 200), Range(10, 30), 2585);

    companion object {
        @JvmStatic
        fun forNpc(npcId: Int): SlayerTaskDefinition? = values().firstOrNull { task -> task.npcId.any { it == npcId } }

        @JvmStatic
        fun forOrdinal(slot: Int): SlayerTaskDefinition? = values().getOrNull(slot)
    }
}

object SlayerData {
    val mazchna: Array<SlayerTaskDefinition> =
        arrayOf(
            SlayerTaskDefinition.CRAWLING_HAND,
            SlayerTaskDefinition.PYREFIENDS,
            SlayerTaskDefinition.DEATH_SPAWN,
            SlayerTaskDefinition.JELLY,
            SlayerTaskDefinition.HEAD_MOURNER,
            SlayerTaskDefinition.HILL_GIANT,
            SlayerTaskDefinition.CHAOS_DWARF,
            SlayerTaskDefinition.LESSER_DEMON,
            SlayerTaskDefinition.ICE_GIANT,
            SlayerTaskDefinition.BERSERK_BARBARIAN_SPIRIT,
            SlayerTaskDefinition.MITHRIL_DRAGON,
            SlayerTaskDefinition.SKELE_HELLHOUNDS,
            SlayerTaskDefinition.FIRE_GIANTS,
            SlayerTaskDefinition.BLOODVELD,
        )

    val vannaka: Array<SlayerTaskDefinition> =
        arrayOf(
            SlayerTaskDefinition.GREATER_DEMON,
            SlayerTaskDefinition.BLACK_DEMON,
            SlayerTaskDefinition.BERSERK_BARBARIAN_SPIRIT,
            SlayerTaskDefinition.MITHRIL_DRAGON,
            SlayerTaskDefinition.TZHAAR,
            SlayerTaskDefinition.MUMMY,
            SlayerTaskDefinition.ABYSSAL_DEMONS,
            SlayerTaskDefinition.GREEN_DRAGONS,
            SlayerTaskDefinition.BLUE_DRAGONS,
            SlayerTaskDefinition.GARGOYLES,
            SlayerTaskDefinition.BLOODVELD,
            SlayerTaskDefinition.ABERRANT_SPECTRE,
        )

    val duradel: Array<SlayerTaskDefinition> =
        arrayOf(
            SlayerTaskDefinition.DAD,
            SlayerTaskDefinition.SAN_TOJALON,
            SlayerTaskDefinition.BLACK_KNIGHT_TITAN,
            SlayerTaskDefinition.JUNGLE_DEMON,
            SlayerTaskDefinition.BLACK_DEMON,
            SlayerTaskDefinition.UNGADULU,
            SlayerTaskDefinition.ICE_QUEEN,
            SlayerTaskDefinition.NECHRYAEL,
            SlayerTaskDefinition.KING_BLACK_DRAGON,
            SlayerTaskDefinition.DAGANNOTH_PRIME,
            SlayerTaskDefinition.HEAD_MOURNER,
            SlayerTaskDefinition.ABYSSAL_GUARDIAN,
        )
}
