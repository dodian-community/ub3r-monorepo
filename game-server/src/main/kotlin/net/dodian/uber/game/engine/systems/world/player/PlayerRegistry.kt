package net.dodian.uber.game.engine.systems.world.player

import net.dodian.uber.game.Constants
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue
import net.dodian.uber.game.engine.util.Utils
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object PlayerRegistry {
    private val logger: Logger = LoggerFactory.getLogger(PlayerRegistry::class.java)

    @JvmField
    val slotLock: Any = Any()

    @JvmField
    val usedSlots: BitSet = BitSet(Constants.maxPlayers + 1)

    @JvmField
    val playersOnline: ConcurrentHashMap<Long, Client> = ConcurrentHashMap()

    @JvmField
    val allOnline: ConcurrentHashMap<Long, Int> = ConcurrentHashMap()

    @JvmField
    val players: Array<Player?> = arrayOfNulls(Constants.maxPlayers + 1)

    @JvmField
    var cycle: Int = 1

    @JvmField
    var lastchatid: Int = 0

    @JvmStatic
    fun currentTick(): Long = GameCycleClock.currentCycle()

    @JvmStatic
    fun currentTickInt(): Int = currentTick().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    @JvmStatic
    fun validClient(index: Int): Boolean {
        val player = players.getOrNull(index) as? Client ?: return false
        return !player.disconnected && player.dbId >= 0
    }

    @JvmStatic
    fun getClient(index: Int): Client? {
        return players.getOrNull(index) as? Client
    }

    @JvmStatic
    fun initializeSlots() {
        for (i in 1..Constants.maxPlayers) {
            players[i] = null
        }
    }

    @JvmStatic
    fun getLocalPlayers(player: Player): List<Player> {
        val regionX = player.position.x shr 6
        val regionY = player.position.y shr 6
        val locals = ArrayList<Player>(256)
        for (other in players) {
            if (other == null || !other.isActive || other === player) {
                continue
            }
            val otherRegionX = other.position.x shr 6
            val otherRegionY = other.position.y shr 6
            if (kotlin.math.abs(regionX - otherRegionX) <= 1 && kotlin.math.abs(regionY - otherRegionY) <= 1) {
                locals.add(other)
            }
        }
        return locals
    }

    @JvmStatic
    fun getPlayerCount(): Int = playersOnline.size

    @JvmStatic
    fun forEachActivePlayer(consumer: Consumer<Client>?) {
        if (consumer == null) {
            return
        }
        for (client in playersOnline.values) {
            if (isActiveClient(client)) {
                consumer.accept(client)
            }
        }
    }

    @JvmStatic
    fun snapshotActivePlayers(): List<Client> {
        val activePlayers = ArrayList<Client>(kotlin.math.max(1, playersOnline.size))
        forEachActivePlayer(activePlayers::add)
        return activePlayers
    }

    @JvmStatic
    fun snapshotActivePlayersSortedBySlot(): List<Client> {
        val activePlayers = ArrayList(snapshotActivePlayers())
        activePlayers.sortWith(compareBy<Client> { it.slot })
        return activePlayers
    }

    @JvmStatic
    fun isPlayerOn(playerName: String): Boolean {
        val playerId = Utils.playerNameToLong(playerName)
        val existing = playersOnline[playerId] ?: return false

        val stale =
            existing.disconnected ||
                !existing.isActive ||
                existing.channel == null ||
                !existing.channel.isActive
        if (stale) {
            playersOnline.remove(playerId, existing)
            GameThreadTaskQueue.submit { removePlayer(existing) }
            return false
        }

        logger.info("Player is already logged in as: {}", playerName)
        return true
    }

    @JvmStatic
    fun getPlayerID(playerName: String): Int {
        val playerId = Utils.playerNameToLong(playerName)
        return playersOnline[playerId]?.slot ?: -1
    }

    @JvmStatic
    fun removePlayer(player: Player?) {
        val client = player as? Client
        if (client == null) {
            logger.warn("Tried to remove a null player!")
            return
        }

        client.destruct()
        logger.info(
            "Finished removing player: '{}' slot={} active={} disconnected={}",
            client.playerName,
            client.slot,
            client.isActive,
            client.disconnected,
        )

        val slot = client.slot
        if (slot in 1..Constants.maxPlayers) {
            synchronized(slotLock) {
                usedSlots.clear(slot)
            }
            players[slot] = null
        }

        playersOnline.remove(client.longName, client)
        client.isActive = false
        client.disconnected = true
    }

    @JvmStatic
    fun getPlayer(name: String): Player? {
        val playerId = Utils.playerNameToLong(name)
        return playersOnline[playerId]
    }

    private fun isActiveClient(client: Client?): Boolean {
        return client != null &&
            client.isActive &&
            !client.disconnected &&
            client.channel != null &&
            client.channel.isActive
    }
}
