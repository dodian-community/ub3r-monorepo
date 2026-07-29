package net.dodian.uber.skills.slayer

/**
 * Canonical Slayer task table. List order intentionally matches the persisted
 * legacy task ordinal so existing player assignments remain valid.
 */
object SlayerTaskRegistry {
    val tasks = listOf(
        task("Crawling Hands", true, 1, 40, 20, 100, 448, 449), task("Pyrefiends", true, 20, 60, 20, 60, 433),
        task("Death Spawns", true, 30, 60, 20, 60, 10), task("Jellies", true, 40, 75, 30, 70, 437),
        task("Head Mourners", true, 45, 200, 10, 30, 5311), task("Hill Giants", false, 1, 50, 20, 40, 2098),
        task("Chaos Dwarves", true, 50, 200, 20, 40, 291), task("Lesser Demon", true, 50, 200, 30, 80, 2005),
        task("Fire Giants", false, 1, 200, 30, 80, 2075), task("Mummy", true, 1, 200, 30, 80, 950),
        task("Ice Giants", false, 30, 60, 30, 50, 2085), task("Druids", false, 1, 30, 20, 35, 3258),
        task("Greater Demon", true, 55, 200, 30, 60, 2025), task("Berserk Barbarian Spirits", true, 70, 200, 25, 60, 5565),
        task("Mithril Dragons", true, 83, 200, 10, 25, 2919), task("Bloodveld", true, 53, 93, 25, 60, 484),
        task("Gargoyles", true, 63, 200, 25, 60, 412), task("Aberrant spectre", true, 73, 200, 25, 60, 2),
        task("Skeleton HellHound", true, 50, 200, 30, 80, 5054), task("TzHaar-Ket", true, 80, 200, 30, 60, 2173),
        task("Abyssal Demons", true, 85, 200, 30, 60, 415), task("Green Dragons", false, 50, 200, 30, 60, 260),
        task("Blue Dragons", false, 50, 200, 30, 60, 265), task("Dad", false, 1, 200, 10, 30, 4130),
        task("San Tojalon", false, 1, 200, 10, 30, 3964), task("Black knight titan", false, 1, 200, 10, 30, 4067),
        task("Jungle demon", false, 1, 200, 10, 30, 1443), task("Black Demon", true, 60, 200, 10, 30, 1432),
        task("Dagannoth prime", false, 86, 200, 12, 24, 2266), task("Ungadulu", false, 1, 200, 10, 30, 3957),
        task("Ice queen", false, 1, 200, 10, 30, 4922), task("Nechryael", false, 1, 200, 10, 30, 8),
        task("King black dragon", false, 1, 200, 10, 30, 239), task("Abyssal guardian", false, 1, 200, 10, 30, 2585),
    )
    fun forNpc(npcId: Int): SlayerTaskDef? = tasks.firstOrNull { npcId in it.npcIds }
    fun forOrdinal(ordinal: Int): SlayerTaskDef? = tasks.getOrNull(ordinal)
    fun tasksForMaster(player: net.dodian.uber.game.api.plugin.skills.SkillPlayer, masterNpcId: Int): List<SlayerTaskDef> {
        val candidates = when (masterNpcId) {
            402 -> intArrayOf(0,1,2,3,4,5,6,7,10,13,14,18,8,15)
            403 -> intArrayOf(12,27,13,14,19,9,20,21,22,16,15,17)
            405 -> intArrayOf(23,24,25,26,27,29,30,31,32,28,4,33)
            else -> intArrayOf()
        }
        return candidates.toList().mapNotNull(tasks::getOrNull).filter { task ->
            if (player.slayerTask.ordinal == tasks.indexOf(task)) return@filter false
            if (player.skills.current(net.dodian.uber.game.model.player.skills.Skill.SLAYER) !in task.levelMin..task.levelMax) return@filter false
            when (masterNpcId) {
                402 -> when (tasks.indexOf(task)) { 4 -> player.profile.combatLevel >= 80 || player.skills.current(net.dodian.uber.game.model.player.skills.Skill.MAGIC) >= 80 || player.skills.current(net.dodian.uber.game.model.player.skills.Skill.RANGED) >= 80; 7 -> !player.inventory.contains(2383) && !player.inventory.contains(989); 18 -> !player.inventory.contains(2382) && !player.inventory.contains(989); 8 -> player.inventory.contains(1543); else -> true }
                403 -> tasks.indexOf(task) != 9 || player.inventory.contains(1544)
                405 -> when (tasks.indexOf(task)) { 4 -> player.profile.combatLevel >= 80 || player.skills.current(net.dodian.uber.game.model.player.skills.Skill.MAGIC) >= 80 || player.skills.current(net.dodian.uber.game.model.player.skills.Skill.RANGED) >= 80; 24,25 -> player.inventory.contains(1544); 26 -> player.inventory.contains(1545); 27 -> player.inventory.contains(989); else -> true }
                else -> true
            }
        }.flatMap { task -> if (tasks.indexOf(task) in intArrayOf(7,18)) listOf(task, task) else listOf(task) }
    }
    private fun task(name: String, only: Boolean, low: Int, high: Int, min: Int, max: Int, vararg npcs: Int) =
        SlayerTaskDef(name, only, low, high, min, max, npcs.toList())
}
