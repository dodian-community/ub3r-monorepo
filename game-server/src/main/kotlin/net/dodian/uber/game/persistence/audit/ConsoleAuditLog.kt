package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectContentRegistry
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionContext
import net.dodian.uber.game.ui.buttons.ButtonClickRequest
import org.slf4j.LoggerFactory

/**
 * Human-readable, toggleable gameplay console audit logs intended for live investigation.
 * These logs are complementary to durable SQL audit tables: they help operators
 * understand what a player was doing immediately before/after an incident.
 */
object ConsoleAuditLog {
    private val chatLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.chat")
    private val commandLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.command")
    private val tradeLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.trade")
    private val duelLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.duel")
    private val bankLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.bank")
    private val shopLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.shop")
    private val itemLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.item")
    private val buttonLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.button")
    private val objectLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.object")
    private val interfaceLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.interface")

    @JvmStatic
    fun publicChat(player: Player, message: String) {
        logChat("PUBLIC", player, message, null)
    }

    @JvmStatic
    fun yellChat(player: Player, message: String) {
        logChat("YELL", player, message, null)
    }

    @JvmStatic
    fun privateChat(sender: Player, receiver: Player, message: String) {
        logChat("PRIVATE", sender, message, receiver)
    }

    @JvmStatic
    fun modChat(player: Player, message: String) {
        logChat("STAFF", player, message, null)
    }

    @JvmStatic
    fun command(player: Player, command: String) {
        if (!commandLogger.isInfoEnabled) return
        commandLogger.info(
            "COMMAND | {} | rights={} | cmd=\"{}\"",
            playerRef(player),
            player.playerRights,
            sanitizeCommand(command),
        )
    }

    @JvmStatic
    fun trade(playerOneId: Int, playerTwoId: Int, playerOneItems: Iterable<GameItem>, playerTwoItems: Iterable<GameItem>, trade: Boolean) {
        if (!tradeLogger.isInfoEnabled) return
        tradeLogger.info(
            "{} | p1={} | offered=[{}] | p2={} | offered=[{}]",
            if (trade) "TRADE COMPLETE" else "DUEL STAKE COMPLETE",
            playerOneId,
            summarizeItems(playerOneItems),
            playerTwoId,
            summarizeItems(playerTwoItems),
        )
    }

    @JvmStatic
    fun duel(player: String, opponent: String, playerStake: String, opponentStake: String, winner: String) {
        if (!duelLogger.isInfoEnabled) return
        duelLogger.info(
            "DUEL COMPLETE | player={} | opponent={} | winner={} | playerStake=[{}] | opponentStake=[{}]",
            player,
            opponent,
            winner,
            sanitizeInlineText(playerStake),
            sanitizeInlineText(opponentStake),
        )
    }

    @JvmStatic
    fun bankDeposit(player: Player, itemId: Int, amountText: String, slot: Int) {
        if (!bankLogger.isInfoEnabled) return
        bankLogger.info(
            "BANK DEPOSIT | {} | item={} | amount={} | inventorySlot={} | pos={}",
            playerRef(player),
            itemRef(itemId),
            amountText,
            slot,
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun bankWithdraw(player: Player, itemId: Int, amountText: String, bankSlot: Int) {
        if (!bankLogger.isInfoEnabled) return
        bankLogger.info(
            "BANK WITHDRAW | {} | item={} | amount={} | bankSlot={} | pos={}",
            playerRef(player),
            itemRef(itemId),
            amountText,
            bankSlot,
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun bankSearch(player: Player, query: String) {
        if (!bankLogger.isInfoEnabled) return
        bankLogger.info(
            "BANK SEARCH | {} | query=\"{}\" | pos={}",
            playerRef(player),
            sanitizeInlineText(query),
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun bankTabAssignment(player: Player, itemId: Int, bankSlot: Int, fromTab: Int, toTab: Int) {
        if (!bankLogger.isInfoEnabled) return
        bankLogger.info(
            "BANK TAB CHANGE | {} | item={} | bankSlot={} | fromTab={} | toTab={} | pos={}",
            playerRef(player),
            itemRef(itemId),
            bankSlot,
            fromTab,
            toTab,
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun shopBuy(player: Player, shopId: Int, slot: Int, itemId: Int, amount: Int, currencyItemId: Int, totalPrice: Int) {
        if (!shopLogger.isInfoEnabled) return
        shopLogger.info(
            "SHOP BUY | {} | shopId={} | shopName={} | slot={} | item={} | amount={} | currency={} | totalPrice={} | pos={}",
            playerRef(player),
            shopId,
            sanitizeInlineText(shopName(shopId)),
            slot,
            itemRef(itemId, amount),
            amount,
            itemRef(currencyItemId),
            totalPrice,
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun shopSell(player: Player, shopId: Int, slot: Int, itemId: Int, amount: Int, currencyItemId: Int, totalPrice: Int) {
        if (!shopLogger.isInfoEnabled) return
        shopLogger.info(
            "SHOP SELL | {} | shopId={} | shopName={} | slot={} | item={} | amount={} | currency={} | totalPrice={} | pos={}",
            playerRef(player),
            shopId,
            sanitizeInlineText(shopName(shopId)),
            slot,
            itemRef(itemId, amount),
            amount,
            itemRef(currencyItemId),
            totalPrice,
            positionRef(player.position),
        )
    }

    @JvmStatic
    fun itemPickup(player: Player, userId: Int, itemId: Int, itemAmount: Int, pos: Position, npc: Boolean) {
        if (!itemLogger.isInfoEnabled) return
        itemLogger.info(
            "ITEM PICKUP | {} | fromId={} | source={} | item={} | pos={}",
            playerRef(player),
            userId,
            if (npc) "npc" else "player",
            itemRef(itemId, itemAmount),
            positionRef(pos),
        )
    }

    @JvmStatic
    fun itemDrop(player: Player, itemId: Int, itemAmount: Int, pos: Position, reason: String) {
        if (!itemLogger.isInfoEnabled) return
        itemLogger.info(
            "ITEM DROP | {} | item={} | reason=\"{}\" | pos={}",
            playerRef(player),
            itemRef(itemId, itemAmount),
            sanitizeInlineText(reason),
            positionRef(pos),
        )
    }

    @JvmStatic
    fun npcDrop(player: Player, npcId: Int, itemId: Int, itemAmount: Int, pos: Position) {
        if (!itemLogger.isInfoEnabled) return
        itemLogger.info(
            "NPC DROP | {} | npcId={} | item={} | pos={}",
            playerRef(player),
            npcId,
            itemRef(itemId, itemAmount),
            positionRef(pos),
        )
    }

    @JvmStatic
    fun itemGathering(player: Player, itemId: Int, itemAmount: Int, pos: Position, reason: String) {
        if (!itemLogger.isInfoEnabled) return
        itemLogger.info(
            "ITEM GATHER | {} | item={} | reason=\"{}\" | pos={}",
            playerRef(player),
            itemRef(itemId, itemAmount),
            sanitizeInlineText(reason),
            positionRef(pos),
        )
    }

    @JvmStatic
    fun button(request: ButtonClickRequest, opcode: Int, handled: Boolean) {
        if (handled) {
            if (!buttonLogger.isInfoEnabled) return
            buttonLogger.info(buttonAuditText(request, opcode, handled))
        } else {
            if (!buttonLogger.isWarnEnabled) return
            buttonLogger.warn(buttonAuditText(request, opcode, handled))
        }
    }

    internal fun buttonAuditText(request: ButtonClickRequest, opcode: Int, handled: Boolean): String =
        buildString {
            append(if (handled) "BUTTON OK" else "BUTTON UNHANDLED")
            append(" | buttonId=")
            append(request.rawButtonId)
            append(" | opcode=")
            append(opcode)
            append(" | activeInterface=")
            append(request.activeInterfaceId)
            append(" | interface=")
            append(request.interfaceId)
            append(" | componentId=")
            append(request.componentId)
            append(" | opIndex=")
            append(request.opIndex)
            if (!handled) {
                append(" | key=")
                append(sanitizeInlineText(request.componentKey))
            }
            append(" | ")
            append(playerRef(request.client))
        }

    @JvmStatic
    fun objectInteraction(
        context: ObjectInteractionContext,
        resolution: ObjectContentRegistry.ObjectResolution?,
        handled: Boolean,
        handlerSource: String? = null,
    ) {
        if (handled) {
            if (!objectLogger.isInfoEnabled) return
            objectLogger.info(
                "OBJECT OK | type={} | option={} | objectId={} | pos={} | source={} | player={}",
                context.type,
                context.option ?: -1,
                context.objectId,
                positionRef(context.position),
                handlerSource ?: resolution?.content?.javaClass?.simpleName ?: "-",
                playerRef(context.client),
            )
        } else {
            if (!objectLogger.isWarnEnabled) return
            objectLogger.warn(
                "OBJECT UNHANDLED | type={} | option={} | objectId={} | pos={} | packetOpcode={} | player={}",
                context.type,
                context.option ?: -1,
                context.objectId,
                positionRef(context.position),
                context.packetOpcode ?: -1,
                playerRef(context.client),
            )
        }
    }

    @JvmStatic
    fun interfaceOpen(player: Player, interfaceId: Int, via: String) {
        if (!interfaceLogger.isInfoEnabled) return
        interfaceLogger.info(
            "INTERFACE OPEN | {} | interface={} | via={}",
            playerRef(player),
            interfaceId,
            via,
        )
    }

    @JvmStatic
    fun interfaceClose(player: Player, interfaceId: Int, via: String) {
        if (!interfaceLogger.isInfoEnabled) return
        interfaceLogger.info(
            "INTERFACE CLOSE | {} | interface={} | via={}",
            playerRef(player),
            interfaceId,
            via,
        )
    }

    private fun logChat(channel: String, player: Player, message: String, receiver: Player?) {
        if (!chatLogger.isInfoEnabled) return
        chatLogger.info(chatAuditText(channel, player, message, receiver))
    }

    internal fun chatAuditText(channel: String, player: Player, message: String, receiver: Player?): String {
        val sanitizedMessage = sanitizeInlineText(message)
        val actor = playerRef(player)
        return if (receiver == null) {
            "$channel CHAT | $actor | msg=\"$sanitizedMessage\""
        } else {
            "$channel CHAT | $actor | receiver=${playerRef(receiver)} | msg=\"$sanitizedMessage\""
        }
    }

    private fun summarizeItems(items: Iterable<GameItem>): String {
        val summary = items.joinToString(", ") { item -> itemRef(item.id, item.amount) }
        return if (summary.isEmpty()) "nothing" else summary
    }

    private fun sanitizeCommand(command: String): String {
        val trimmed = sanitizeInlineText(command)
        return if (trimmed.contains("password", ignoreCase = true)) {
            "<redacted>"
        } else {
            trimmed
        }
    }

    private fun sanitizeInlineText(text: String): String =
        text.replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .replace('"', '\'')
            .trim()

    internal fun playerRef(player: Player): String {
        val name = player.playerName?.takeIf { it.isNotBlank() }?.let(::sanitizeInlineText) ?: "<unknown>"
        val dbId = if (player.dbId >= 0) player.dbId.toString() else "unknown"
        return "player=$name | dbId=$dbId"
    }

    private fun positionRef(position: Position): String = "${position.x},${position.y},${position.z}"

    private fun itemRef(itemId: Int, amount: Int? = null): String {
        val name =
            Server.itemManager?.getName(itemId)
                ?.takeIf { it.isNotBlank() }
                ?: "item#$itemId"
        return if (amount == null) {
            "$name($itemId)"
        } else {
            "$name($itemId)x$amount"
        }
    }

    private fun shopName(shopId: Int): String =
        net.dodian.uber.game.shop.ShopCatalog.find(shopId)?.name
            ?: "shop#$shopId"
}


