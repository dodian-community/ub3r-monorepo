package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
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
    private val logger = LoggerFactory.getLogger(ConsoleAuditLog::class.java)
    private val chatLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.chat")
    private val commandLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.command")
    private val tradeLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.trade")
    private val duelLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.duel")
    private val bankLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.bank")
    private val equipmentLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.equipment")
    private val shopLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.shop")
    private val itemLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.item")
    private val buttonLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.button")
    private val objectLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.object")
    private val interfaceLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.interface")
    private val npcLogger = LoggerFactory.getLogger("net.dodian.consoleaudit.npc")

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
    fun equipmentWear(player: Player, itemId: Int, amount: Int, inventorySlot: Int, equipmentSlot: Int, displacedItemId: Int?) {
        if (!equipmentLogger.isInfoEnabled) return
        equipmentLogger.info(
            "EQUIPMENT WEAR | {} | item={} | amount={} | inventorySlot={} | equipmentSlot={} | displacedItem={} | pos={}",
            playerRef(player), itemRef(itemId), amount, inventorySlot, equipmentSlot,
            displacedItemId?.let(::itemRef) ?: "none", positionRef(player.position),
        )
    }

    @JvmStatic
    fun equipmentRemove(player: Player, itemId: Int, amount: Int, equipmentSlot: Int) {
        if (!equipmentLogger.isInfoEnabled) return
        equipmentLogger.info(
            "EQUIPMENT REMOVE | {} | item={} | amount={} | equipmentSlot={} | destination=inventory | pos={}",
            playerRef(player), itemRef(itemId), amount, equipmentSlot, positionRef(player.position),
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
    @JvmOverloads
    fun itemDrop(
        player: Player,
        itemId: Int,
        itemAmount: Int,
        pos: Position,
        reason: String,
        invSlot: Int = -1,
        interfaceId: Int = 3214
    ) {
        if (!itemLogger.isInfoEnabled) return
        itemLogger.info(
            "ITEM DROP | {} | item={} | invSlot={} | interface={} | pos={} | reason=\"{}\"",
            playerRef(player),
            itemRef(itemId, itemAmount),
            if (invSlot >= 0) invSlot else "unknown",
            interfaceId,
            positionRef(pos),
            sanitizeInlineText(reason),
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
    fun npcClick(player: Player, npc: Npc, option: Int, opcode: Int) {
        if (!npcLogger.isInfoEnabled) return
        val npcName = Server.npcManager?.getName(npc.id) ?: "N/A"
        val optionLabel = when (option) {
            1 -> "TALK/FIRST"
            2 -> "ATTACK/SECOND"
            3 -> "TRADE/THIRD"
            4 -> "FOURTH"
            5 -> "ATTACK"
            else -> "OPTION_$option"
        }
        npcLogger.info(
            "NPC CLICK | option={} ({}) | opcode={} | npcId={} | npcName=\"{}\" | index={} | pos={} | {}",
            option,
            optionLabel,
            opcode,
            npc.id,
            npcName,
            npc.slot,
            positionRef(player.position),
            playerRef(player),
        )
    }

    @JvmStatic
    fun npcInteraction(
        player: Player,
        npc: Npc,
        option: Int,
        opcode: Int,
        handled: Boolean,
        handlerSource: String? = null,
        routeOutcome: String = "REACHED",
        elapsedNanos: Long = 0L,
    ) {
        val elapsedMs = elapsedNanos.coerceAtLeast(0L) / 1_000_000.0
        val npcName = Server.npcManager?.getName(npc.id) ?: "N/A"
        if (handled) {
            if (!npcLogger.isInfoEnabled) return
            npcLogger.info(
                "NPC OK | option={} | npcId={} | npcName=\"{}\" | index={} | pos={} | route={} | source={} | elapsedMs={} | {}",
                option,
                npc.id,
                npcName,
                npc.slot,
                positionRef(npc.position),
                routeOutcome,
                handlerSource ?: "-",
                String.format(java.util.Locale.ROOT, "%.3f", elapsedMs),
                playerRef(player),
            )
        } else {
            if (!npcLogger.isWarnEnabled) return
            npcLogger.warn(
                "NPC UNHANDLED | option={} | npcId={} | npcName=\"{}\" | index={} | pos={} | route={} | opcode={} | elapsedMs={} | {}",
                option,
                npc.id,
                npcName,
                npc.slot,
                positionRef(npc.position),
                routeOutcome,
                opcode,
                String.format(java.util.Locale.ROOT, "%.3f", elapsedMs),
                playerRef(player),
            )
        }
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
            append(" | pos=")
            append(positionRef(request.client.position))
            append(" | ")
            append(playerRef(request.client))
        }

    @JvmStatic
    fun objectInteraction(
        context: ObjectInteractionContext,
        resolution: ObjectContentRegistry.ObjectResolution?,
        handled: Boolean,
        handlerSource: String? = null,
        routeOutcome: String = "REACHED",
        elapsedNanos: Long = 0L,
    ) {
        val elapsedMs = elapsedNanos.coerceAtLeast(0L) / 1_000_000.0
        if (handled) {
            if (!objectLogger.isInfoEnabled) return
            objectLogger.info(
                "OBJECT OK | type={} | option={} | objectId={} | pos={} | route={} | source={} | elapsedMs={} | {}",
                context.type,
                context.option ?: -1,
                context.objectId,
                positionRef(context.position),
                routeOutcome,
                handlerSource ?: resolution?.content?.javaClass?.simpleName ?: "-",
                String.format(java.util.Locale.ROOT, "%.3f", elapsedMs),
                playerRef(context.client),
            )
        } else {
            if (!objectLogger.isWarnEnabled) return
            objectLogger.warn(
                "OBJECT UNHANDLED | type={} | option={} | objectId={} | pos={} | route={} | packetOpcode={} | elapsedMs={} | {}",
                context.type,
                context.option ?: -1,
                context.objectId,
                positionRef(context.position),
                routeOutcome,
                context.packetOpcode ?: -1,
                String.format(java.util.Locale.ROOT, "%.3f", elapsedMs),
                playerRef(context.client),
            )
        }
    }

    /**
     * Friendly names for interfaces whose ids show up constantly in audit logs. Purely
     * advisory - unknown ids just log without a name.
     */
    private val interfaceNames = mapOf(
        60000 to "bank",
        5292 to "bank (legacy)",
        3824 to "shop",
        3559 to "character design",
        58000 to "teleport menu",
        2400 to "furnace smelting",
        36700 to "account services / modcp",
        12855 to "ancient spellbook",
        40000 to "spellbook",
        3323 to "trade",
        3443 to "trade confirm",
        6575 to "duel config",
        6412 to "duel confirm",
        1119 to "smithing", 1120 to "smithing", 1121 to "smithing", 1122 to "smithing", 1123 to "smithing",
    )

    private fun interfaceRef(interfaceId: Int): String {
        val name = interfaceNames[interfaceId] ?: return interfaceId.toString()
        return "$interfaceId ($name)"
    }

    @JvmStatic
    @JvmOverloads
    fun interfaceOpen(player: Player, interfaceId: Int, via: String, context: String? = null) {
        if (interfaceLogger.isDebugEnabled) {
            interfaceLogger.debug(
                "INTERFACE DETAIL | {} | pos={} | interface={} | via={} | details={}",
                playerRef(player),
                positionRef(player.position),
                interfaceRef(interfaceId),
                via,
                interfaceDetails(player, interfaceId).replace('\n', ' '),
            )
        }
        if (!interfaceLogger.isInfoEnabled) return
        if (context != null) {
            interfaceLogger.info(
                "INTERFACE OPEN | {} | pos={} | interface={} | via={} | context={}",
                playerRef(player),
                positionRef(player.position),
                interfaceRef(interfaceId),
                via,
                sanitizeInlineText(context),
            )
        } else {
            interfaceLogger.info(
                "INTERFACE OPEN | {} | pos={} | interface={} | via={}",
                playerRef(player),
                positionRef(player.position),
                interfaceRef(interfaceId),
                via,
            )
        }
    }

    internal fun interfaceDetails(player: Player, interfaceId: Int): String {
        if (interfaceId == 60000) {
            val client = player as? net.dodian.uber.game.model.entity.player.Client
            return when {
                player.bankStyleViewOpen ->
                    "  [CUSTOM INTERFACE] Bank-style read-only view, Title: '${client?.bankStyleViewTitle.orEmpty()}'"
                player.IsBanking ->
                    "  [CUSTOM INTERFACE] Player bank"
                else ->
                    "  [CUSTOM INTERFACE] Bank container shell (not currently banking)"
            }
        }
        val def = net.dodian.uber.game.engine.systems.cache.CacheInterfaceDefinitions.get(interfaceId)
        if (def != null) {
            val sb = StringBuilder()
            sb.append("  Type: ${def.type}, ParentId: ${def.parentId}")
            if (def.width > 0 || def.height > 0) {
                sb.append(", Size: ${def.width}x${def.height}")
            }
            if (def.tooltip.isNotEmpty()) {
                sb.append(", Tooltip: '${def.tooltip}'")
            }
            if (def.spellName.isNotEmpty()) {
                sb.append(", Spell: '${def.spellName}'")
            }
            if (def.disabledMessage.isNotEmpty()) {
                sb.append(", Text: '${def.disabledMessage}'")
            }
            val actionsList = def.actions?.filterNotNull()?.filter { it.isNotEmpty() }
            if (actionsList != null && actionsList.isNotEmpty()) {
                sb.append(", Actions: $actionsList")
            }

            val children = def.children
            if (children != null && children.isNotEmpty()) {
                val childDetails = mutableListOf<String>()
                for (childId in children) {
                    val childDef = net.dodian.uber.game.engine.systems.cache.CacheInterfaceDefinitions.get(childId)
                    if (childDef != null) {
                        val parts = mutableListOf<String>()
                        if (childDef.disabledMessage.isNotEmpty()) {
                            parts.add("text='${childDef.disabledMessage}'")
                        }
                        if (childDef.tooltip.isNotEmpty()) {
                            parts.add("tooltip='${childDef.tooltip}'")
                        }
                        val childActions = childDef.actions?.filterNotNull()?.filter { it.isNotEmpty() }
                        if (childActions != null && childActions.isNotEmpty()) {
                            parts.add("actions=$childActions")
                        }
                        if (parts.isNotEmpty()) {
                            childDetails.add("    Child $childId: ${parts.joinToString(", ")}")
                        }
                    } else {
                        val customChild = CustomInterfaceRegistry.get(childId)
                        if (customChild != null) {
                            val parts = mutableListOf<String>()
                            if (customChild.text.isNotEmpty()) {
                                parts.add("text='${customChild.text}'")
                            }
                            if (customChild.tooltip.isNotEmpty()) {
                                parts.add("tooltip='${customChild.tooltip}'")
                            }
                            if (parts.isNotEmpty()) {
                                childDetails.add("    Child $childId [Custom]: ${parts.joinToString(", ")}")
                            }
                        }
                    }
                }
                if (childDetails.isNotEmpty()) {
                    sb.append("\n  Children:\n").append(childDetails.joinToString("\n"))
                }
            }
            return sb.toString()
        }

        val customDef = CustomInterfaceRegistry.get(interfaceId) ?: return "  No cache or custom definition found."
        val sb = StringBuilder()
        sb.append("  [CUSTOM INTERFACE] Id: ${customDef.id}")
        if (customDef.text.isNotEmpty()) {
            sb.append(", Text: '${customDef.text}'")
        }
        if (customDef.tooltip.isNotEmpty()) {
            sb.append(", Tooltip: '${customDef.tooltip}'")
        }
        if (customDef.children.isNotEmpty()) {
            val childDetails = mutableListOf<String>()
            for (childId in customDef.children) {
                val childDef = net.dodian.uber.game.engine.systems.cache.CacheInterfaceDefinitions.get(childId)
                if (childDef != null) {
                    val parts = mutableListOf<String>()
                    if (childDef.disabledMessage.isNotEmpty()) {
                        parts.add("text='${childDef.disabledMessage}'")
                    }
                    if (childDef.tooltip.isNotEmpty()) {
                        parts.add("tooltip='${childDef.tooltip}'")
                    }
                    if (parts.isNotEmpty()) {
                        childDetails.add("    Child $childId: ${parts.joinToString(", ")}")
                    }
                } else {
                    val customChild = CustomInterfaceRegistry.get(childId)
                    if (customChild != null) {
                        val parts = mutableListOf<String>()
                        if (customChild.text.isNotEmpty()) {
                            parts.add("text='${customChild.text}'")
                        }
                        if (customChild.tooltip.isNotEmpty()) {
                            parts.add("tooltip='${customChild.tooltip}'")
                        }
                        if (parts.isNotEmpty()) {
                            childDetails.add("    Child $childId [Custom]: ${parts.joinToString(", ")}")
                        }
                    }
                }
            }
            if (childDetails.isNotEmpty()) {
                sb.append("\n  Children:\n").append(childDetails.joinToString("\n"))
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun interfaceClose(player: Player, interfaceId: Int, via: String) {
        if (!interfaceLogger.isInfoEnabled) return
        interfaceLogger.info(
            "INTERFACE CLOSE | {} | pos={} | interface={} | via={}",
            playerRef(player),
            positionRef(player.position),
            interfaceRef(interfaceId),
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

object CustomInterfaceRegistry {
    private val logger = LoggerFactory.getLogger(CustomInterfaceRegistry::class.java)
    data class CustomInterface(
        val id: Int,
        val type: Int = 0,
        val parentId: Int = -1,
        var text: String = "",
        val children: MutableList<Int> = mutableListOf(),
        var tooltip: String = ""
    )

    private val customInterfaces = java.util.concurrent.ConcurrentHashMap<Int, CustomInterface>()
    private val initialized = java.util.concurrent.atomic.AtomicBoolean(false)

    @JvmStatic
    fun get(id: Int): CustomInterface? {
        if (initialized.compareAndSet(false, true)) {
            initialize()
        }
        return customInterfaces[id]
    }

    @JvmStatic
    fun startBackgroundLoad() {
        if (initialized.compareAndSet(false, true)) {
            net.dodian.uber.game.persistence.DbDispatchers.accountExecutor.execute {
                initialize()
            }
        }
    }

    private fun initialize() {
        try {
            val file = java.io.File("game-client/src/main/java/com/osroyale/CustomInterface.java")
            val targetFile = if (file.exists()) file else {
                val alternative = java.io.File("../game-client/src/main/java/com/osroyale/CustomInterface.java")
                if (alternative.exists()) alternative else null
            }
            if (targetFile == null) return
            
            val lines = targetFile.readLines()
            var currentInterface: CustomInterface? = null
            
            val addTabInterfaceRegex = Regex("""addTabInterface\(\s*(\d+)\s*\)""")
            val addInterfaceRegex = Regex("""addInterface\(\s*(\d+)\s*\)""")
            val addTextRegex = Regex("""addText\(\s*(\d+)\s*,\s*"([^"]*)"\s*""")
            val addHoverButtonRegex = Regex("""addHoverButton\(\s*(\d+)\s*,\s*[^,]+,\s*[^,]+,\s*[^,]+,\s*"([^"]*)"\s*""")
            val childRegex = Regex("""\w+\.child\(\s*\d+\s*,\s*(\d+)\s*,"""")
            
            for (line in lines) {
                val trimmed = line.trim()
                
                val tabMatch = addTabInterfaceRegex.find(trimmed)
                val ifaceMatch = addInterfaceRegex.find(trimmed)
                if (tabMatch != null) {
                    val id = tabMatch.groupValues[1].toInt()
                    currentInterface = CustomInterface(id)
                    customInterfaces[id] = currentInterface
                } else if (ifaceMatch != null) {
                    val id = ifaceMatch.groupValues[1].toInt()
                    currentInterface = CustomInterface(id)
                    customInterfaces[id] = currentInterface
                }
                
                val textMatch = addTextRegex.find(trimmed)
                if (textMatch != null) {
                    val id = textMatch.groupValues[1].toInt()
                    val text = textMatch.groupValues[2]
                    val item = customInterfaces.getOrPut(id) { CustomInterface(id) }
                    item.text = text
                }
                
                val hoverMatch = addHoverButtonRegex.find(trimmed)
                if (hoverMatch != null) {
                    val id = hoverMatch.groupValues[1].toInt()
                    val tooltip = hoverMatch.groupValues[2]
                    val item = customInterfaces.getOrPut(id) { CustomInterface(id) }
                    item.tooltip = tooltip
                }
                
                val childMatch = childRegex.find(trimmed)
                if (childMatch != null && currentInterface != null) {
                    val childId = childMatch.groupValues[1].toInt()
                    currentInterface.children.add(childId)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse custom interface source; audit interface details may be incomplete", e)
        }
    }
}
