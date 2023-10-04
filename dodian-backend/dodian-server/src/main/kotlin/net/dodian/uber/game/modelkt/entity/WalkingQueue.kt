package net.dodian.uber.game.modelkt.entity

import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Position
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class WalkingQueue(
    val mob: Mob,
    var running: Boolean = false,
    val points: Deque<Position> = ArrayDeque(),
    val pointsPrevious: Deque<Position> = ArrayDeque()
) {
    val size: Int get() = points.size

    fun isNotEmpty() = size > 0
    fun isEmpty() = size <= 0

    fun addFirstStep(next: Position) {
        points.clear()
        running = false

        val backtrack: Queue<Position> = ArrayDeque()

        while (pointsPrevious.isNotEmpty()) {
            val position = pointsPrevious.pollLast()
            backtrack.add(position)

            if (position == next) {
                backtrack.forEach(this::addStep)
                pointsPrevious.clear()
                return
            }
        }

        pointsPrevious.clear()
        addStep(next)
    }

    fun addStep(next: Position) {
        val current = points.peekLast() ?: mob.position
        addStep(current, next)
    }

    fun addStep(current: Position, next: Position) {
        val nextX = next.x
        val nextY = next.y
        val nextHeight = next.height

        var deltaX = nextX - current.x
        var deltaY = nextY - current.y

        val max = max(abs(deltaX), abs(deltaY))

        val repository = mob.world.regions
        var region = repository.fromPosition(current)

        for (count in 0 until max) {
            if (deltaX < 0) deltaX++
            else if (deltaX > 0) deltaX--

            if (deltaY < 0) deltaY++
            else if (deltaY > 0) deltaY--

            val step = Position(nextX - deltaX, nextY - deltaY, nextHeight)
            if (!region.contains(step))
                region = repository.fromPosition(step)

            points.add(step)
        }
    }

    fun clear() {
        points.clear()
        running = false
        pointsPrevious.clear()
    }

    fun pulse() {
        var position = mob.position
        val height = position.height

        var firstDirection = Direction.NONE
        var secondDirection = Direction.NONE

        val world = mob.world
        val collisionManager = world.collisionManager

        points.poll().also {
            var next: Position = it ?: return@also

            firstDirection = Direction.between(position, next)

            if (!collisionManager.isTraversable(position, EntityType.NPC, firstDirection)) {
                clear()
                firstDirection = Direction.NONE
            } else {
                pointsPrevious.add(next)
                position = Position(next.x, next.y, height)
                mob.lastDirection = firstDirection

                if (!running) return@also
                next = points.poll() ?: return@also

                secondDirection = Direction.between(position, next)

                if (!collisionManager.isTraversable(position, EntityType.NPC, secondDirection)) {
                    clear()
                    secondDirection = Direction.NONE
                } else {
                    pointsPrevious.add(next)
                    position = Position(next.x, next.y, height)
                    mob.lastDirection = secondDirection
                }
            }
        }

        mob.setDirections(firstDirection, secondDirection)
        mob.position = position
    }
}