package net.dodian.uber.game.model.entity.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import net.dodian.uber.game.api.plugin.skills.PendingSkillMulti
import net.dodian.uber.game.engine.sync.player.PlayerSyncInvariantValidator
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.skillRecipe

class PlayerContentRuntimeStateTest {
    @Test
    fun `skill sessions and throttles are isolated and clearable`() {
        val state = PlayerContentRuntimeState()

        state.setActiveSkillSession("fishing", 42L)
        state.setThrottleUntilCycle("content:fishing", 99L)

        assertEquals("fishing", state.getActiveSkillSessionKey())
        assertEquals(42L, state.getActiveSkillSessionStartedCycle())
        assertEquals(99L, state.getThrottleUntilCycle("content:fishing"))

        state.clearActiveSkillSession()
        state.clearThrottleUntilCycle("content:fishing")

        assertNull(state.getActiveSkillSessionKey())
        assertEquals(0L, state.getActiveSkillSessionStartedCycle())
        assertEquals(0L, state.getThrottleUntilCycle("content:fishing"))
    }

    @Test
    fun `nonpositive throttle removes an existing throttle`() {
        val state = PlayerContentRuntimeState()
        state.setThrottleUntilCycle("content:bank", 12L)
        state.setThrottleUntilCycle("content:bank", 0L)

        assertEquals(0L, state.getThrottleUntilCycle("content:bank"))
    }

    @Test
    fun `termination clears pending production menu`() {
        val state = PlayerContentRuntimeState()
        val recipe = skillRecipe("test.recipe", 2) { material(1) }
        state.setPendingSkillMulti(PendingSkillMulti(SkillMultiConfig("test.menu", entries = listOf(SkillMultiEntry(recipe)))) {})

        state.terminatePlayerTasks()

        assertNull(state.getPendingSkillMulti())
    }

    @Test
    fun `previous sync snapshot round-trips and starts null`() {
        // WorldSynchronizationService's dev-gated invariant check (PlayerSyncInvariantValidator)
        // reads/writes this per-tick. It must start null for a fresh player object so a new
        // login never gets compared against a stale snapshot left behind by a previous session
        // that happened to reuse the same numeric slot.
        val state = PlayerContentRuntimeState()
        assertNull(state.getPreviousSyncSnapshot())

        val snapshot = PlayerSyncInvariantValidator.ViewerLocalSnapshot(intArrayOf(1, 2, 3), 7L)
        state.setPreviousSyncSnapshot(snapshot)

        assertEquals(snapshot, state.getPreviousSyncSnapshot())
    }

    @Test
    fun `plugin attributes are isolated by key and clearable at logout`() {
        val state = PlayerContentRuntimeState()

        state.putPluginAttribute("fletching:selection", 2)
        state.putPluginAttribute("crafting:selection", 3)

        assertEquals(2, state.getPluginAttribute<Int>("fletching:selection"))
        assertEquals(3, state.getPluginAttribute<Int>("crafting:selection"))

        state.clearPluginAttributes()

        assertNull(state.getPluginAttribute<Int>("fletching:selection"))
        assertNull(state.getPluginAttribute<Int>("crafting:selection"))
    }
}
