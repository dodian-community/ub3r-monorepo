package net.dodian.uber.game.model.chunk

import java.util.AbstractSet
import java.util.ArrayList
import java.util.Collections
import java.util.EnumMap
import java.util.LinkedHashSet
import java.util.NoSuchElementException
import java.util.Objects
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.entity.Entity

class ChunkEntityIndex(
    private val chunk: Chunk,
) {
    private val entities: MutableMap<EntityType, MutableSet<Entity>> = EnumMap(EntityType::class.java)

    fun getChunk(): Chunk = chunk

    fun add(entity: Entity) {
        val type = entity.entityType
        entities.computeIfAbsent(type) { SmallEntitySet<Entity>() }.add(entity)
    }

    fun remove(entity: Entity) {
        val type = entity.entityType
        val entitySet = entities[type] ?: return
        if (entitySet.remove(entity) && entitySet.isEmpty()) {
            entities.remove(type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Entity> getAll(type: EntityType): MutableSet<E> {
        val entitySet = entities[type]
        return if (entitySet == null) Collections.emptySet<E>().toMutableSet() else entitySet as MutableSet<E>
    }

    fun isEmpty(): Boolean = entities.isEmpty()

    fun size(): Int {
        var total = 0
        for (entitySet in entities.values) {
            total += entitySet.size
        }
        return total
    }

    internal class SmallEntitySet<E> : AbstractSet<E>() {
        private var single: E? = null
        private var small: ArrayList<E>? = null
        private var large: LinkedHashSet<E>? = null

        override fun iterator(): MutableIterator<E> {
            val largeSet = large
            if (largeSet != null) {
                return largeSet.iterator()
            }
            val smallSet = small
            if (smallSet != null) {
                return smallSet.iterator()
            }
            val only = single
            if (only == null) {
                return Collections.emptyIterator<E>() as MutableIterator<E>
            }
            return object : MutableIterator<E> {
                private var hasNext = true
                private var removable = false

                override fun hasNext(): Boolean = hasNext

                override fun next(): E {
                    if (!hasNext) {
                        throw NoSuchElementException()
                    }
                    hasNext = false
                    removable = true
                    return only
                }

                override fun remove() {
                    if (!removable) {
                        throw IllegalStateException()
                    }
                    this@SmallEntitySet.single = null
                    removable = false
                }
            }
        }

        override val size: Int
            get() {
                val largeSet = large
                if (largeSet != null) {
                    return largeSet.size
                }
                val smallSet = small
                if (smallSet != null) {
                    return smallSet.size
                }
                return if (single == null) 0 else 1
            }

        override fun contains(element: E): Boolean {
            val largeSet = large
            if (largeSet != null) {
                return largeSet.contains(element)
            }
            val smallSet = small
            if (smallSet != null) {
                return smallSet.contains(element)
            }
            return single != null && Objects.equals(single, element)
        }

        override fun add(element: E): Boolean {
            val largeSet = large
            if (largeSet != null) {
                return largeSet.add(element)
            }
            if (single == null && small == null) {
                single = element
                return true
            }
            if (single != null) {
                if (Objects.equals(single, element)) {
                    return false
                }
                small = ArrayList(PROMOTION_THRESHOLD)
                small!!.add(single!!)
                single = null
            }
            val smallSet = small!!
            if (smallSet.contains(element)) {
                return false
            }
            smallSet.add(element)
            if (smallSet.size > PROMOTION_THRESHOLD) {
                large = LinkedHashSet(smallSet)
                small = null
            }
            return true
        }

        override fun remove(element: E): Boolean {
            val largeSet = large
            if (largeSet != null) {
                return largeSet.remove(element)
            }
            val smallSet = small
            if (smallSet != null) {
                val removed = smallSet.remove(element)
                if (!removed) {
                    return false
                }
                if (smallSet.isEmpty()) {
                    small = null
                } else if (smallSet.size == 1) {
                    single = smallSet[0]
                    small = null
                }
                return true
            }
            if (single != null && Objects.equals(single, element)) {
                single = null
                return true
            }
            return false
        }

        override fun clear() {
            single = null
            small = null
            large = null
        }

        companion object {
            private const val PROMOTION_THRESHOLD = 8
        }
    }
}
