package net.dodian.uber.game.content.objects

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObjectContentRegistryTest {

    @BeforeEach
    fun setUp() {
        ObjectContentRegistry.clearForTests()
    }

    @AfterEach
    fun tearDown() {
        ObjectContentRegistry.resetForTests()
    }

    @Test
    fun `duplicate object ids inside one content throws`() {
        val duplicateInside = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(10, 10)
        }

        assertThrows(IllegalArgumentException::class.java) {
            ObjectContentRegistry.register("DuplicateInside", duplicateInside)
        }
    }

    @Test
    fun `duplicate object ids across content throws`() {
        val first = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(20)
        }
        val duplicate = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(20)
        }

        ObjectContentRegistry.register("First", first)
        assertThrows(IllegalArgumentException::class.java) {
            ObjectContentRegistry.register("Duplicate", duplicate)
        }
    }

    @Test
    fun `overlapping position range on same object id throws`() {
        val first = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(
                ObjectBinding.inRange(objectId = 30, minX = 3200, maxX = 3210, minY = 3200, maxY = 3210, plane = 0),
            )
        }
        val overlapping = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(
                ObjectBinding.inRange(objectId = 30, minX = 3205, maxX = 3220, minY = 3205, maxY = 3220, plane = 0),
            )
        }

        ObjectContentRegistry.register("RangeA", first)
        assertThrows(IllegalArgumentException::class.java) {
            ObjectContentRegistry.register("RangeB", overlapping)
        }
    }

    @Test
    fun `deterministic binding order for same specificity`() {
        val zeta = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(
                ObjectBinding.at(objectId = 40, x = 3200, y = 3200, z = 0),
            )
        }
        val alpha = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(
                ObjectBinding.at(objectId = 40, x = 3210, y = 3210, z = 0),
            )
        }

        ObjectContentRegistry.register("ZetaModule", zeta)
        ObjectContentRegistry.register("AlphaModule", alpha)

        val bindings = ObjectContentRegistry.bindingsForObjectForTests(40)
        assertEquals(2, bindings.size)
        assertEquals("exact:3210:3210:0", bindings[0].matcher.describe())
        assertEquals("exact:3200:3200:0", bindings[1].matcher.describe())
    }
}
