package net.dodian.uber.game.systems.sync

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.model.item.ItemManager
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.systems.net.PacketWalkingService
import net.dodian.uber.game.systems.net.WalkRequest
import net.dodian.uber.game.systems.world.npc.NpcManager
import net.dodian.utilities.Utils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InteractionMaskEncodingTest {
    private var previousNpcManager: NpcManager? = null
    private var previousItemManager: ItemManager? = null

    @AfterEach
    fun tearDown() {
        Server.npcManager = previousNpcManager
        previousNpcManager = null
        Server.itemManager = previousItemManager
        previousItemManager = null
    }

    @Test
    fun `player face-target -1 encodes as 65535 little-endian`() {
        val player = Client(EmbeddedChannel(), 1)
        player.faceTarget(-1)

        val encoded = encodePlayerFaceTarget(player)

        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), encoded)
    }

    @Test
    fun `player face-target 32768 plus 42 encodes exact little-endian value`() {
        val player = Client(EmbeddedChannel(), 1)
        player.faceTarget(32768 + 42)

        val encoded = encodePlayerFaceTarget(player)

        assertArrayEquals(byteArrayOf(0x2A.toByte(), 0x80.toByte()), encoded)
    }

    @Test
    fun `npc face-target invalid encodes as 65535`() {
        previousNpcManager = Server.npcManager
        if (Server.npcManager == null) {
            Server.npcManager = NpcManager()
        }

        val npc = Npc(1, 1, Position(3200, 3200, 0), 0)
        npc.faceTarget(-1)

        val encoded = encodeNpcFaceTarget(npc)

        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), encoded)
    }

    @Test
    fun `clear update flags does not requeue face-character reset mask`() {
        val player = Client(EmbeddedChannel(), 1)
        player.facePlayer(42)
        assertFalse(!player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))

        player.clearUpdateFlags()

        assertEquals(-1, player.faceTarget)
        assertFalse(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))

        player.clearUpdateFlags()
        assertFalse(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))
    }

    @Test
    fun `face-target clears face-coordinate flag to keep interaction authoritative`() {
        val player = Client(EmbeddedChannel(), 1)
        player.setFocus(3200, 3200)
        assertTrue(player.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))

        player.facePlayer(2)

        assertFalse(player.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))
        assertTrue(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))
    }

    @Test
    fun `set-focus clears face-character flag to keep coordinate-facing authoritative`() {
        val player = Client(EmbeddedChannel(), 1)
        player.facePlayer(2)
        assertTrue(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))

        player.setFocus(3200, 3200)

        assertFalse(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))
        assertTrue(player.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))
    }

    @Test
    fun `add-local block includes persisted face coordinate after movement`() {
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})

        val player = Client(EmbeddedChannel(), 1)
        player.longName = 1L
        player.playerName = "player-1"
        player.isActive = true
        player.initialized = true
        player.disconnected = false
        player.pLoaded = true
        player.dbId = 1
        player.teleportTo(3200, 3200, 0)

        primeMovementState(player)
        PacketWalkingService.handle(
            player,
            WalkRequest(
                opcode = 164,
                firstStepXAbs = 3201,
                firstStepYAbs = 3200,
                running = false,
                deltasX = intArrayOf(0),
                deltasY = intArrayOf(0),
            ),
        )
        player.postProcessing()
        player.getNextPlayerMovement()

        assertEquals(3201, player.persistedFaceX)
        assertEquals(3200, player.persistedFaceY)

        val block = PlayerUpdating.getInstance().buildSharedBlock(player, "ADD_LOCAL")

        assertEquals(0x12, block[0].toInt() and 0x12)
        assertArrayEquals(expectedFaceCoordinateTail(3201, 3200), block.copyOfRange(block.size - 4, block.size))
    }

    @Test
    fun `teleport remaps persisted face coordinate into new area for add-local sync`() {
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})

        val player = Client(EmbeddedChannel(), 1)
        player.longName = 1L
        player.playerName = "player-1"
        player.isActive = true
        player.initialized = true
        player.disconnected = false
        player.pLoaded = true
        player.dbId = 1
        player.teleportTo(3200, 3200, 0)
        player.setPersistedFaceCoord(3201, 3200)

        player.teleportTo(3300, 3300, 0)

        assertEquals(3301, player.persistedFaceX)
        assertEquals(3300, player.persistedFaceY)

        val block = PlayerUpdating.getInstance().buildSharedBlock(player, "ADD_LOCAL")

        assertEquals(0x12, block[0].toInt() and 0x12)
        assertArrayEquals(expectedFaceCoordinateTail(3301, 3300), block.copyOfRange(block.size - 4, block.size))
    }

    @Test
    fun `npc clear update flags reasserts default facing coordinate`() {
        previousNpcManager = Server.npcManager
        if (Server.npcManager == null) {
            Server.npcManager = NpcManager()
        }

        val npc = Npc(1, 1, Position(3200, 3200, 0), 2)
        npc.setFace(2)

        npc.clearUpdateFlags()

        assertTrue(npc.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))
        val expectedX = 3200 + Utils.directionDeltaX[2]
        val expectedY = 3200 + Utils.directionDeltaY[2]
        assertEquals((expectedX * 2) + 1, npc.faceCoordinateX)
        assertEquals((expectedY * 2) + 1, npc.faceCoordinateY)
    }

    private fun encodePlayerFaceTarget(player: Client): ByteArray {
        val buffer = ByteMessage.raw(2)
        try {
            PlayerUpdating.getInstance().appendFaceCharacter(player, buffer)
            return buffer.toByteArray()
        } finally {
            buffer.release()
        }
    }

    private fun encodeNpcFaceTarget(npc: Npc): ByteArray {
        val buffer = ByteMessage.raw(2)
        try {
            NpcUpdating.getInstance().appendFaceCharacter(npc, buffer)
            return buffer.toByteArray()
        } finally {
            buffer.release()
        }
    }

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }

    private fun expectedFaceCoordinateTail(x: Int, y: Int): ByteArray {
        val encodedX = encodeFaceCoordinate(x)
        val encodedY = encodeFaceCoordinate(y)
        return byteArrayOf(
            ((encodedX and 0xFF) + 128).toByte(),
            ((encodedX ushr 8) and 0xFF).toByte(),
            (encodedY and 0xFF).toByte(),
            ((encodedY ushr 8) and 0xFF).toByte(),
        )
    }

    private fun encodeFaceCoordinate(value: Int): Int = (value * 2) + 1
}
