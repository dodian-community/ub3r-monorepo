package net.dodian.uber.game.modelkt

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.area.RegionRepository
import net.dodian.uber.game.modelkt.area.collision.CollisionManager
import net.dodian.uber.game.modelkt.entity.MobRepository
import net.dodian.uber.game.modelkt.entity.Npc
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.game.scheduling.ScheduledTask
import net.dodian.uber.game.scheduling.Scheduler
import net.dodian.utilities.NameUtil

private val logger = InlineLogger()

const val MAXIMUM_PLAYERS = 2000
const val MAXIMUM_NPCS = 32768

class World(
    val players: MutableMap<Long, Player> = mutableMapOf(),
    val playerRepository: MobRepository<Player> = MobRepository(),
    val npcRepository: MobRepository<Npc> = MobRepository(),
    val regions: RegionRepository = RegionRepository.immutable
) {

    fun initialize() {

    }

    val collisionManager: CollisionManager = CollisionManager(regions)

    fun isPlayerOnline(username: String) = players.containsKey(NameUtil.encodeBase37(username))

    fun register(player: Player) {
        val username = player.username

        playerRepository.add(player)
        players[NameUtil.encodeBase37(username)] = player

        logger.info { "Registered player: ${player.username} [count=${playerRepository.size}]" }
    }

    fun unregister(player: Player) {
        players.remove(NameUtil.encodeBase37(player.username))

        val region = regions.fromPosition(player.position)
        region.removeEntity(player)

        playerRepository.remove(player)
        logger.info { "Unregistered player: ${player.username} [count=${playerRepository.size}]" }
    }

    private val scheduler: Scheduler = Scheduler()
    fun schedule(task: ScheduledTask) = scheduler.schedule(task)

    fun pulse() {
        scheduler.pulse()
    }
}