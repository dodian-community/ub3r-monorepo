package net.dodian.uber.game.persistence.audit

import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.social.chat.YellSystem
import org.slf4j.LoggerFactory

object ChatLog {
    private val logger = LoggerFactory.getLogger(ChatLog::class.java)
    private val messageQueue = LinkedBlockingQueue<ChatMessage>()
    private val startLock = Any()
    private val started = AtomicBoolean(false)
    private val running = AtomicBoolean(true)
    private val workerScheduled = AtomicBoolean(false)
    private val processedMessages = AtomicLong(0)
    private val failedMessages = AtomicLong(0)

    private const val DEBUG_METRICS = false
    private const val BATCH_SIZE = 20
    private const val MAX_RETRY_ATTEMPTS = 2
    private const val SHUTDOWN_WAIT_MS = 5_000L
    private val insertSql =
        "INSERT INTO ${DbTables.GAME_CHAT_LOGS} (type, sender, receiver, message, timestamp) VALUES (?, ?, ?, ?, ?)"

    private data class ChatMessage(
        val type: Int,
        val senderId: Int,
        val receiverId: Int,
        val senderName: String,
        val message: String,
    )

    @JvmStatic
    fun recordPublicChat(player: Player, message: String) {
        ConsoleAuditLog.publicChat(player, message)
        enqueue(1, player.dbId, -1, player.playerName, message)
    }

    @JvmStatic
    fun recordYellChat(player: Player, message: String) {
        ConsoleAuditLog.yellChat(player, message)
        enqueue(2, player.dbId, -1, player.playerName, message)
    }

    @JvmStatic
    fun recordPrivateChat(sender: Player, receiver: Player, message: String) {
        ConsoleAuditLog.privateChat(sender, receiver, message)
        enqueue(3, sender.dbId, receiver.dbId, sender.playerName, message)
    }

    @JvmStatic
    fun recordModChat(player: Player, message: String) {
        ConsoleAuditLog.modChat(player, message)
        enqueue(4, player.dbId, -1, player.playerName, message)
    }

    private fun enqueue(type: Int, senderId: Int, receiverId: Int, senderName: String, message: String) {
        if (gameWorldId > 1) return
        ensureStarted()
        messageQueue.add(ChatMessage(type, senderId, receiverId, senderName, sanitizeMessage(message)))
        scheduleDrain()
    }

    private fun ensureStarted() {
        if (started.get()) return
        synchronized(startLock) {
            if (!started.compareAndSet(false, true)) return
            logger.info("ChatLog processor started.")
        }
    }

    private fun scheduleDrain() {
        if (!workerScheduled.compareAndSet(false, true)) {
            return
        }
        DbDispatchers.logExecutor.execute {
            try {
                drainQueue()
            } catch (exception: RuntimeException) {
                logger.error("Chat log drain worker failed", exception)
            } finally {
                workerScheduled.set(false)
                if ((running.get() || messageQueue.isNotEmpty()) && messageQueue.isNotEmpty()) {
                    scheduleDrain()
                }
            }
        }
    }

    private fun drainQueue() {
        while (running.get() || messageQueue.isNotEmpty()) {
            val batch = ArrayList<ChatMessage>(BATCH_SIZE)
            messageQueue.drainTo(batch, BATCH_SIZE)
            if (batch.isEmpty()) {
                break
            }
            processBatch(batch)
        }
    }

    private fun processBatch(batch: List<ChatMessage>): Int {
        if (batch.isEmpty()) return 0

        return try {
            DbAsyncRepository.withConnection { connection ->
                connection.prepareStatement(insertSql).use { statement ->
                    val timestamp = LogEntry.getTimeStamp()
                    for (message in batch) {
                        statement.setInt(1, message.type)
                        statement.setInt(2, message.senderId)
                        statement.setInt(3, message.receiverId)
                        statement.setString(4, message.message)
                        statement.setString(5, timestamp)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
            processedMessages.addAndGet(batch.size.toLong())
            batch.size
        } catch (exception: java.sql.SQLException) {
            logger.error("Failed to save chat batch", exception)
            for (message in batch) {
                var persisted = false
                var attempts = 0
                while (!persisted && attempts <= MAX_RETRY_ATTEMPTS) {
                    attempts++
                    persisted = saveMessage(message)
                }
                if (!persisted) {
                    failedMessages.incrementAndGet()
                }
            }
            batch.size
        }
    }

    private fun saveMessage(message: ChatMessage): Boolean {
        try {
            DbAsyncRepository.withConnection { connection ->
                saveMessage(connection, message, LogEntry.getTimeStamp())
            }
            processedMessages.incrementAndGet()
            if (DEBUG_METRICS) {
                logger.debug(
                    "Chat log metrics: processed={} failed={} queue={}",
                    processedMessages.get(),
                    failedMessages.get(),
                    messageQueue.size,
                )
            }
            return true
        } catch (exception: java.sql.SQLException) {
            logger.error("Unable to record chat message", exception)
            if (message.type == 1 || message.type == 2) {
                YellSystem.alertStaff("Unable to record chat, please contact an admin.")
            }
            return false
        }
    }

    private fun saveMessage(connection: Connection, message: ChatMessage, timestamp: String) {
        connection.prepareStatement(insertSql).use { statement ->
            statement.setInt(1, message.type)
            statement.setInt(2, message.senderId)
            statement.setInt(3, message.receiverId)
            statement.setString(4, message.message)
            statement.setString(5, timestamp)
            statement.executeUpdate()
        }
    }

    private fun sanitizeMessage(message: String): String = message.replace("'", "`").replace("\\", "/")

    @JvmStatic
    fun shutdown() {
        running.set(false)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SHUTDOWN_WAIT_MS)
        while (System.nanoTime() < deadline) {
            if (messageQueue.isEmpty() && !workerScheduled.get()) {
                break
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
        }
        if (DEBUG_METRICS || failedMessages.get() > 0L) {
            logger.info(
                "Chat log shutdown stats: processed={} failed={} remaining={}",
                processedMessages.get(),
                failedMessages.get(),
                messageQueue.size,
            )
        }
    }
}
