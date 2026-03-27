package net.dodian.uber.game.model.entity.player

import net.dodian.uber.game.systems.world.player.PlayerRegistry
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class PlayerHandler {
    @JvmField
    var lastchatid: Int = 1

    fun validClient(index: Int): Boolean = PlayerRegistry.validClient(index)

    fun getClient(index: Int): Client? = PlayerRegistry.getClient(index)

    fun removePlayer(player: Player?) {
        PlayerRegistry.removePlayer(player)
    }

    companion object {
        @JvmField
        val SLOT_LOCK: Any = PlayerRegistry.slotLock

        @JvmField
        val usedSlots: BitSet = PlayerRegistry.usedSlots

        @JvmField
        val playersOnline: ConcurrentHashMap<Long, Client> = PlayerRegistry.playersOnline

        @JvmField
        val allOnline: ConcurrentHashMap<Long, Int> = PlayerRegistry.allOnline

        @JvmField
        val players: Array<Player?> = PlayerRegistry.players

        @JvmField
        var cycle: Int = 1

        @JvmStatic
        fun getLocalPlayers(player: Player): List<Player> = PlayerRegistry.getLocalPlayers(player)

        @JvmStatic
        fun getPlayerCount(): Int = PlayerRegistry.getPlayerCount()

        @JvmStatic
        fun forEachActivePlayer(consumer: Consumer<Client>?) {
            PlayerRegistry.forEachActivePlayer(consumer)
        }

        @JvmStatic
        fun snapshotActivePlayers(): List<Client> = PlayerRegistry.snapshotActivePlayers()

        @JvmStatic
        fun isPlayerOn(playerName: String): Boolean = PlayerRegistry.isPlayerOn(playerName)

        @JvmStatic
        fun getPlayerID(playerName: String): Int = PlayerRegistry.getPlayerID(playerName)

        @JvmStatic
        fun getPlayer(name: String): Player? = PlayerRegistry.getPlayer(name)
    }

    init {
        PlayerRegistry.initializeSlots()
    }
}
