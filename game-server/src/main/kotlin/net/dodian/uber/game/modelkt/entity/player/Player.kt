package net.dodian.uber.game.modelkt.entity.player

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.MAXIMUM_PLAYERS
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.entity.EntityType
import net.dodian.uber.game.modelkt.entity.Mob
import net.dodian.uber.game.modelkt.inter.InterfaceSet
import net.dodian.uber.game.modelkt.inventory.Inventory
import net.dodian.uber.game.sync.block.AppearanceBlock
import net.dodian.uber.game.sync.block.SynchronizationBlockSet
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.server.ConfigMessage
import net.dodian.uber.net.protocol.packets.server.IdAssignmentMessage
import net.dodian.uber.net.protocol.packets.server.ServerChatMessage
import net.dodian.uber.session.GameSession
import net.dodian.utilities.CollectionUtil
import net.dodian.utilities.security.PlayerCredentials
import java.util.*
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

private val logger = InlineLogger()

@Suppress("MemberVisibilityCanBePrivate")
class Player(
    val credentials: PlayerCredentials,
    override var position: Position,
    override val world: World,
    override val entityType: EntityType = EntityType.PLAYER
) : Mob() {
    private val queuedMessages: Deque<Message> = ArrayDeque()

    val interfaceSet: InterfaceSet = InterfaceSet(this)

    private var running: Boolean = false
    val isRunning get() = running
    fun toggleRunning() {
        running = !running
        walkingQueue.running = running
        send(ConfigMessage(173, if (running) 1 else 0))
    }

    var lastKnownRegion: Position? = null
    val hasLastKnownRegion: Boolean get() = lastKnownRegion != null
    var regionChanged: Boolean = false

    var playerRights: Int = 0
    var premium: Boolean = false

    var session: GameSession? = null

    val username get() = credentials.username
    val encodedName get() = credentials.usernameEncoded

    val directions: Array<Direction>
        get() {
            if (firstDirection == Direction.NONE)
                return Direction.EMPTY_DIRECTION_ARRAY

            return when (secondDirection == Direction.NONE) {
                true -> arrayOf(firstDirection)
                false -> arrayOf(firstDirection, secondDirection)
            }
        }

    fun sendInitialMessages() {
        logger.info { "Sending initial messages to player, $username..." }
        updateAppearance()
        send(IdAssignmentMessage(index, premium))
        sendMessage("Welcome to Dodian OSRS!")
    }

    fun send(message: Message) {
        if (!isActive) {
            logger.info { "Player is inactive..." }
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


    var appearance: Appearance = Appearance.DEFAULT
    var appearanceTicket: Int = nextAppearanceTicket()
    val appearanceTickets: IntArray = IntArray(MAXIMUM_PLAYERS)

    fun updateAppearance() {
        appearanceTicket = nextAppearanceTicket()
        blockSet.add(AppearanceBlock.createFrom(this))
    }

    companion object {
        const val DEFAULT_VIEWING_DISTANCE = 15

        var appearanceTicketCounter: Int = 0

        fun nextAppearanceTicket(): Int {
            if (appearanceTicketCounter.also { appearanceTicketCounter++ } == 0)
                appearanceTicketCounter = 1

            return appearanceTicketCounter
        }
    }
}