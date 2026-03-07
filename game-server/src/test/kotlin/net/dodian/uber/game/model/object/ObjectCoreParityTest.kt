package net.dodian.uber.game.model.`object`

import net.dodian.uber.game.model.`object`.door.DoorDefinitionLoader
import net.dodian.uber.game.model.`object`.door.doorDefinitions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectCoreParityTest {
    @AfterEach
    fun tearDown() {
        GlobalObject.getGlobalObject().clear()
        DoorHandler.doorX = IntArray(100)
        DoorHandler.doorY = IntArray(100)
        DoorHandler.doorId = IntArray(100)
        DoorHandler.doorHeight = IntArray(100)
        DoorHandler.doorFaceOpen = IntArray(100)
        DoorHandler.doorFaceClosed = IntArray(100)
        DoorHandler.doorFace = IntArray(100)
        DoorHandler.doorState = IntArray(100)
    }

    @Test
    fun `world object constructors and attachment remain unchanged`() {
        val worldObject = Object(100, 3200, 3201, 0, 10, 2, 99)
        val attachment = Any()

        worldObject.setAttachment(attachment)

        assertEquals(100, worldObject.id)
        assertEquals(3200, worldObject.x)
        assertEquals(3201, worldObject.y)
        assertEquals(0, worldObject.z)
        assertEquals(10, worldObject.type)
        assertEquals(2, worldObject.face)
        assertEquals(99, worldObject.oldId)
        assertSame(attachment, worldObject.getAttachment())
    }

    @Test
    fun `rs2 object constructors and attachment remain unchanged`() {
        val objectDef = RS2Object(200, 3210, 3211, 22, 1, 88)
        val attachment = "tag"

        objectDef.setAttachment(attachment)

        assertEquals(200, objectDef.id)
        assertEquals(3210, objectDef.x)
        assertEquals(3211, objectDef.y)
        assertEquals(22, objectDef.type)
        assertEquals(1, objectDef.face)
        assertEquals(88, objectDef.oldId)
        assertEquals("tag", objectDef.getAttachment())
    }

    @Test
    fun `global object coordinate lookup and duplicate checks remain unchanged`() {
        val first = Object(100, 3200, 3200, 0, 10, 0, 50)
        val second = Object(101, 3201, 3201, 0, 10, 1, 51)

        first.setAttachment(System.currentTimeMillis() + 5_000L)
        second.setAttachment(System.currentTimeMillis() + 5_000L)
        GlobalObject.getGlobalObject().add(first)
        GlobalObject.getGlobalObject().add(second)

        assertTrue(GlobalObject.hasGlobalObject(Object(999, 3200, 3200, 0, 10)))
        assertFalse(GlobalObject.hasGlobalObject(Object(999, 3300, 3300, 0, 10)))
        assertSame(first, GlobalObject.getGlobalObject(3200, 3200))
        assertNull(GlobalObject.getGlobalObject(3300, 3300))
    }

    @Test
    fun `door handler populates legacy arrays and keeps mutable state`() {
        val definitions =
            doorDefinitions {
                door(2605, 3104, 1530, 0, 1, 0, 0, 0)
                door(2607, 3104, 1531, 1, 3, 2, 2, 1)
            }

        DoorHandler(DoorDefinitionLoader { definitions })

        assertTrue(DoorHandler.doorX.size >= 100)
        assertEquals(2605, DoorHandler.doorX[0])
        assertEquals(3104, DoorHandler.doorY[0])
        assertEquals(1530, DoorHandler.doorId[0])
        assertEquals(1, DoorHandler.doorFaceOpen[0])
        assertEquals(0, DoorHandler.doorFaceClosed[0])
        assertEquals(0, DoorHandler.doorFace[0])
        assertEquals(0, DoorHandler.doorState[0])
        assertEquals(0, DoorHandler.doorHeight[0])

        assertEquals(2607, DoorHandler.doorX[1])
        assertEquals(3104, DoorHandler.doorY[1])
        assertEquals(1531, DoorHandler.doorId[1])
        assertEquals(3, DoorHandler.doorFaceOpen[1])
        assertEquals(2, DoorHandler.doorFaceClosed[1])
        assertEquals(2, DoorHandler.doorFace[1])
        assertEquals(1, DoorHandler.doorState[1])
        assertEquals(1, DoorHandler.doorHeight[1])

        DoorHandler.doorState[0] = 1
        DoorHandler.doorFace[0] = DoorHandler.doorFaceOpen[0]

        assertEquals(1, DoorHandler.doorState[0])
        assertEquals(1, DoorHandler.doorFace[0])
    }
}
