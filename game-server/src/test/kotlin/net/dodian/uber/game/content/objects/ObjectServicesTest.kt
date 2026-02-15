package net.dodian.uber.game.content.objects

import net.dodian.uber.game.content.objects.services.ObjectClipService
import net.dodian.uber.game.content.objects.services.ObjectSpawnService
import net.dodian.uber.game.content.objects.services.PersonalObjectService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.`object`.GlobalObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObjectServicesTest {
    @BeforeEach
    fun setUp() {
        GlobalObject.getGlobalObject().clear()
        ObjectSpawnService.clearForTests()
        PersonalObjectService.clearForTests()
        ObjectClipService.clearForTests()
    }

    @AfterEach
    fun tearDown() {
        GlobalObject.getGlobalObject().clear()
        ObjectSpawnService.clearForTests()
        PersonalObjectService.clearForTests()
        ObjectClipService.clearForTests()
    }

    @Test
    fun `global object lifecycle spawn and remove`() {
        val pos = Position(3200, 3200, 0)
        assertTrue(
            ObjectSpawnService.spawnTemporaryGlobal(
                objectId = 12345,
                position = pos,
                face = 0,
                type = 10,
                oldObjectId = -1,
                durationMs = 1_000,
            ),
        )
        assertNotNull(GlobalObject.getGlobalObject(pos.x, pos.y))
        assertEquals(1, ObjectSpawnService.activeSpawnsForTests().size)

        assertTrue(ObjectSpawnService.removeTemporaryGlobal(pos))
        assertNull(GlobalObject.getGlobalObject(pos.x, pos.y))
    }

    @Test
    fun `personal object lifecycle isolates by player`() {
        val pos = Position(3210, 3210, 0)
        val first = Client(null, 1)
        val second = Client(null, 2)

        PersonalObjectService.show(first, pos, objectId = 5000, face = 0, type = 10)
        PersonalObjectService.show(second, pos, objectId = 6000, face = 0, type = 10)

        assertEquals(5000, PersonalObjectService.stateForTests(first, pos)?.objectId)
        assertEquals(6000, PersonalObjectService.stateForTests(second, pos)?.objectId)
    }

    @Test
    fun `clip apply remove symmetry`() {
        val pos = Position(3220, 3220, 0)
        ObjectClipService.apply(position = pos, type = 0, direction = 1, solid = false)
        assertNotNull(ObjectClipService.getAppliedForTests(pos))

        ObjectClipService.remove(position = pos, type = 0, direction = 1, solid = false)
        assertNull(ObjectClipService.getAppliedForTests(pos))
    }
}
