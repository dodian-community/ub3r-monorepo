package net.dodian.uber.game.model.player.skills.slayer

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Range

object SlayerTask {
    enum class slayerTasks(
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
            fun getSlayerNpc(npcId: Int): slayerTasks? =
                values().firstOrNull { task -> task.npcId.any { it == npcId } }

            @JvmStatic
            fun getTask(slot: Int): slayerTasks? = values().getOrNull(slot)
        }
    }

    private val mazchna = arrayOf(
        slayerTasks.CRAWLING_HAND, slayerTasks.PYREFIENDS, slayerTasks.DEATH_SPAWN,
        slayerTasks.JELLY, slayerTasks.HEAD_MOURNER, slayerTasks.HILL_GIANT,
        slayerTasks.CHAOS_DWARF, slayerTasks.LESSER_DEMON, slayerTasks.ICE_GIANT,
        slayerTasks.BERSERK_BARBARIAN_SPIRIT, slayerTasks.MITHRIL_DRAGON, slayerTasks.SKELE_HELLHOUNDS,
        slayerTasks.FIRE_GIANTS, slayerTasks.BLOODVELD,
    )

    private val vannaka = arrayOf(
        slayerTasks.GREATER_DEMON, slayerTasks.BLACK_DEMON, slayerTasks.BERSERK_BARBARIAN_SPIRIT,
        slayerTasks.MITHRIL_DRAGON, slayerTasks.TZHAAR, slayerTasks.MUMMY,
        slayerTasks.ABYSSAL_DEMONS, slayerTasks.GREEN_DRAGONS, slayerTasks.BLUE_DRAGONS,
        slayerTasks.GARGOYLES, slayerTasks.BLOODVELD, slayerTasks.ABERRANT_SPECTRE,
    )

    private val duradel = arrayOf(
        slayerTasks.DAD, slayerTasks.SAN_TOJALON, slayerTasks.BLACK_KNIGHT_TITAN,
        slayerTasks.JUNGLE_DEMON, slayerTasks.BLACK_DEMON, slayerTasks.UNGADULU,
        slayerTasks.ICE_QUEEN, slayerTasks.NECHRYAEL, slayerTasks.KING_BLACK_DRAGON,
        slayerTasks.DAGANNOTH_PRIME, slayerTasks.HEAD_MOURNER, slayerTasks.ABYSSAL_GUARDIAN,
    )

    @JvmStatic
    fun mazchnaTasks(c: Client): ArrayList<slayerTasks> = collectTasks(c, mazchna) { task ->
        when (task) {
            slayerTasks.HEAD_MOURNER -> c.determineCombatLevel() >= 80 || c.getLevel(Skill.MAGIC) >= 80 || c.getLevel(Skill.RANGED) >= 80
            slayerTasks.LESSER_DEMON -> !c.checkItem(2383) && !c.checkItem(989)
            slayerTasks.SKELE_HELLHOUNDS -> !c.checkItem(2382) && !c.checkItem(989)
            slayerTasks.FIRE_GIANTS -> c.checkItem(1543)
            else -> true
        }
    }

    @JvmStatic
    fun vannakaTasks(c: Client): ArrayList<slayerTasks> = collectTasks(c, vannaka) { task ->
        task != slayerTasks.MUMMY || c.checkItem(1544)
    }

    @JvmStatic
    fun duradelTasks(c: Client): ArrayList<slayerTasks> = collectTasks(c, duradel) { task ->
        when (task) {
            slayerTasks.HEAD_MOURNER -> c.determineCombatLevel() >= 80 || c.getLevel(Skill.MAGIC) >= 80 || c.getLevel(Skill.RANGED) >= 80
            slayerTasks.SAN_TOJALON, slayerTasks.BLACK_KNIGHT_TITAN -> c.checkItem(1544)
            slayerTasks.JUNGLE_DEMON -> c.checkItem(1545)
            slayerTasks.BLACK_DEMON -> c.checkItem(989)
            else -> true
        }
    }

    @JvmStatic
    fun sendTask(client: Client) {
        val checkTask = slayerTasks.getTask(client.slayerData[1])
        if (checkTask != null && client.slayerData[3] > 0) {
            client.send(SendMessage("You need to kill ${client.slayerData[3]} more of ${checkTask.textRepresentation} <col=FF0000>|</col> Current streak is ${client.slayerData[4]}."))
        } else {
            client.send(SendMessage("You need to be assigned a task!"))
        }
    }

    private inline fun collectTasks(
        client: Client,
        tasks: Array<slayerTasks>,
        allow: (slayerTasks) -> Boolean,
    ): ArrayList<slayerTasks> {
        val slayer = ArrayList<slayerTasks>()
        var index = 0
        while (index < tasks.size) {
            val task = tasks[index]
            val slayerLevel = client.getLevel(Skill.SLAYER)
            if (client.slayerData[1] != -1 && client.slayerData[1] == task.ordinal) {
                index++
            } else if (task.assignedLevelRange.floor <= slayerLevel && slayerLevel <= task.assignedLevelRange.ceiling && allow(task)) {
                slayer.add(task)
                if (task == slayerTasks.LESSER_DEMON || task == slayerTasks.SKELE_HELLHOUNDS) {
                    slayer.add(task)
                }
            }
            index++
        }
        return slayer
    }
}
