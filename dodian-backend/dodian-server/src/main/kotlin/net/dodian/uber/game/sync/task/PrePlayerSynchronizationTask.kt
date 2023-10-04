package net.dodian.uber.game.sync.task

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.area.RegionCoordinates
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.protocol.packets.server.region.ClearRegionMessage
import net.dodian.uber.net.protocol.packets.server.region.GroupedRegionUpdateMessage
import net.dodian.uber.net.protocol.packets.server.region.RegionChangeMessage
import net.dodian.uber.net.protocol.packets.server.region.RegionUpdateMessage

class PrePlayerSynchronizationTask(
    private val player: Player,
    private val updates: MutableMap<RegionCoordinates, Set<RegionUpdateMessage>>,
    private val encodes: MutableMap<RegionCoordinates, Set<RegionUpdateMessage>>
) : SynchronizationTask() {

    override fun run() {
        val oldPosition = player.position
        player.walkingQueue.pulse()

        var local = true

        if (player.isTeleporting) {
            player.resetViewingDistance()
            local = false
        }

        val position = player.position

        if (!player.hasLastKnownRegion || isRegionUpdateRequired) {
            player.regionChanged = true
            local = false

            player.lastKnownRegion = position
            player.send(RegionChangeMessage(position))
        }

        val repository = player.world.regions
        val oldViewable = repository.fromPosition(oldPosition).surrounding
        val newViewable = repository.fromPosition(position).surrounding

        val differences = HashSet(newViewable)
        differences.retainAll(oldViewable)

        val full = HashSet(newViewable)
        if (local) full.removeAll(oldViewable)

        sendUpdates(player.lastKnownRegion ?: error("What!? Last known region was just set..."), differences, full)
    }

    private val isRegionUpdateRequired: Boolean
        get() {
            val current = player.position
            val last = player.lastKnownRegion ?: error("No last known region for this player...")

            val deltaX = current.localX(last)
            val deltaY = current.localY(last)

            return deltaX <= Position.MAX_DISTANCE || deltaX >= Region.VIEWPORT_WIDTH - Position.MAX_DISTANCE - 1 ||
                    deltaY <= Position.MAX_DISTANCE || deltaY >= Region.VIEWPORT_WIDTH - Position.MAX_DISTANCE - 1
        }

    private fun sendUpdates(position: Position, differences: Set<RegionCoordinates>, full: Set<RegionCoordinates>) {
        val repository = player.world.regions
        val height = position.height

        differences.forEach { coordinates ->
            val messages = updates.computeIfAbsent(coordinates) { repository[it].updates(height) }

            if (messages.isNotEmpty())
                player.send(GroupedRegionUpdateMessage(position, coordinates, messages))
        }

        full.forEach { coordinates ->
            val messages = encodes.computeIfAbsent(coordinates) { repository[it].encode(height) }

            if (messages.isEmpty())
                return@forEach

            player.send(ClearRegionMessage(position, coordinates))
            player.send(GroupedRegionUpdateMessage(position, coordinates, messages))
        }
    }
}