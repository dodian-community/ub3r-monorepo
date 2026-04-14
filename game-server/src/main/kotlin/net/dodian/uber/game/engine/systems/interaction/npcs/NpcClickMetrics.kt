package net.dodian.uber.game.engine.systems.interaction.npcs

/**
 * Legacy no-op shim retained only to keep package structure stable while the
 * old NPC click metrics plumbing is phased out.
 */
object NpcClickMetrics {
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun count(key: String): Long = 0L

    @JvmStatic
    fun clearForTests() {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordDecoded(opcode: Int, option: Int, npcIndex: Int, playerName: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordScheduled(opcode: Int, option: Int, npcId: Int, npcIndex: Int, playerName: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordRejected(reason: String, opcode: Int, option: Int, npcIndex: Int, playerName: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordWait(reason: String, option: Int, npcId: Int, npcIndex: Int, playerName: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordDispatch(option: Int, npcId: Int, handled: Boolean, handlerName: String?, playerName: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordQueueStale(playerName: String, intentType: String) {
    }
}
