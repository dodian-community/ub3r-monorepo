package net.dodian.uber.game.content.objects

import net.dodian.uber.game.model.Position

data class ObjectCoordinate(val x: Int, val y: Int, val z: Int = 0) {
    fun matches(position: Position): Boolean = x == position.x && y == position.y && z == position.z

    override fun toString(): String = "($x,$y,$z)"
}

sealed interface ObjectPositionMatcher {
    val specificity: Int
    fun matches(position: Position): Boolean
    fun overlaps(other: ObjectPositionMatcher): Boolean
    fun describe(): String

    object Any : ObjectPositionMatcher {
        override val specificity: Int = 0
        override fun matches(position: Position): Boolean = true
        override fun overlaps(other: ObjectPositionMatcher): Boolean = true
        override fun describe(): String = "any"
    }

    data class Exact(val coordinate: ObjectCoordinate) : ObjectPositionMatcher {
        override val specificity: Int = 2
        override fun matches(position: Position): Boolean = coordinate.matches(position)
        override fun overlaps(other: ObjectPositionMatcher): Boolean = when (other) {
            is Any -> true
            is Exact -> coordinate == other.coordinate
            is ExactSet -> other.coordinates.contains(coordinate)
            is Range -> other.matchesCoordinate(coordinate)
            is Predicate -> true
        }

        override fun describe(): String = "exact:${coordinate.x}:${coordinate.y}:${coordinate.z}"
    }

    data class ExactSet(val coordinates: Set<ObjectCoordinate>) : ObjectPositionMatcher {
        override val specificity: Int = 2
        override fun matches(position: Position): Boolean = coordinates.any { it.matches(position) }
        override fun overlaps(other: ObjectPositionMatcher): Boolean = when (other) {
            is Any -> true
            is Exact -> coordinates.contains(other.coordinate)
            is ExactSet -> coordinates.any { other.coordinates.contains(it) }
            is Range -> coordinates.any { other.matchesCoordinate(it) }
            is Predicate -> true
        }

        override fun describe(): String = "set:${coordinates.sortedWith(compareBy({ it.x }, { it.y }, { it.z }))}"
    }

    data class Range(
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int,
        val plane: Int? = null,
    ) : ObjectPositionMatcher {
        init {
            require(minX <= maxX) { "Range minX must be <= maxX" }
            require(minY <= maxY) { "Range minY must be <= maxY" }
        }

        override val specificity: Int = 1

        override fun matches(position: Position): Boolean {
            if (plane != null && position.z != plane) {
                return false
            }
            return position.x in minX..maxX && position.y in minY..maxY
        }

        fun matchesCoordinate(coordinate: ObjectCoordinate): Boolean {
            if (plane != null && coordinate.z != plane) {
                return false
            }
            return coordinate.x in minX..maxX && coordinate.y in minY..maxY
        }

        override fun overlaps(other: ObjectPositionMatcher): Boolean = when (other) {
            is Any -> true
            is Exact -> matchesCoordinate(other.coordinate)
            is ExactSet -> other.coordinates.any { matchesCoordinate(it) }
            is Range -> {
                val planesOverlap = plane == null || other.plane == null || plane == other.plane
                if (!planesOverlap) {
                    false
                } else {
                    minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
                }
            }
            is Predicate -> true
        }

        override fun describe(): String = "range:$minX:$maxX:$minY:$maxY:${plane ?: "any"}"
    }

    class Predicate(
        private val label: String,
        private val matcher: (Position) -> Boolean,
    ) : ObjectPositionMatcher {
        override val specificity: Int = 1
        override fun matches(position: Position): Boolean = matcher(position)

        // Conservatively treated as overlapping to avoid ambiguous duplicate registration.
        override fun overlaps(other: ObjectPositionMatcher): Boolean = true

        override fun describe(): String = "predicate:$label"
    }
}

data class ObjectBinding(
    val objectId: Int,
    val matcher: ObjectPositionMatcher = ObjectPositionMatcher.Any,
    val priority: Int = 0,
) {
    init {
        require(objectId >= 0) { "ObjectBinding objectId must be >= 0 (was $objectId)" }
    }

    companion object {
        fun at(objectId: Int, x: Int, y: Int, z: Int = 0, priority: Int = 0): ObjectBinding {
            return ObjectBinding(objectId, ObjectPositionMatcher.Exact(ObjectCoordinate(x, y, z)), priority)
        }

        fun atAnyOf(objectId: Int, coordinates: Set<ObjectCoordinate>, priority: Int = 0): ObjectBinding {
            return ObjectBinding(objectId, ObjectPositionMatcher.ExactSet(coordinates), priority)
        }

        fun inRange(
            objectId: Int,
            minX: Int,
            maxX: Int,
            minY: Int,
            maxY: Int,
            plane: Int? = null,
            priority: Int = 0,
        ): ObjectBinding {
            return ObjectBinding(objectId, ObjectPositionMatcher.Range(minX, maxX, minY, maxY, plane), priority)
        }

        fun byPredicate(objectId: Int, label: String, priority: Int = 0, matcher: (Position) -> Boolean): ObjectBinding {
            return ObjectBinding(objectId, ObjectPositionMatcher.Predicate(label, matcher), priority)
        }
    }
}
