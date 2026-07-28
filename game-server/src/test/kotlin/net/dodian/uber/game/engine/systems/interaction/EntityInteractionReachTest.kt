package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.entity.Entity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityInteractionReachTest {
    @Test
    fun `classifies overlap adjacency range and plane`() {
        val actor = entity(3200, 3200)

        assertEquals(EntityReachResult.OVERLAPPING, EntityInteractionReach.resolve(actor, entity(3200, 3200), 1))
        assertEquals(EntityReachResult.REACHED, EntityInteractionReach.resolve(actor, entity(3201, 3200), 1))
        assertEquals(EntityReachResult.REACHED, EntityInteractionReach.resolve(actor, entity(3201, 3201), 1))
        assertEquals(EntityReachResult.OUT_OF_RANGE, EntityInteractionReach.resolve(actor, entity(3202, 3200), 1))
        assertEquals(EntityReachResult.DIFFERENT_PLANE, EntityInteractionReach.resolve(actor, entity(3201, 3200, z = 1), 1))
        assertEquals(EntityReachResult.INVALID_TARGET, EntityInteractionReach.resolve(actor, null, 1))
    }

    @Test
    fun `uses complete footprints for multi-tile targets`() {
        val actor = entity(3199, 3201)
        val largeTarget = entity(3200, 3200, size = 3)
        val overlappingActor = entity(3201, 3201)

        assertEquals(EntityReachResult.REACHED, EntityInteractionReach.resolve(actor, largeTarget, 1))
        assertEquals(EntityReachResult.OVERLAPPING, EntityInteractionReach.resolve(overlappingActor, largeTarget, 1))
    }

    private fun entity(x: Int, y: Int, z: Int = 0, size: Int = 1): Entity =
        object : Entity(Position(x, y, z), 1, Type.PLAYER) {
            override fun didMove(): Boolean = false

            override fun getSize(): Int = size

            override fun getEntityType(): EntityType = EntityType.PLAYER
        }
}
