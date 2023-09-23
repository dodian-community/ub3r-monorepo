package net.dodian.uber.game.modelkt.entity

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.server.IdAssignment
import net.dodian.uber.net.protocol.packets.server.ServerChat
import net.dodian.uber.session.GameSession
import net.dodian.utilities.CollectionUtil
import net.dodian.utilities.security.PlayerCredentials
import java.util.*

private val logger = InlineLogger()

class Player(
    val credentials: PlayerCredentials,
    override val type: EntityType = EntityType.PLAYER
) : Mob() {
    private val queuedMessages: Deque<Message> = ArrayDeque()

    var index: Int = Random().nextInt()
    var playerRights: Int = 0
    var premium: Boolean = false

    var session: GameSession? = null

    val username get() = credentials.username
    val isActive get() = index != -1

    fun sendInitialMessages() {
        logger.info { "Sending initial messages to player, $username..." }
        //send(IdAssignment(index, premium))
        sendMessage("Welcome to Dodian OSRS!")
    }

    fun send(message: Message) {
        if (!isActive) {
            queuedMessages.add(message)
            return
        }

        if (queuedMessages.isNotEmpty())
            CollectionUtil.pollAll(queuedMessages, session!!::dispatchMessage)

        session!!.dispatchMessage(message)
    }

    fun sendMessage(message: String) {
        send(ServerChat(message))
    }
}