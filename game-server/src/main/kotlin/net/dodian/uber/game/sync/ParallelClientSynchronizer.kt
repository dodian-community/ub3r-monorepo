package net.dodian.uber.game.sync

import net.dodian.uber.game.modelkt.area.RegionCoordinates
import net.dodian.uber.game.modelkt.entity.Npc
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.game.sync.task.PhasedSynchronizationTask
import net.dodian.uber.game.sync.task.PlayerSynchronizationTask
import net.dodian.uber.game.sync.task.PostPlayerSynchronizationTask
import net.dodian.uber.game.sync.task.PrePlayerSynchronizationTask
import net.dodian.uber.net.protocol.packets.server.regionupdate.RegionUpdateMessage
import net.dodian.utilities.ThreadUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Phaser

class ParallelClientSynchronizer(
    private val executor: ExecutorService = Executors.newFixedThreadPool(
        ThreadUtil.AVAILABLE_PROCESSORS,
        ThreadUtil.create("ClientSynchronizer")
    )
) : ClientSynchronizer() {

    private val phaser = Phaser(1)

    override fun synchronize(players: Iterable<Player>, npcs: Iterable<Npc>) {
        val playerCount = players.count()
        val npcCount = npcs.count()

        val encodes: MutableMap<RegionCoordinates, Set<RegionUpdateMessage>> = mutableMapOf()
        val updates: MutableMap<RegionCoordinates, Set<RegionUpdateMessage>> = mutableMapOf()

        phaser.bulkRegister(playerCount)
        players.forEach {
            val task = PrePlayerSynchronizationTask(it, encodes, updates)
            executor.submit(PhasedSynchronizationTask(phaser, task))
        }
        phaser.arriveAndAwaitAdvance()

        phaser.bulkRegister(playerCount)
        players.forEach {
            val task = PlayerSynchronizationTask(it)
            executor.submit(PhasedSynchronizationTask(phaser, task))
        }
        phaser.arriveAndAwaitAdvance()

        phaser.bulkRegister(playerCount)
        players.forEach {
            val task = PostPlayerSynchronizationTask(it)
            executor.submit(PhasedSynchronizationTask(phaser, task))
        }
        phaser.arriveAndAwaitAdvance()
    }
}