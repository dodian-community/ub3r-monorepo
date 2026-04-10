package net.dodian.uber.game.systems.pathing.collision

data class CollisionUpdate(
    val x: Int,
    val y: Int,
    val z: Int,
    val flags: Int,
    val width: Int = 1,
    val height: Int = 1,
    val remove: Boolean = false,
)
