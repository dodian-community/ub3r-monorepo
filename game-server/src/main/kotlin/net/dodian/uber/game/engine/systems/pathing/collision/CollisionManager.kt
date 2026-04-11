package net.dodian.uber.game.engine.systems.pathing.collision

class CollisionManager(
    private val matrix: CollisionMatrix = CollisionMatrix(),
) {
    fun flagWall(x: Int, y: Int, z: Int, eastBlocked: Boolean) {
        if (!eastBlocked) {
            return
        }

        wall(x, y, z, CollisionDirection.EAST)
    }

    fun flagSolid(x: Int, y: Int, z: Int, impenetrable: Boolean = true) {
        matrix.flag(x, y, z, CollisionFlag.fullTile(impenetrable))
    }

    fun clearSolid(x: Int, y: Int, z: Int, impenetrable: Boolean = true) {
        matrix.clear(x, y, z, CollisionFlag.fullTile(impenetrable))
    }

    fun flagRouteBlocker(x: Int, y: Int, z: Int) {
        matrix.flag(x, y, z, CollisionFlag.ROUTE_BLOCKER)
    }

    fun clearRouteBlocker(x: Int, y: Int, z: Int) {
        matrix.clear(x, y, z, CollisionFlag.ROUTE_BLOCKER)
    }

    fun wall(x: Int, y: Int, z: Int, direction: CollisionDirection, impenetrable: Boolean = true) {
        tile(x, y, z, setOf(direction), impenetrable)
        tile(x + direction.dx, y + direction.dy, z, setOf(direction.opposite()), impenetrable)
    }

    fun clearWall(x: Int, y: Int, z: Int, direction: CollisionDirection, impenetrable: Boolean = true) {
        clearTile(x, y, z, setOf(direction), impenetrable)
        clearTile(x + direction.dx, y + direction.dy, z, setOf(direction.opposite()), impenetrable)
    }

    fun largeCornerWall(x: Int, y: Int, z: Int, direction: CollisionDirection, impenetrable: Boolean = true) {
        val components = CollisionDirection.diagonalComponents(direction)
        tile(x, y, z, components.toSet(), impenetrable)
        for (component in components) {
            tile(x + component.dx, y + component.dy, z, setOf(component.opposite()), impenetrable)
        }
    }

    fun clearLargeCornerWall(x: Int, y: Int, z: Int, direction: CollisionDirection, impenetrable: Boolean = true) {
        val components = CollisionDirection.diagonalComponents(direction)
        clearTile(x, y, z, components.toSet(), impenetrable)
        for (component in components) {
            clearTile(x + component.dx, y + component.dy, z, setOf(component.opposite()), impenetrable)
        }
    }

    fun tile(x: Int, y: Int, z: Int, directions: Set<CollisionDirection>, impenetrable: Boolean = true) {
        var flags = 0
        for (direction in directions) {
            flags = flags or CollisionFlag.singleDirectionFlag(direction.dx, direction.dy)
            if (impenetrable) {
                flags = flags or CollisionFlag.singleDirectionFlag(direction.dx, direction.dy, projectile = true)
            }
        }
        matrix.flag(x, y, z, flags)
    }

    fun clearTile(x: Int, y: Int, z: Int, directions: Set<CollisionDirection>, impenetrable: Boolean = true) {
        var flags = 0
        for (direction in directions) {
            flags = flags or CollisionFlag.singleDirectionFlag(direction.dx, direction.dy)
            if (impenetrable) {
                flags = flags or CollisionFlag.singleDirectionFlag(direction.dx, direction.dy, projectile = true)
            }
        }
        matrix.clear(x, y, z, flags)
    }

    fun apply(update: CollisionUpdate) {
        matrix.apply(update)
    }

    fun clear() {
        matrix.clearAll()
    }

    fun traversable(x: Int, y: Int, z: Int, dx: Int, dy: Int): Boolean = canStep(x - dx, y - dy, z, dx, dy, 1, 1)

    fun getFlags(x: Int, y: Int, z: Int): Int = matrix.getFlags(x, y, z)

    fun isTileBlocked(x: Int, y: Int, z: Int): Boolean = matrix.hasAllFlags(x, y, z, CollisionFlag.FULL_MOB_BLOCK)

    fun canMove(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        z: Int,
        xLength: Int,
        yLength: Int,
    ): Boolean {
        var currentX = startX
        var currentY = startY

        while (currentX != endX || currentY != endY) {
            val stepX = Integer.compare(endX, currentX)
            val stepY = Integer.compare(endY, currentY)
            val nextX = if (currentX != endX) currentX + stepX else currentX
            val nextY = if (currentY != endY) currentY + stepY else currentY

            if (!canStep(currentX, currentY, z, stepX, stepY, xLength, yLength)) {
                return false
            }

            currentX = nextX
            currentY = nextY
        }

        return true
    }

    private fun canStep(currentX: Int, currentY: Int, z: Int, stepX: Int, stepY: Int, xLength: Int, yLength: Int): Boolean {
        if (stepX == 0 && stepY == 0) {
            return true
        }

        val destinationX = currentX + stepX
        val destinationY = currentY + stepY

        if (stepX != 0 && stepY != 0) {
            if (!canStep(currentX, currentY, z, stepX, 0, xLength, yLength)) {
                return false
            }
            if (!canStep(currentX, currentY, z, 0, stepY, xLength, yLength)) {
                return false
            }
            if (isAnyFullBlocked(destinationX, destinationY, z, xLength, yLength)) {
                return false
            }
            for (dx in 0 until xLength) {
                for (dy in 0 until yLength) {
                    if (isApproachBlocked(destinationX + dx, destinationY + dy, z, stepX, stepY)) {
                        return false
                    }
                }
            }
            return true
        }

        if (isAnyFullBlocked(destinationX, destinationY, z, xLength, yLength)) {
            return false
        }

        when {
            stepX > 0 -> {
                val edgeX = destinationX + xLength - 1
                for (dy in 0 until yLength) {
                    if (isApproachBlocked(edgeX, destinationY + dy, z, stepX, 0)) {
                        return false
                    }
                }
            }

            stepX < 0 -> {
                val edgeX = destinationX
                for (dy in 0 until yLength) {
                    if (isApproachBlocked(edgeX, destinationY + dy, z, stepX, 0)) {
                        return false
                    }
                }
            }

            stepY > 0 -> {
                val edgeY = destinationY + yLength - 1
                for (dx in 0 until xLength) {
                    if (isApproachBlocked(destinationX + dx, edgeY, z, 0, stepY)) {
                        return false
                    }
                }
            }

            else -> {
                val edgeY = destinationY
                for (dx in 0 until xLength) {
                    if (isApproachBlocked(destinationX + dx, edgeY, z, 0, stepY)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun isAnyFullBlocked(baseX: Int, baseY: Int, z: Int, xLength: Int, yLength: Int): Boolean {
        for (dx in 0 until xLength) {
            for (dy in 0 until yLength) {
                val x = baseX + dx
                val y = baseY + dy
                if (matrix.hasAllFlags(x, y, z, CollisionFlag.FULL_MOB_BLOCK)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isApproachBlocked(x: Int, y: Int, z: Int, dx: Int, dy: Int): Boolean {
        val flags = matrix.getFlags(x, y, z)
        return flags and CollisionFlag.approachMask(dx, dy) != 0
    }

    companion object {
        private val GLOBAL = CollisionManager()

        @JvmStatic
        fun global(): CollisionManager = GLOBAL
    }
}
