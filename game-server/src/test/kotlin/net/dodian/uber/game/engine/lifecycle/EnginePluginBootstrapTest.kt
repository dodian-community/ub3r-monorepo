package net.dodian.uber.game.engine.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EnginePluginBootstrapTest {
    @Test
    fun `engine bootstrap starts in discover phase`() {
        assertSame(EnginePluginBootstrap.LifecyclePhase.DISCOVER, EnginePluginBootstrap.currentPhase())
    }

    @Test
    fun `engine bootstrap order is deterministic and filters legacy alias registry`() {
        val orderedIds = EnginePluginBootstrap.orderedContentBootstrapIds()

        assertEquals(
            listOf(
                "plugins.registry",
                "commands.registry",
                "items.registry",
                "npcs.registry",
                "objects.registry",
                "shops.registry",
                "skills.doctor",
            ),
            orderedIds,
        )
        assertFalse("skills.registry" in orderedIds, "legacy SkillPluginRegistry alias must not participate in engine startup order")
    }
}

