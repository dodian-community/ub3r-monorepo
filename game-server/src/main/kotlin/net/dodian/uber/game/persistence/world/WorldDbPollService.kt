package net.dodian.uber.game.persistence.world

import java.sql.Connection
import java.time.Duration
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.utilities.DbTables
import net.dodian.utilities.dbConnection
import org.slf4j.LoggerFactory

object WorldDbPollService {
    private val logger = LoggerFactory.getLogger(WorldDbPollService::class.java)
    private val worker: ExecutorService = DbDispatchers.worldExecutor
    private val inFlight = AtomicReference<CompletableFuture<WorldPollResult>?>()
    private val latestResult = AtomicReference(WorldPollResult.EMPTY)
    private val running = AtomicBoolean(true)

    @JvmStatic
    fun pollAsync(input: WorldPollInput?): CompletableFuture<WorldPollResult> {
        if (input == null || !running.get()) {
            return CompletableFuture.completedFuture(latestResult.get())
        }

        val current = inFlight.get()
        if (current == null || current.isDone) {
            val next = CompletableFuture.supplyAsync({ runBlockingPoll(input) }, worker)
            if (inFlight.compareAndSet(current, next)) {
                next.whenComplete { result, throwable ->
                    if (throwable != null) {
                        logger.error("World DB poll failed", throwable)
                    } else if (result != null) {
                        latestResult.set(result)
                    }
                }
            }
        }

        return inFlight.get() ?: CompletableFuture.completedFuture(latestResult.get())
    }

    @JvmStatic
    fun getLatestResult(): WorldPollResult = latestResult.get()

    @JvmStatic
    fun runBlockingPoll(input: WorldPollInput): WorldPollResult {
        var latestNewsId: Int? = null
        val refundReceivers = HashSet<Int>()
        val muteTimes = HashMap<Int, Long>()
        val bannedPlayers = HashSet<Int>()

        try {
            dbConnection.use { connection ->
                if (input.worldId == 1) {
                    updatePlayerCount(connection, input.worldId, input.playerCount)
                }
                latestNewsId = loadLatestNews(connection)
                refundReceivers += loadRefundReceivers(connection)
                if (refundReceivers.isNotEmpty()) {
                    markRefundMessagesDispatched(connection, refundReceivers)
                }
                loadMuteAndBanState(connection, input.onlinePlayerDbIds, muteTimes, bannedPlayers)
            }
        } catch (exception: Exception) {
            logger.error("World DB polling failed", exception)
        }

        return WorldPollResult(latestNewsId, refundReceivers, muteTimes, bannedPlayers)
    }

    private fun updatePlayerCount(connection: Connection, worldId: Int, playerCount: Int) {
        val query = "UPDATE ${DbTables.GAME_WORLDS} SET players = ? WHERE id = ?"
        connection.prepareStatement(query).use { statement ->
            statement.setInt(1, playerCount)
            statement.setInt(2, worldId)
            statement.executeUpdate()
        }
    }

    private fun loadLatestNews(connection: Connection): Int? {
        val query = "SELECT threadid FROM thread WHERE forumid IN ('98', '99', '101') AND visible = '1' ORDER BY threadid DESC LIMIT 1"
        connection.createStatement().use { statement ->
            statement.executeQuery(query).use { results ->
                return if (results.next()) results.getInt("threadid") else null
            }
        }
    }

    private fun loadRefundReceivers(connection: Connection): Set<Int> {
        val receivers = HashSet<Int>()
        val query = "SELECT DISTINCT receivedBy FROM ${DbTables.GAME_REFUND_ITEMS} WHERE message='0' AND claimed IS NULL"
        connection.createStatement().use { statement ->
            statement.executeQuery(query).use { results ->
                while (results.next()) {
                    receivers += results.getInt("receivedBy")
                }
            }
        }
        return receivers
    }

    private fun markRefundMessagesDispatched(connection: Connection, receivers: Set<Int>) {
        val ids = receivers.joinToString(",")
        val updateQuery = "UPDATE ${DbTables.GAME_REFUND_ITEMS} SET message='1' WHERE message='0' AND claimed IS NULL AND receivedBy IN ($ids)"
        connection.createStatement().use { statement ->
            statement.executeUpdate(updateQuery)
        }
    }

    private fun loadMuteAndBanState(connection: Connection, onlinePlayerDbIds: List<Int>, muteTimes: MutableMap<Int, Long>, bannedPlayers: MutableSet<Int>) {
        if (onlinePlayerDbIds.isEmpty()) return
        val ids = onlinePlayerDbIds.joinToString(",")
        val query = "SELECT id, unmutetime, unbantime FROM ${DbTables.GAME_CHARACTERS} WHERE id IN ($ids)"
        val now = System.currentTimeMillis()
        connection.createStatement().use { statement ->
            statement.executeQuery(query).use { results ->
                while (results.next()) {
                    val id = results.getInt("id")
                    val unmuteTime = results.getLong("unmutetime")
                    val unbanTime = results.getLong("unbantime")
                    muteTimes[id] = unmuteTime
                    if (unbanTime > now) bannedPlayers += id
                }
            }
        }
    }

    @JvmStatic
    fun shutdown(timeout: Duration) {
        running.set(false)
        inFlight.get()?.let { future ->
            try {
                future.get(maxOf(1L, timeout.toMillis() / 2), TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
            }
        }
        DbDispatchers.shutdown(worker, timeout)
    }
}
