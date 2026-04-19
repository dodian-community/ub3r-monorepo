package net.dodian.uber.game.systems.world.npc

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc

object NpcSpawnLocator {
    private data class SpawnKey(val npcId: Int, val x: Int, val y: Int, val z: Int)

    private val GNOME_COURSE_NPCS = listOf(
        SpawnKey(6080, 2475, 3428, 0),
        SpawnKey(6080, 2476, 3423, 1),
        SpawnKey(6080, 2475, 3419, 2),
        SpawnKey(6080, 2485, 3421, 2),
        SpawnKey(6080, 2487, 3423, 0),
        SpawnKey(6080, 2486, 3430, 0),
    )

    private val WEREWOLF_COURSE_NPCS = listOf(
        SpawnKey(5924, 3540, 9873, 0),
        SpawnKey(5926, 3540, 9890, 0),
        SpawnKey(5926, 3537, 9903, 0),
        SpawnKey(5926, 3533, 9908, 0),
        SpawnKey(5926, 3528, 9913, 0),
        SpawnKey(5927, 3527, 9865, 0),
    )

    private val DAGANNOTH_REX = SpawnKey(2267, 3248, 2794, 0)
    private val DAGANNOTH_SUPREME = SpawnKey(2265, 3251, 2794, 0)

    @JvmStatic
    fun gnomeCourseNpc(index: Int): Npc? = npcByKey(GNOME_COURSE_NPCS.getOrNull(index))

    @JvmStatic
    fun werewolfCourseNpc(index: Int): Npc? = npcByKey(WEREWOLF_COURSE_NPCS.getOrNull(index))

    @JvmStatic
    fun dagannothRex(): Npc? = npcByKey(DAGANNOTH_REX)

    @JvmStatic
    fun dagannothSupreme(): Npc? = npcByKey(DAGANNOTH_SUPREME)

    @JvmStatic
    fun dagannothTwinFor(npc: Npc?): Npc? {
        if (npc == null) return null
        return when {
            matches(npc, DAGANNOTH_REX) -> dagannothSupreme()
            matches(npc, DAGANNOTH_SUPREME) -> dagannothRex()
            else -> null
        }
    }

    @JvmStatic
    fun isGnomeCourseNpc(npc: Npc?): Boolean {
        if (npc == null) return false
        return GNOME_COURSE_NPCS.any { matches(npc, it) }
    }

    private fun npcByKey(key: SpawnKey?): Npc? {
        if (key == null) return null
        val manager = Server.npcManager ?: return null
        return manager.findNpcByIdAtPosition(key.npcId, key.x, key.y, key.z)
    }

    private fun matches(npc: Npc, key: SpawnKey): Boolean {
        val pos: Position = npc.position
        return npc.id == key.npcId && pos.x == key.x && pos.y == key.y && pos.z == key.z
    }
}
