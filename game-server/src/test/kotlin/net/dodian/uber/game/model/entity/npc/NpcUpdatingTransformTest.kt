package net.dodian.uber.game.model.entity.npc

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.testutil.RequiresCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NpcUpdatingTransformTest {
    private var previousNpcManager: NpcManager? = null

    @BeforeEach
    fun setUp() {
        RequiresCache.assume()
        previousNpcManager = Server.npcManager
        Server.npcManager = NpcManager()
    }

    @AfterEach
    fun tearDown() {
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `add local uses transformed npc definition`() {
        val viewer = Client(EmbeddedChannel(), 1)
        val npc = Npc(2, 1, Position(3201, 3200, 0), 0)
        npc.transformTo(2)

        val method = NpcUpdating::class.java.getDeclaredMethod("displayIdFor", net.dodian.uber.game.model.entity.player.Player::class.java, Npc::class.java)
        method.isAccessible = true

        assertEquals(2, method.invoke(NpcUpdating.getInstance(), viewer, npc))
        viewer.saveNeeded = false
        viewer.destruct()
    }
}
