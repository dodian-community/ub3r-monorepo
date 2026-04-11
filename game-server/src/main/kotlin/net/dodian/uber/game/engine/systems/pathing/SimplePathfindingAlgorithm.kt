package net.dodian.uber.game.engine.systems.pathing

import java.util.ArrayDeque

class SimplePathfindingAlgorithm(
    private val collision: PathingCollision = PathingCollision.ALLOW_ALL,
) : PathfindingAlgorithm {
    override fun find(srcX: Int, srcY: Int, dstX: Int, dstY: Int, z: Int): ArrayDeque<Node> {
        val path = ArrayDeque<Node>()
        var x = srcX
        var y = srcY

        while (x != dstX || y != dstY) {
            val stepX = Integer.signum(dstX - x)
            val stepY = Integer.signum(dstY - y)
            val nextX = x + stepX
            val nextY = y + stepY
            if (!collision.traversable(nextX, nextY, z, stepX, stepY)) {
                break
            }

            x = nextX
            y = nextY
            path.addLast(Node(x, y, z))
        }

        return path
    }
}
