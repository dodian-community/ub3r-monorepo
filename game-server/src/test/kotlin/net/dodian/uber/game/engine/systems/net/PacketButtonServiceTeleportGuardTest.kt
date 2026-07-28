package net.dodian.uber.game.engine.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.action.PlayerActionController
import net.dodian.uber.game.engine.systems.action.PlayerActionType
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * PacketButtonService.prepareAction used to unconditionally call client.resetAction(false)
 * for almost every button click, before the click's own handler ever ran. For teleport this
 * cleared activeActionType/UsingAgility one step before Client.triggerTele's own busy-guard
 * (doingTeleport()) checked them, so a second teleport click mid-cast always looked "not busy"
 * and restarted the cast - repeatable forever, the player could never actually leave.
 */
class PacketButtonServiceTeleportGuardTest {
    private fun client(): Client = Client(EmbeddedChannel(), 1)

    @Test
    fun `prepareAction does not clear an in-progress teleport`() {
        val client = client()
        client.triggerTele(3200, 3200, 0, false)
        // The teleport coroutine must actually be running (suspended mid-cast, like a real
        // in-flight teleport) for its cancellation path to be exercised at all - without this,
        // terminate()'s onTerminate/finally cleanup never registered in the first place, and
        // the test would pass regardless of whether the fix is present.
        GameTaskRuntime.cyclePlayer(client)
        assertEquals(
            PlayerActionType.TELEPORT,
            client.activeActionType,
            "precondition: triggerTele must actually start a teleport action",
        )

        // 58035 (teleport-menu execute button) is not in prepareAction's whitelist.
        PacketButtonService.prepareAction(client, 58035)

        assertEquals(
            PlayerActionType.TELEPORT,
            client.activeActionType,
            "a second button click mid-cast must not clear the in-progress teleport",
        )
    }

    @Test
    fun `prepareAction still resets an unrelated in-progress action as before`() {
        val client = client()
        // Must actually suspend (mid-action, like a real in-progress action) rather than run to
        // completion on the first cycle, or there'd be nothing left active to prove gets reset.
        PlayerActionController.start(player = client, type = PlayerActionType.PRODUCTION) {
            wait(5)
        }
        GameTaskRuntime.cyclePlayer(client) // starts the coroutine; it suspends inside wait(5)
        assertEquals(PlayerActionType.PRODUCTION, client.activeActionType)

        PacketButtonService.prepareAction(client, 58035)

        assertNull(
            client.activeActionType,
            "the teleport-specific carve-out must not change the existing reset behavior for other action types",
        )
    }
}
