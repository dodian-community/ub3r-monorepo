package net.dodian.uber.game.ui

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MagicInterfaceTest {
    @Test
    fun `magic interface bindings preserve spellbook autocast and teleport routes`() {
        val bindingsByKey = MagicInterface.bindings.associateBy { it.componentKey }

        assertEquals(13, MagicInterface.bindings.size, "Expected core magic interface binding count to remain stable")

        val spellbookToggle = requireNotNull(bindingsByKey["magic.spellbook_toggle"])
        assertEquals(1151, spellbookToggle.interfaceId)
        assertEquals(0, spellbookToggle.componentId)
        assertArrayEquals(intArrayOf(74212, 49047, 49046, 23024), spellbookToggle.rawButtonIds)

        val autocastClear = requireNotNull(bindingsByKey["magic.autocast.clear"])
        assertEquals(12855, autocastClear.interfaceId)
        assertEquals(1, autocastClear.componentId)
        assertArrayEquals(intArrayOf(1097, 1094, 1093), autocastClear.rawButtonIds)

        val autocastSelect = requireNotNull(bindingsByKey["magic.autocast.select"])
        assertEquals(12855, autocastSelect.interfaceId)
        assertEquals(2, autocastSelect.componentId)
        assertArrayEquals(intArrayOf(51133, 51185, 51091, 24018, 51159, 51211, 51111, 51069, 51146, 51198, 51102, 51058, 51172, 51224, 51122, 51080), autocastSelect.rawButtonIds)

        val autocastRefresh = requireNotNull(bindingsByKey["magic.autocast.refresh"])
        assertEquals(12855, autocastRefresh.interfaceId)
        assertEquals(3, autocastRefresh.componentId)
        assertArrayEquals(intArrayOf(24017), autocastRefresh.rawButtonIds)

        val expectedTeleports = mapOf(
            "magic.teleport.yanille" to intArrayOf(21741, 75010, 84237),
            "magic.teleport.seers" to intArrayOf(13035, 4143, 50235),
            "magic.teleport.ardougne" to intArrayOf(13045, 4146, 50245),
            "magic.teleport.catherby" to intArrayOf(13053, 4150, 50253),
            "magic.teleport.legends_guild" to intArrayOf(13061, 6004, 51005),
            "magic.teleport.taverly" to intArrayOf(13069, 6005, 51013),
            "magic.teleport.fishing_guild" to intArrayOf(13079, 29031, 51023),
            "magic.teleport.gnome_village" to intArrayOf(13087, 72038, 51031),
            "magic.teleport.edgeville" to intArrayOf(13095, 4140, 51039),
        )

        assertTrue(expectedTeleports.keys.all(bindingsByKey::containsKey), "Expected every teleport binding key to remain registered")
        expectedTeleports.forEach { (componentKey, rawButtons) ->
            val binding = requireNotNull(bindingsByKey[componentKey])
            assertEquals(12855, binding.interfaceId)
            assertArrayEquals(rawButtons, binding.rawButtonIds)
        }
    }
}
