package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GnomeCourseObjectsTest {
    @Test
    fun `first click gnome obstacles require settled movement`() {
        val policy =
            GnomeCourseObjects.clickInteractionPolicy(
                option = 1,
                objectId = 23138,
                position = Position(2484, 3430, 0),
                obj = null,
            )

        assertNotNull(policy)
        assertEquals(ObjectInteractionPolicy.DistanceRule.LEGACY_OBJECT_DISTANCE, policy!!.distanceRule)
        assertTrue(policy.requireMovementSettled)
        assertEquals(1, policy.settleTicks)
    }

    @Test
    fun `non first click gnome obstacles do not override interaction policy`() {
        val policy =
            GnomeCourseObjects.clickInteractionPolicy(
                option = 2,
                objectId = 23138,
                position = Position(2484, 3430, 0),
                obj = null,
            )

        assertNull(policy)
    }
}
