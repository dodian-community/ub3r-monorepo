package net.dodian.uber.game.sync.task

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.context
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.sync.block.SynchronizationBlockSet
import net.dodian.uber.game.sync.segment.MovementSegment
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.protocol.downstream.PlayerSynchronization

private const val MAXIMUM_LOCAL_PLAYERS = 255
private const val NEW_PLAYERS_PER_CYCLE = 20

private val logger = InlineLogger()

class PlayerSynchronizationTask {

    fun execute() {
        context.players.forEach { player ->
            val lastKnownRegion = player.position
            val regionChanged = player.didMapRegionChange()
            val appearanceTickets = player.playerLooks

            // TODO: Get this from the player instead
            val blockSet = SynchronizationBlockSet()

            // TODO: Do ChatBlock

            val position = player.position

            // TODO: Do Teleport or Walk segment
            val segment = MovementSegment(blockSet, arrayOf())

            val localPlayers = player.playerListSize
            val oldCount = localPlayers

            // TODO: Do local players update
            val segments = arrayListOf<SynchronizationSegment>()
            val distance = 15

            player.downstream += PlayerSynchronization(
                localPlayers = localPlayers,
                lastKnownRegion = lastKnownRegion,
                position = position,
                regionChanged = regionChanged,
                segment = segment,
                segments = segments
            )
        }
    }

    private fun Player.preSync() {

    }

    private fun Player.sync() {

    }

    private fun Player.postSync() {

    }
}