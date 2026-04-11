package net.dodian.uber.game.engine.systems.pathing

import java.util.ArrayDeque

interface PathfindingAlgorithm {
    fun find(srcX: Int, srcY: Int, dstX: Int, dstY: Int, z: Int): ArrayDeque<Node>
}

fun interface PathingCollision {
    fun traversable(x: Int, y: Int, z: Int, dx: Int, dy: Int): Boolean

    companion object {
        val ALLOW_ALL: PathingCollision = PathingCollision { _, _, _, _, _ -> true }
    }
}
