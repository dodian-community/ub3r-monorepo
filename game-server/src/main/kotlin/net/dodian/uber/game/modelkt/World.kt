package net.dodian.uber.game.modelkt

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.entity.MobRepository
import net.dodian.uber.game.modelkt.entity.Npc
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.utilities.NameUtil

private val logger = InlineLogger()
class World(
    val players: MutableMap<Long, Player> = mutableMapOf(),
    val playerRepository: MobRepository<Player> = MobRepository(),
    val npcRepository: MobRepository<Npc> = MobRepository()
) {

    fun isPlayerOnline(username: String) = players.containsKey(NameUtil.encodeBase37(username))

    fun register(player: Player) {
        val username = player.username

        playerRepository.add(player)
        players[NameUtil.encodeBase37(username)] = player

        logger.info { "Registered player: ${player.username} [count=${playerRepository.size}]" }
    }

    fun unregister(player: Player) {
        players.remove(NameUtil.encodeBase37(player.username))

        val region = Region()
        region.removeEntity(player)

        playerRepository.remove(player)
        logger.info { "Unregistered player: ${player.username} [count=${playerRepository.size}]" }
    }

    fun pulse() {

    }
}