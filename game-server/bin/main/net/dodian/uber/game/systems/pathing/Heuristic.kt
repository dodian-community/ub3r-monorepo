package net.dodian.uber.game.systems.pathing

import kotlin.math.abs
import kotlin.math.sqrt

fun interface Heuristic {
    fun estimate(fromX: Int, fromY: Int, toX: Int, toY: Int): Int

    companion object {
        val MANHATTAN: Heuristic = Heuristic { fromX, fromY, toX, toY ->
            abs(toX - fromX) + abs(toY - fromY)
        }

        val EUCLIDEAN: Heuristic = Heuristic { fromX, fromY, toX, toY ->
            val dx = (toX - fromX).toDouble()
            val dy = (toY - fromY).toDouble()
            sqrt(dx * dx + dy * dy).toInt()
        }
    }
}
