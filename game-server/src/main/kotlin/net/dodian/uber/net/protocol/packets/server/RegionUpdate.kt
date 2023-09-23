package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

abstract class RegionUpdate : Message(), Comparable<RegionUpdate> {
    abstract val priority: Int
}