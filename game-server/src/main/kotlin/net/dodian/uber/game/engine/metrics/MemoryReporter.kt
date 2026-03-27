package net.dodian.uber.game.engine.metrics

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MemoryReporter private constructor() {
    fun process() {
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / MB).toInt()
        val maxMemoryMb = (runtime.maxMemory() / MB).toInt()
        val onlinePlayers = PlayerHandler.getPlayerCount()
        val trackedPlayers = PlayerHandler.playersOnline.size

        logger.info("--------------------------------------------------------------------------------")
        logger.info(
            "Players Online: {} | Memory Usage: {}/{}MB | Players in ConcurrentHashMap: {}",
            onlinePlayers,
            usedMemoryMb,
            maxMemoryMb,
            trackedPlayers,
        )

        val nettyGroups = collectNettyThreadGroups()
        if (nettyGroups.isNotEmpty()) {
            logger.info("Netty Thread Groups:")
            for ((groupName, count) in nettyGroups) {
                logger.info("  {} threads: {}", groupName, count)
            }
        }

        val clients = snapshotActiveClients()
        if (clients.isEmpty()) {
            logger.info("No players online")
            logger.info("--------------------------------------------------------------------------------")
            return
        }

        logger.info("Client Names: {}", clients.joinToString(", ") { it.playerName })
        for (client in clients) {
            logger.info(
                "  Slot {} : {} | disconnected = {} | isActive = {}",
                client.slot,
                client.playerName.padEnd(16),
                client.disconnected,
                client.isActive,
            )
        }
        logger.info("--------------------------------------------------------------------------------")
    }

    private fun collectNettyThreadGroups(): Map<String, Int> {
        val groupedCounts = linkedMapOf<String, Int>()
        for (thread in Thread.getAllStackTraces().keys) {
            val name = thread.name
            if (!name.startsWith("nioEventLoopGroup")) {
                continue
            }
            val lastDash = name.lastIndexOf('-')
            val groupName = if (lastDash > 0) name.substring(0, lastDash) else name
            groupedCounts[groupName] = (groupedCounts[groupName] ?: 0) + 1
        }
        return groupedCounts.toSortedMap()
    }

    private fun snapshotActiveClients(): List<Client> {
        val clients = ArrayList<Client>(PlayerHandler.playersOnline.size)
        for (client in PlayerHandler.playersOnline.values) {
            if (client != null && client.playerName != null) {
                clients.add(client)
            }
        }
        clients.sortBy { it.slot }
        return clients
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MemoryReporter::class.java)
        private const val MB: Long = 1024L * 1024L
        private val singleton: MemoryReporter = MemoryReporter()

        @JvmStatic
        fun getSingleton(): MemoryReporter = singleton
    }
}
