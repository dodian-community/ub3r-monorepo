package net.dodian.uber.game.sync.task

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.game.sync.block.ChatBlock
import net.dodian.uber.game.sync.segment.MovementSegment
import net.dodian.uber.game.sync.segment.RemoveMobSegment
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.game.sync.segment.TeleportSegment
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronizationMessage

private val logger = InlineLogger()

class PlayerSynchronizationTask(private val player: Player) : SynchronizationTask() {

    override fun run() {
        val lastKnownRegion = player.lastKnownRegion ?: error("Uhm, what the fuck!? No last known region!??")
        val regionChanged = player.regionChanged
        val appearanceTickets = player.appearanceTickets

        var blockSet = player.blockSet

        if (blockSet.contains(ChatBlock::class)) {
            blockSet = blockSet.clone()
            blockSet.remove<ChatBlock>(ChatBlock::class)
        }

        val position = player.position

        val segment = if (player.isTeleporting || player.regionChanged) {
            TeleportSegment(position, blockSet)
        } else MovementSegment(player.directions, blockSet)

        // TODO:
        val localPlayers = mutableListOf<Player>()
        val oldCount = 0

        val segments = arrayListOf<SynchronizationSegment>()
        val distance = player.viewingDistance

        localPlayers.forEach { localPlayer ->
            if (canRemove(position, distance, localPlayer)) {
                localPlayers.remove(localPlayer)
                segments.add(RemoveMobSegment())
            } else segments.add(MovementSegment(localPlayer.directions, localPlayer.blockSet))
        }

        var added = 0
        val count = localPlayers.size

        val repository = player.world.regions
        val current = repository.fromPosition(position)

        val regions = current.surrounding
        regions.add(current.coordinates)

        // TODO: Do local players stuff here

        player.send(
            PlayerSynchronizationMessage(
                lastKnownRegion = lastKnownRegion,
                localPlayers = oldCount,
                position = position,
                hasRegionChanged = regionChanged,
                segments = segments,
                segment = segment
            )
        )
    }

    private fun canRemove(position: Position, distance: Int, other: Player): Boolean {
        if (other.isTeleporting || !other.isActive)
            return true

        val otherPosition = other.position
        return otherPosition.longestDelta(position) > distance || !otherPosition.isWithinDistance(position, distance)
    }
}