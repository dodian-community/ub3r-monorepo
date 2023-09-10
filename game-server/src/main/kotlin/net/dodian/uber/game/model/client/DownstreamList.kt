package net.dodian.uber.game.model.client

import io.netty.channel.Channel
import net.dodian.uber.protocol.packet.DownstreamPacket

class DownstreamList(
    private val packets: MutableList<DownstreamPacket> = mutableListOf()
) : MutableList<DownstreamPacket> by packets {
    fun flush(channel: Channel): DownstreamList {
        forEach { channel.write(it) }
        channel.flush()
        return this
    }
}