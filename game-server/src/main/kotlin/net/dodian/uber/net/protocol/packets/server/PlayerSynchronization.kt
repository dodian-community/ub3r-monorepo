package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.game.modelkt.Position
import net.dodian.uber.net.message.Message

data class PlayerSynchronization(
    val lastKnownRegion: Position,
    val localPlayers: Int,
    val position: Position,
    val hasRegionChanged: Boolean
) : Message()