package net.dodian.uber.net.protocol.decoders

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.net.codec.game.*
import net.dodian.uber.net.message.MessageDecoder
import net.dodian.uber.net.protocol.packets.client.WalkMessage

class WalkMessageDecoder : MessageDecoder<WalkMessage>() {
    override fun decode(packet: GamePacket): WalkMessage {
        val reader = GamePacketReader(packet)

        var length = packet.length
        if (packet.opcode == 248)
            length -= 14 // Strip anti-cheat data

        val steps = (length - 5) / 2
        val path = Array<IntArray>(steps) { IntArray(2) }

        val x = reader.getUnsigned(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD).toInt()
        for (i in 0 until steps) {
            path[i][0] = reader.getSigned(DataType.BYTE).toInt()
            path[i][1] = reader.getSigned(DataType.BYTE).toInt()
        }
        val y = reader.getUnsigned(DataType.SHORT, DataOrder.LITTLE).toInt()
        val run = reader.getUnsigned(DataType.BYTE, DataTransformation.NEGATE).toInt() == 1

        val positions = Array<Position>(steps + 1) { Position(x, y) }
        for (i in 0 until steps)
            positions[i + 1] = Position(path[i][0] + x, path[i][1] + y)

        return WalkMessage(positions.toList(), run)
    }
}