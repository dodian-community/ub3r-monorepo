package net.dodian.uber.protocol.game

import net.dodian.uber.net.builder.downstream.DownstreamPacketMap
import net.dodian.uber.net.builder.upstream.UpstreamPacketMap

class GamePacketMaps(
    val downstream: DownstreamPacketMap = DownstreamPacketMap(),
    val upstream: UpstreamPacketMap = UpstreamPacketMap()
) {

    fun eagerInitialize() {
        downstream.getOrCreateProtocol()
        upstream.getOrCreateProtocol()
    }
}