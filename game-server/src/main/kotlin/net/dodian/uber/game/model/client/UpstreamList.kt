package net.dodian.uber.game.model.client

import net.dodian.uber.protocol.packet.UpstreamPacket

class UpstreamList(
    private val packets: MutableList<UpstreamPacket> = mutableListOf()
) : MutableList<UpstreamPacket> by packets