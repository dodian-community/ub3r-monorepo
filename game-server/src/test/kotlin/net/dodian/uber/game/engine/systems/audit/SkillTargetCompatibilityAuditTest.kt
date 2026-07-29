package net.dodian.uber.game.engine.systems.audit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillTargetCompatibilityAuditTest {
    @Test
    fun `every active skill object and npc route resolves in committed rev218 mappings`() {
        val entries = SkillTargetCompatibilityAudit.audit()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries.any { it.binding.kind == SkillTargetCompatibilityAudit.TargetKind.OBJECT })
        assertTrue(entries.any { it.binding.kind == SkillTargetCompatibilityAudit.TargetKind.NPC })
        assertTrue(entries.none { it.local == null }, "Active routes must not target an unmapped client definition")
    }

    @Test
    fun `local binding signatures retain their current cache names and click actions`() {
        val entries = SkillTargetCompatibilityAudit.audit()
        val smithingFurnace = entries.first { it.binding.plugin == "Smithing" && it.binding.id == 2030 }
        val fishingSpot = entries.first { it.binding.plugin == "Fishing" && it.binding.id == 1518 }
        assertEquals("Furnace", smithingFurnace.local!!.name)
        assertTrue(smithingFurnace.local!!.options.contains("Smelt"))
        assertEquals("Fishing spot", fishingSpot.local!!.name)
        assertTrue(fishingSpot.local!!.options.contains("Small Net"))
    }

    @Test
    fun `interface inventory excludes the custom make all presentation`() {
        val interfaces = SkillTargetCompatibilityAudit.interfaceBindings()
        assertTrue(interfaces.any { it.owner == "SmithingSmeltingBridge" && it.id == 2400 })
        assertTrue(interfaces.any { it.owner == "Skillguide" && it.id == 8714 })
        assertFalse(interfaces.any { it.excluded })
    }

    @Test
    fun `accepted remaps replace existing fishing routes without adding a ninth route`() {
        val remaps = SkillTargetCompatibilityAudit.acceptedExistingContentRemaps
        assertEquals(4, remaps.size)
        assertTrue(remaps.all { it.plugin == "Fishing" && it.kind == SkillTargetCompatibilityAudit.TargetKind.NPC && it.route == "npc-click" })

        val activeFishingRoutes = SkillTargetCompatibilityAudit.targetBindings().filter { it.plugin == "Fishing" && it.kind == SkillTargetCompatibilityAudit.TargetKind.NPC }
        assertEquals(8, activeFishingRoutes.size)
        remaps.forEach { remap ->
            assertTrue(activeFishingRoutes.any { it.id == remap.currentId && it.option == remap.option })
            assertFalse(activeFishingRoutes.any { it.id == remap.legacyId && it.option == remap.option })
        }

        val auditEntries = SkillTargetCompatibilityAudit.audit()
        remaps.forEach { remap ->
            val entry = auditEntries.first {
                it.binding.plugin == remap.plugin &&
                    it.binding.kind == remap.kind &&
                    it.binding.id == remap.currentId &&
                    it.binding.option == remap.option &&
                    it.binding.route == remap.route
            }
            assertEquals(SkillTargetCompatibilityAudit.Status.EXISTING_CONTENT_REMAP, entry.status)
            assertEquals(listOf(remap.legacyId), entry.tarnishCandidates)
        }
    }
}
