package net.dodian.cache.`object`

import java.util.concurrent.ConcurrentHashMap

class GameObjectData(
    val id: Int,
    var name: String,
    val description: String,
    val sizeX: Int,
    val sizeY: Int,
    private val solid: Boolean,
    private val walkable: Boolean,
    private val hasActionsFlag: Boolean,
    private val unknownValue: Boolean,
    private val walkType: Int,
) {
    fun unknown(): Boolean = unknownValue
    fun getSizeX(rotation: Int): Int = if (rotation == 1 || rotation == 3) sizeY else sizeX
    fun getSizeY(rotation: Int): Int = if (rotation == 1 || rotation == 3) sizeX else sizeY
    fun isSolid(): Boolean = solid
    fun isWalkable(): Boolean = walkable
    fun hasActions(): Boolean = hasActionsFlag
    fun getBiggestSize(): Int = maxOf(sizeX, sizeY)
    fun isRangeAble(): Boolean = walkType <= 1 || (walkType == 2 && !solid)
    fun canShootThru(): Boolean = !solid

    companion object {
        private const val MAX_DYNAMIC_DEFINITIONS = 200_000
        private val definitions = ConcurrentHashMap<Int, GameObjectData>()

        @JvmStatic
        fun init() {
            // Transitional compatibility surface; no legacy cache bootstrap.
        }

        @JvmStatic
        fun addDefinition(def: GameObjectData?) {
            if (def == null || def.id < 0 || def.id > MAX_DYNAMIC_DEFINITIONS) {
                return
            }
            definitions[def.id] = def
        }

        @JvmStatic
        fun forId(id: Int): GameObjectData {
            if (id < 0) {
                return fallback(id)
            }
            return definitions.computeIfAbsent(id) { fallback(it) }
        }

        private fun fallback(id: Int): GameObjectData {
            return GameObjectData(
                id = id,
                name = "Object: #$id",
                description = "Legacy cache removed; fallback definition.",
                sizeX = 1,
                sizeY = 1,
                solid = false,
                walkable = true,
                hasActionsFlag = true,
                unknownValue = false,
                walkType = 2,
            )
        }
    }
}
