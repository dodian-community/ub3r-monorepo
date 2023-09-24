package net.dodian.uber.game.modelkt.entity.player

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.entity.EntityType
import net.dodian.uber.game.modelkt.entity.Mob
import net.dodian.uber.game.sync.block.SynchronizationBlockSet
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.server.IdAssignmentMessage
import net.dodian.uber.net.protocol.packets.server.ServerChatMessage
import net.dodian.uber.session.GameSession
import net.dodian.utilities.CollectionUtil
import net.dodian.utilities.security.PlayerCredentials
import java.util.*
import java.util.ArrayDeque

private val logger = InlineLogger()
@Suppress("MemberVisibilityCanBePrivate")
class Player(
    val credentials: PlayerCredentials,
    override var position: Position,
    override val world: World,
    override val entityType: EntityType = EntityType.PLAYER
) : Mob() {
    private val queuedMessages: Deque<Message> = ArrayDeque()

    var blockSet: SynchronizationBlockSet = SynchronizationBlockSet()
    fun resetBlockSet() {
        blockSet = SynchronizationBlockSet()
    }

    var lastKnownRegion: Position? = null
    val hasLastKnownRegion: Boolean get() = lastKnownRegion != null
    var regionChanged: Boolean = false

    var isTeleporting = false

    var index: Int = Random().nextInt()
    var playerRights: Int = 0
    var premium: Boolean = false

    var session: GameSession? = null

    val username get() = credentials.username
    val isActive get() = index != -1

    fun sendInitialMessages() {
        logger.info { "Sending initial messages to player, $username..." }
        send(IdAssignmentMessage(index, premium))
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
        send(ServerChatMessage(message))
    }

    var viewingDistance = DEFAULT_VIEWING_DISTANCE
    fun resetViewingDistance() {
        viewingDistance = DEFAULT_VIEWING_DISTANCE
    }

    companion object {
        const val DEFAULT_VIEWING_DISTANCE = 15
    }
}