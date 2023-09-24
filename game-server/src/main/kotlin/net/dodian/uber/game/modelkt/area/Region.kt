package net.dodian.uber.game.modelkt.area

import com.google.common.collect.ImmutableSet
import net.dodian.uber.game.modelkt.area.collision.CollisionMatrix
import net.dodian.uber.game.modelkt.area.update.GroupableEntity
import net.dodian.uber.game.modelkt.entity.Entity
import net.dodian.uber.game.modelkt.entity.EntityType
import net.dodian.uber.game.modelkt.entity.isMob
import net.dodian.uber.game.modelkt.entity.isTransient
import net.dodian.uber.game.modelkt.entity.`object`.DynamicGameObject
import net.dodian.uber.net.protocol.packets.server.regionupdate.RegionUpdateMessage
import java.util.stream.Collectors

@Suppress("MemberVisibilityCanBePrivate")
data class Region(
    val coordinates: RegionCoordinates,
    private val listeners: MutableList<RegionListener> = mutableListOf(UpdateRegionListener()),
    private val entities: MutableMap<Position, MutableSet<Entity>> = mutableMapOf()
) {
    private val removedObjects: MutableList<MutableSet<RegionUpdateMessage>> = mutableListOf()
    private val updates: MutableList<MutableSet<RegionUpdateMessage>> = mutableListOf()

    init {
        for (height in 0 until Position.HEIGHT_LEVELS) {
            removedObjects.add(HashSet())
            updates.add(HashSet(DEFAULT_LIST_SIZE))
        }
    }

    constructor(x: Int, y: Int) : this(RegionCoordinates(x, y))

    private class UpdateRegionListener : RegionListener {

        override fun execute(region: Region, entity: Entity, update: EntityUpdateType) {
            if (entity.entityType.isMob || entity !is GroupableEntity) return

            region.record(entity, update)
        }
    }

    val surrounding: MutableSet<RegionCoordinates>
        get() {
            val localX = coordinates.x
            val localY = coordinates.y
            val maxX = localX + VIEWABLE_REGION_RADIUS
            val maxY = localY + VIEWABLE_REGION_RADIUS

            val viewable = hashSetOf<RegionCoordinates>()

            for (x in localX - VIEWABLE_REGION_RADIUS until maxX) {
                for (y in localY - VIEWABLE_REGION_RADIUS until maxY) {
                    viewable.add(RegionCoordinates(x, y))
                }
            }

            return viewable
        }

    private val matrices: Array<CollisionMatrix> = CollisionMatrix.createMatrices(Position.HEIGHT_LEVELS, SIZE, SIZE)

    fun addEntity(entity: Entity, notify: Boolean) {
        val type = entity.entityType
        val position = entity.position
        checkPosition(position)

        if (!type.isTransient) {
            val local: MutableSet<Entity> =
                entities.computeIfAbsent(position) { _ -> HashSet<Entity>(Region.DEFAULT_LIST_SIZE) }
            local.add(entity)
        }

        if (notify)
            notifyListeners(entity, EntityUpdateType.ADD)
    }

    fun removeEntity(entity: Entity) {
        val type = entity.entityType
        if (type.isTransient)
            error("Tried to remove a transient entity ($entity) from region ($this).")

        val position = entity.position
        checkPosition(position)

        val local = entities[position]
        if (local == null || !local.remove(entity))
            error("$entity belongs in $this but does not exist.")

        notifyListeners(entity, EntityUpdateType.REMOVE)
    }

    fun addListener(listener: RegionListener) {
        listeners.add(listener)
    }

    fun contains(position: Position) = coordinates == position.regionCoordinates

    fun updates(height: Int): Set<RegionUpdateMessage> {
        val updates = this.updates[height]
        val copy = ImmutableSet.copyOf(updates)

        updates.clear()
        return copy
    }

    fun encode(height: Int): Set<RegionUpdateMessage> {
        val additions = entities.values.stream()
            .flatMap<Entity> { obj: Set<Entity> -> obj.stream() }
            .filter { entity: Entity ->
                entity is DynamicGameObject && entity.position.height == height
            }
            .map<RegionUpdateMessage> { entity: Entity ->
                (entity as GroupableEntity).toUpdateOperation(this, EntityUpdateType.ADD).toMessage()
            }
            .collect(Collectors.toSet<RegionUpdateMessage>())

        val builder: ImmutableSet.Builder<RegionUpdateMessage> = ImmutableSet.builder()
        builder.addAll(additions).addAll(updates[height]).addAll(removedObjects[height])

        return builder.build()
    }

    fun matrix(height: Int): CollisionMatrix {
        if (height >= 0 && height < matrices.size)
            return matrices[height]

        error("Matrix height level must be between 0 and ${matrices.size - 1}, received: $height")
    }

    fun notifyListeners(entity: Entity, updateType: EntityUpdateType) {
        listeners.forEach { it.execute(this, entity, updateType) }
    }

    fun isTraversable(position: Position, entityType: EntityType, direction: Direction): Boolean {
        val matrix = matrices[position.height]
        val x = position.x
        val y = position.y

        return !matrix.isNotTraversable(x % SIZE, y % SIZE, entityType, direction)
    }

    private fun <T> record(entity: T, update: EntityUpdateType) where T : Entity, T : GroupableEntity {
        val operation = entity.toUpdateOperation(this, update)
        val message = operation.toMessage()
        val inverse = operation.inverse()

        val height = entity.position.height
        val updates = this.updates[height]

        if (entity.entityType == EntityType.STATIC_OBJECT) {
            removedObjects[height].apply {
                when (update == EntityUpdateType.REMOVE) {
                    true -> add(message)
                    false -> remove(inverse)
                }
            }
        } else updates.remove(inverse)

        updates.add(message)
    }

    private fun checkPosition(position: Position) {
        if (coordinates != RegionCoordinates.fromPosition(position))
            error("Position is not included in this Region.")
    }

    companion object {
        const val SIZE = 8
        const val VIEWABLE_REGION_RADIUS = 3
        const val VIEWPORT_WIDTH = SIZE * 13
        const val DEFAULT_LIST_SIZE = 2
    }
}