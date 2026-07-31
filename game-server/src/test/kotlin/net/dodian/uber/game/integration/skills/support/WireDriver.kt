package net.dodian.uber.game.integration.skills.support

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.util.AttributeKey
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteBufReader
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.ByteOrder
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.netty.codec.ValueType
import net.dodian.uber.game.netty.game.GamePacketDecoder
import net.dodian.uber.game.netty.game.GamePacketHandler
import net.dodian.uber.game.netty.listener.PacketListenerManager
import net.dodian.utilities.ISAACCipher
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Test-side protocol driver: pushes real client-shaped frames through the REAL wire
 * layer (GamePacketDecoder -> GamePacketHandler -> InboundPacketMailbox -> EntityProcessor
 * on real ticks) instead of calling packet services directly like [RealWorldHarness] does.
 *
 * Inbound frames are encoded exactly the way the real 317 client encodes them - the opcode
 * byte XORed with the next ISAAC keystream key, and the payload mirrored field-for-field
 * from the server's own decode classes (TarnishPackets) so a decode-order mismatch fails
 * the test instead of silently misparsing. Outbound packets (the server's real
 * ByteMessage objects) are captured from the channel for assertions.
 *
 * Usage: call [initialize] once (registers the real PacketListenerManager, which the
 * production Server startup does but RealWorldHarness deliberately does not), then
 * [wireClient] for each player before sending anything, then drive with [objectOption]
 * and friends, advancing real ticks via [RealWorldHarness.tick] / [RealWorldHarness.tickUntil].
 */
object WireDriver {
    private val initialized = AtomicBoolean(false)

    /** Fixed test seed; both sides use the same seed, so the keystreams stay in lockstep. */
    private val CIPHER_SEED = intArrayOf(0x1A2B3C4D, 0x5E6F7081, 0x92939495.toInt(), 0xA6A7A8A9.toInt())

    private val OUT_CIPHER_KEY: AttributeKey<ISAACCipher> = AttributeKey.valueOf("wireOutCipher")

    /** Registers the real inbound packet listeners (idempotent, one-time). */
    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            PacketListenerManager.initialize()
        }
    }

    /**
     * Wires [client]'s channel with the real decoder/handler pair and the ISAAC ciphers,
     * and connects the EmbeddedChannel so `Client.send(...)`'s `isActive()` guard passes
     * (without this the server drops every outbound packet in tests).
     */
    fun wireClient(client: Client) {
        initialize()
        val channel = client.getChannel() as EmbeddedChannel
        channel.connect(InetSocketAddress("127.0.0.1", 43594))
        channel.pipeline().addLast(GamePacketDecoder())
        channel.pipeline().addLast(GamePacketHandler(client))
        channel.attr(GamePacketDecoder.IN_CIPHER_KEY).set(ISAACCipher(CIPHER_SEED))
        channel.attr(OUT_CIPHER_KEY).set(ISAACCipher(CIPHER_SEED))
    }

    /**
     * Sends a real object-click packet (the same wire frames a real client produces for
     * options 1-5 of any world object). Field orders mirror
     * `TarnishPackets.ObjectClick.decode(...)` exactly, per opcode.
     */
    fun objectOption(client: Client, objectId: Int, x: Int, y: Int, option: Int) {
        val opcode = when (option) {
            1 -> 132
            2 -> 252
            3 -> 70
            4 -> 234
            5 -> 228
            else -> error("No known object-click opcode for option $option")
        }
        val msg = ByteMessage.message(opcode, MessageType.FIXED)
        when (opcode) {
            132 -> {
                msg.putShort(x, ByteOrder.LITTLE, ValueType.ADD)
                msg.putShort(objectId, ByteOrder.BIG, ValueType.NORMAL)
                msg.putShort(y, ByteOrder.BIG, ValueType.ADD)
            }
            252 -> {
                msg.putShort(objectId, ByteOrder.LITTLE, ValueType.ADD)
                msg.putShort(y, ByteOrder.LITTLE, ValueType.NORMAL)
                msg.putShort(x, ByteOrder.BIG, ValueType.ADD)
            }
            70 -> {
                msg.putShort(x, ByteOrder.LITTLE, ValueType.NORMAL)
                msg.putShort(y, ByteOrder.BIG, ValueType.NORMAL)
                msg.putShort(objectId, ByteOrder.LITTLE, ValueType.ADD)
            }
            234 -> {
                msg.putShort(x, ByteOrder.LITTLE, ValueType.ADD)
                msg.putShort(objectId, ByteOrder.BIG, ValueType.ADD)
                msg.putShort(y, ByteOrder.LITTLE, ValueType.ADD)
            }
            228 -> {
                msg.putShort(objectId, ByteOrder.BIG, ValueType.ADD)
                msg.putShort(y, ByteOrder.BIG, ValueType.ADD)
                msg.putShort(x, ByteOrder.BIG, ValueType.NORMAL)
            }
        }
        sendFrame(client, opcode, msg)
    }

    private fun sendFrame(client: Client, opcode: Int, msg: ByteMessage) {
        val channel = client.getChannel() as EmbeddedChannel
        val cipher = channel.attr(OUT_CIPHER_KEY).get()
        val payload = msg.getBuffer()
        val frame = Unpooled.buffer(1 + payload.readableBytes())
        try {
            frame.writeByte((opcode + cipher.getNextKey()) and 0xFF)
            frame.writeBytes(payload, payload.readerIndex(), payload.readableBytes())
            channel.writeInbound(frame)
        } finally {
            msg.release()
        }
    }

    /** Drains every server->client packet that landed on the wire (opcode + payload). */
    fun drainOutbound(client: Client): List<ByteMessage> {
        val channel = client.getChannel() as EmbeddedChannel
        val result = ArrayList<ByteMessage>()
        while (true) {
            val message = channel.readOutbound<ByteMessage>() ?: break
            result.add(message)
        }
        return result
    }

    /** Decodes all opcode-253 SendMessage packets into their text, in wire order. */
    fun outboundMessages(client: Client): List<String> =
        drainOutbound(client)
            .filter { it.getOpcode() == 253 }
            .map { ByteBufReader.readTerminatedString(it.getBuffer()) }
}
