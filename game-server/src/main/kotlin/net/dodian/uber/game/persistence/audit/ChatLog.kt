package net.dodian.uber.game.persistence.audit

import java.sql.Connection
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import net.dodian.uber.game.model.YellSystem
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.gameWorldId
import org.slf4j.LoggerFactory

object ChatLog {
    private val logger = LoggerFactory.getLogger(ChatLog::class.java)
    private val messageQueue: BlockingQueue<ChatMessage> = LinkedBlockingQueue()
    private val startLock = Any()

    @Volatile
    private var running = false

    @Volatile
    private var processorThread: Thread? = null

    private const val DEBUG_METRICS = true
    private const val BATCH_SIZE = 20
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
        enqueue(1, player.dbId, -1, player.playerName, message)
    }

    @JvmStatic
    fun recordYellChat(player: Player, message: String) {
        enqueue(2, player.dbId, -1, player.playerName, message)
    }

    @JvmStatic
    fun recordPrivateChat(sender: Player, receiver: Player, message: String) {
        enqueue(3, sender.dbId, receiver.dbId, sender.playerName, message)
    }

    @JvmStatic
    fun recordModChat(player: Player, message: String) {
        enqueue(4, player.dbId, -1, player.playerName, message)
    }

    private fun enqueue(type: Int, senderId: Int, receiverId: Int, senderName: String, message: String) {
        if (gameWorldId > 1) return
        ensureStarted()
        messageQueue.add(ChatMessage(type, senderId, receiverId, senderName, sanitizeMessage(message)))
    }

    private fun ensureStarted() {
        if (running) return
        synchronized(startLock) {
            if (running) return
            running = true
            processorThread = Thread(::processMessages, "ChatLog-Processor").apply {
                isDaemon = true
                start()
            }
            logger.info("ChatLog processor started.")
        }
    }

    private fun processMessages() {
        while (running || messageQueue.isNotEmpty()) {
            try {
                if (messageQueue.size > 1) {
                    processBatch()
                } else {
                    val message = messageQueue.poll(10, TimeUnit.MILLISECONDS)
                    if (message != null) {
                        val start = System.currentTimeMillis()
                        saveMessage(message)
                        if (DEBUG_METRICS) {
                            val duration = System.currentTimeMillis() - start
                            logger.info("{}: {} saved in {}ms", message.senderName, message.message, duration)
                        }
                    }
                }
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.warn("ChatLog processor thread interrupted")
            } catch (exception: Exception) {
                logger.error("Error processing chat messages", exception)
                try {
                    Thread.sleep(100)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    private fun processBatch(): Int {
        val batch = ArrayList<ChatMessage>(BATCH_SIZE)
        messageQueue.drainTo(batch, BATCH_SIZE)
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
            batch.size
        } catch (exception: java.sql.SQLException) {
            logger.error("Failed to save chat batch", exception)
            for (message in batch) {
                try {
                    saveMessage(message)
                } catch (fallbackException: Exception) {
                    logger.error("Failed to save individual message", fallbackException)
                }
            }
            batch.size
        }
    }

    private fun saveMessage(message: ChatMessage) {
        try {
            DbAsyncRepository.withConnection { connection ->
                saveMessage(connection, message, LogEntry.getTimeStamp())
            }
        } catch (exception: java.sql.SQLException) {
            logger.error("Unable to record chat message", exception)
            if (message.type == 1 || message.type == 2) {
                YellSystem.alertStaff("Unable to record chat, please contact an admin.")
            }
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
        synchronized(startLock) {
            running = false
            processorThread?.interrupt()
            processorThread?.join(5_000L)
            processorThread = null
        }
    }
}
