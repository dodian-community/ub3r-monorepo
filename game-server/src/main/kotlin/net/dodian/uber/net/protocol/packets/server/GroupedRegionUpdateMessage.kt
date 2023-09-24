package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.RegionCoordinates
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.server.regionupdate.RegionUpdateMessage

data class GroupedRegionUpdateMessage(
    val lastKnownRegion: Position,
    val coordinates: RegionCoordinates,
    val messages: Set<RegionUpdateMessage>,
    val region: Position = Position(coordinates.absoluteX, coordinates.absoluteY)
) : Message()