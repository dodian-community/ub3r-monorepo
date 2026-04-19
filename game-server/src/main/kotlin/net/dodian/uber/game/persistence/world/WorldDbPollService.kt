package net.dodian.uber.game.persistence.world

import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
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
            DbAsyncRepository.withConnection { connection ->
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
        } catch (exception: SQLException) {
            logger.error("World DB polling failed due to SQL error", exception)
        } catch (exception: RuntimeException) {
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
        connection.prepareStatement(query).use { statement ->
            statement.executeQuery().use { results ->
                return if (results.next()) results.getInt("threadid") else null
            }
        }
    }

    private fun loadRefundReceivers(connection: Connection): Set<Int> {
        val receivers = HashSet<Int>()
        val query = "SELECT DISTINCT receivedBy FROM ${DbTables.GAME_REFUND_ITEMS} WHERE message='0' AND claimed IS NULL"
        connection.prepareStatement(query).use { statement ->
            statement.executeQuery().use { results ->
                while (results.next()) {
                    receivers += results.getInt("receivedBy")
                }
            }
        }
        return receivers
    }

    private fun markRefundMessagesDispatched(connection: Connection, receivers: Set<Int>) {
        if (receivers.isEmpty()) {
            return
        }
        val ids = normalizedIds(receivers)
        val updateQuery =
            "UPDATE ${DbTables.GAME_REFUND_ITEMS} SET message='1' WHERE message='0' AND claimed IS NULL AND receivedBy IN (${inClausePlaceholders(ids)})"
        connection.prepareStatement(updateQuery).use { statement ->
            bindIds(statement, startIndex = 1, ids = ids)
            statement.executeUpdate()
        }
    }

    private fun loadMuteAndBanState(connection: Connection, onlinePlayerDbIds: List<Int>, muteTimes: MutableMap<Int, Long>, bannedPlayers: MutableSet<Int>) {
        if (onlinePlayerDbIds.isEmpty()) return
        val ids = normalizedIds(onlinePlayerDbIds)
        if (ids.isEmpty()) {
            return
        }
        val query = "SELECT id, unmutetime, unbantime FROM ${DbTables.GAME_CHARACTERS} WHERE id IN (${inClausePlaceholders(ids)})"
        val now = System.currentTimeMillis()
        connection.prepareStatement(query).use { statement ->
            bindIds(statement, startIndex = 1, ids = ids)
            statement.executeQuery().use { results ->
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
            } catch (_: TimeoutException) {
                logger.warn("Timed out waiting for in-flight world poll result during shutdown")
            } catch (_: CancellationException) {
                logger.info("In-flight world poll was cancelled during shutdown")
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: ExecutionException) {
            }
        }
        DbDispatchers.shutdown(worker, timeout)
    }

    internal fun normalizedIds(ids: Iterable<Int>): List<Int> {
        val unique = LinkedHashSet<Int>()
        for (id in ids) {
            if (id > 0) {
                unique += id
            }
        }
        return unique.toList()
    }

    internal fun inClausePlaceholders(ids: List<Int>): String = List(ids.size.coerceAtLeast(1)) { "?" }.joinToString(",")

    private fun bindIds(statement: java.sql.PreparedStatement, startIndex: Int, ids: List<Int>) {
        var index = startIndex
        for (id in ids) {
            statement.setInt(index++, id)
        }
    }
}
