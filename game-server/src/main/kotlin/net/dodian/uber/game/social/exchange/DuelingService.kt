package net.dodian.uber.game.social.exchange

import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendString
import java.util.Arrays
import java.util.UUID
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventScheduler
import net.dodian.uber.game.engine.systems.action.DuelCountdownService
import net.dodian.uber.game.engine.systems.action.DuelCountdownState
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.model.item.transaction.OfferTransactions
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.DuelArmourUpdate
import net.dodian.uber.game.netty.listener.out.DuelItemsUpdate
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.persistence.audit.DuelLog
import net.dodian.uber.game.persistence.audit.TradeLog
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.player.PlayerSaveService
import net.dodian.uber.game.api.plugin.social.ExchangeKind

/** Kotlin route owner for duel requests and rule mutations. */
object DuelingService {
    private val completedSessions = linkedSetOf<UUID>()
    private val immutableRuleIndices = setOf(3, 4, 5, 9, 10)
    private val ruleConfigIds = intArrayOf(631, 632, 633, 634, 635, 636, 638, 639, 637, 640, 641)
    private val ruleLines = intArrayOf(31045, 31046, 31047, 31048, 31049, 31050, 31052, 31053, 31051, 31054, 31055)
    private val ruleNames = arrayOf("No Ranged", "No Melee", "No Magic", "No Sp. Atk", "Fun Weapons", "No Forfeit",
        "No Drinks", "No Food", "No Prayer", "No Movement", "Obstacles")
    private val bodyConfigValues = intArrayOf(16384, 32768, 65536, 134217728, 131072, 262144, 524288, 2097152, 67108864, 16777216, 8388608)
    private val equipmentSlots = intArrayOf(0, 1, 2, 13, 3, 4, 5, 7, 12, 10, 9)

    @JvmStatic
    fun isEquipmentRestricted(client: Client, equipmentSlot: Int): Boolean {
        val ruleIndex = equipmentSlots.indexOf(equipmentSlot)
        return ruleIndex >= 0 && client.duelBodyRules[ruleIndex]
    }

    @JvmStatic
    fun request(client: Client, targetSlot: Int) {
        val other = client.getClient(targetSlot) ?: return
        client.setFocus(other.position.x, other.position.y)
        PlayerInteractionGuardService.duelBlockMessage(client, other)?.let {
            client.sendMessage(it)
            return
        }
        if (client.isBusy || other.isBusy) {
            client.sendMessage(if (client.isBusy) "You are currently busy" else "${other.playerName} is currently busy!")
            return
        }
        if (CombatLogoutLockService.isLocked(client) || CombatLogoutLockService.isLocked(other)) {
            client.sendMessage(if (CombatLogoutLockService.isLocked(client)) "You can't duel while in combat." else "${other.playerName} can't duel while in combat.")
            return
        }
        if (client.inWildy() || other.inWildy()) {
            client.sendMessage("You cant duel in the wilderness!")
            return
        }
        if (!FeatureStateService.dueling.get()) {
            client.sendMessage("Dueling has been temporarily disabled")
            return
        }
        for (slot in PlayerRegistry.players.indices) {
            val duplicate = client.getClient(slot)
            if (slot != client.slot && client.validClient(slot) && duplicate.dbId > 0 && duplicate.dbId == client.dbId) {
                client.logout()
            }
        }
        client.duel_with = targetSlot
        client.duelRequested = true
        if (!client.validClient(targetSlot)) return
        if (client.isBusy || other.isBusy || other.duelConfirmed || other.duelConfirmed2) {
            client.sendMessage("Other player is busy at the moment")
            client.duelRequested = false
            return
        }
        if (client.tradeLocked && other.playerRights < 1) return
        if (other.duelRequested && other.duel_with == client.slot) {
            open(client)
            open(other)
        } else {
            client.sendMessage("Sending duel request...")
            other.sendMessage("${client.playerName}:duelreq:")
        }
    }

    @JvmStatic
    fun open(client: Client) {
        val other = client.getClient(client.duel_with) ?: return
        if (ExchangeRuntime.session(client, other, ExchangeKind.DUEL) == null) {
            ExchangeRuntime.create(ExchangeKind.DUEL, client, other)
        }
        client.duelRule[9] = true
        refreshRules(client)
        refreshOffer(client)
        client.inDuel = true
        client.sendString("Dueling with: ${other.playerName} (level-${other.determineCombatLevel()})", 31005)
        client.sendString("Opponent's combat level: @or2@${other.determineCombatLevel()}", 31006)
        client.send(SendString("", 31009))
        client.resetItems(3322)
        client.send(InventoryInterface(31000, 3321))
        sendArmour(client, other)
    }

    @JvmStatic
    fun toggleRule(client: Client, ruleIndex: Int): Boolean {
        val other = client.getClient(client.duel_with) ?: return false
        if (ruleIndex !in client.duelRule.indices || ruleIndex in immutableRuleIndices ||
            !ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_RULES, 800L)
        ) return false
        if (!client.inDuel || client.duelFight || client.duelConfirmed2 || other.duelConfirmed2 ||
            client.duelConfirmed && other.duelConfirmed
        ) return false
        val enabled = !client.duelRule[ruleIndex]
        val session = ExchangeRuntime.session(client, other, ExchangeKind.DUEL) ?: return false
        if (session.setRule(ExchangeRuntime.participant(client), ruleIndex, enabled) !is
            net.dodian.uber.game.api.plugin.social.ExchangeCommandResult.Applied
        ) return false
        client.duelRule[ruleIndex] = enabled
        other.duelRule[ruleIndex] = enabled
        client.duelConfirmed = false
        other.duelConfirmed = false
        client.send(SendString("", 31009))
        other.send(SendString("", 31009))
        refreshRules(client)
        refreshRules(other)
        return true
    }

    @JvmStatic
    fun toggleBodyRule(client: Client, ruleIndex: Int): Boolean {
        val other = client.getClient(client.duel_with) ?: return false
        if (ruleIndex !in client.duelBodyRules.indices ||
            !ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_BODY_RULES, 400L)
        ) return false
        if (!client.inDuel || client.duelFight || client.duelConfirmed2 || other.duelConfirmed2 ||
            client.duelConfirmed && other.duelConfirmed
        ) return false
        val enabled = !client.duelBodyRules[ruleIndex]
        val session = ExchangeRuntime.session(client, other, ExchangeKind.DUEL) ?: return false
        if (session.setRule(ExchangeRuntime.participant(client), 11 + ruleIndex, enabled) !is
            net.dodian.uber.game.api.plugin.social.ExchangeCommandResult.Applied
        ) return false
        client.duelBodyRules[ruleIndex] = enabled
        other.duelBodyRules[ruleIndex] = enabled
        client.duelConfirmed = false
        other.duelConfirmed = false
        client.send(SendString("", 31009))
        other.send(SendString("", 31009))
        refreshRules(client)
        refreshRules(other)
        return true
    }

    @JvmStatic
    fun refreshRules(client: Client) {
        client.duelRule.indices.forEach { index ->
            val forcedDisabled = index in immutableRuleIndices - 9
            val enabled = if (index == 9) true else client.duelRule[index] && !forcedDisabled
            val color = if (enabled) "@red@" else if (forcedDisabled) "@or2@" else "@gre@"
            client.send(SendString(color + ruleNames[index], ruleLines[index]))
            client.setVarp(ruleConfigIds[index], if (enabled) 1 else 0)
        }
        val bodyConfig = client.duelBodyRules.indices.filter { client.duelBodyRules[it] }.sumOf { bodyConfigValues[it] }
        client.varbit(286, bodyConfig)
    }

    @JvmStatic
    fun refreshOffer(client: Client) {
        val other = client.getClient(client.duel_with) ?: return
        client.send(DuelItemsUpdate(31012, ExchangeRuntime.offers(client), true))
        client.send(DuelItemsUpdate(31014, ExchangeRuntime.offers(other), true))
    }

    @JvmStatic
    fun showConfirmation(client: Client) {
        val other = client.getClient(client.duel_with) ?: return decline(client)
        client.canOffer = false
        client.resetItems(3322)
        client.send(InventoryInterface(31500, 3321))
        var line = 0
        client.duelRule.indices.forEach { index ->
            if (line >= 15) return@forEach
            val forcedDisabled = index in immutableRuleIndices - 9
            val enabled = if (index == 9) true else client.duelRule[index] && !forcedDisabled
            val color = if (enabled) "@red@" else if (forcedDisabled) "@or2@" else "@gre@"
            client.send(SendString(color + ruleNames[index], 31505 + line++))
        }
        while (line < 15) client.send(SendString("", 31505 + line++))
        writeStakeLines(client, ExchangeRuntime.offers(client), 31531)
        writeStakeLines(client, ExchangeRuntime.offers(other), 31561)
        client.send(SendString("", 31526))
    }

    private fun writeStakeLines(client: Client, items: List<GameItem>, firstLine: Int) {
        var slot = 0
        items.filter { it.id > 0 && it.amount > 0 }.take(28).forEach {
            client.send(SendString("</col>${client.getItemName(it.id)} @whi@x ${String.format("%,d", it.amount)}", firstLine + slot++))
        }
        while (slot < 28) client.send(SendString("", firstLine + slot++))
    }

    @JvmStatic
    fun decline(client: Client) {
        val other = client.getClient(client.duel_with)?.takeIf {
            it !== client && it.inDuel && it.duel_with == client.slot && !it.duelFight
        }
        val participants = listOfNotNull(client, other).distinct()
        ExchangeRuntime.remove(client)
        participants.forEach(::reset)
        participants.forEach {
            refreshRules(it)
            it.failer = ""
            it.faceTarget(-1)
            it.checkItemUpdate()
        }
    }

    @JvmStatic
    fun start(client: Client) {
        val other = client.getClient(client.duel_with) ?: return decline(client)
        client.send(RemoveInterfaces())
        client.contentRuntimeState.setCombatAttackEnabled(false)
        client.canOffer = false
        client.duelFight = true
        client.prayerManager.reset()
        client.addEffectTime(2, 0)
        client.GetBonus(true)
        client.boostedLevel.indices.forEach {
            client.boostedLevel[it] = 0
            Skill.getSkill(it)?.let { skill -> ProgressionService.refresh(client, skill) }
        }
        client.otherdbId = other.dbId
        var state = DuelCountdownState.initial()
        client.requestForceChat("It is time to D-D-D-DUEL!")
        GameEventScheduler.runRepeatingMs(600) {
            val step = DuelCountdownService.advance(state)
            state = step.nextState
            step.forceChat?.let(client::requestForceChat)
            if (step.enableCombat) {
                client.contentRuntimeState.setCombatAttackEnabled(true)
                false
            } else true
        }
    }

    @JvmStatic
    fun reset(client: Client) {
        client.send(RemoveInterfaces())
        client.duelWin = false
        client.canOffer = true
        client.duel_with = 0
        client.duelRequested = false
        client.duelConfirmed = false
        client.duelConfirmed2 = false
        client.duelFight = false
        client.contentRuntimeState.setCombatAttackEnabled(true)
        client.inDuel = false
        client.duelRule = BooleanArray(11)
        Arrays.fill(client.duelBodyRules, false)
        client.otherdbId = -1
        ExchangeRuntime.remove(client)
    }

    @JvmStatic
    fun sendArmour(source: Client, recipient: Client) {
        recipient.send(DuelArmourUpdate(source.equipment, source.equipmentN))
    }

    @JvmStatic
    fun hasEnoughSpace(client: Client): Boolean {
        val other = client.getClient(client.duel_with) ?: return true
        var spaces = client.duelBodyRules.indices.count {
            client.duelBodyRules[it] && client.equipmentN[equipmentSlots[it]] > 0
        }
        val seen = hashSetOf<Int>()
        (ExchangeRuntime.offers(client) + ExchangeRuntime.offers(other)).forEach { item ->
            if (item.amount > 0 && (seen.add(item.id) || !Server.itemManager.isStackable(item.id))) spaces++
        }
        if (spaces > client.freeSpace) {
            val message = "${client.playerName} does not have enough space to hold items being removed and/or staked."
            client.failer = message
            other.failer = message
            return true
        }
        return false
    }

    @JvmStatic
    fun removeEquipment(client: Client) {
        client.duelBodyRules.indices.forEach { index ->
            val slot = equipmentSlots[index]
            if (client.duelBodyRules[index] && client.equipmentN[slot] > 0) {
                net.dodian.uber.game.engine.systems.inventory.EquipmentService.forceUnequipToInventory(client, slot)
                client.checkItemUpdate()
            }
        }
    }

    @JvmStatic
    fun victory(winner: Client) {
        val loser = winner.getClient(winner.duel_with) ?: return
        val session = ExchangeRuntime.session(winner, loser, ExchangeKind.DUEL) ?: return
        val sessionId = session.id
        if (sessionId in completedSessions || !winner.duelFight || !winner.duelWin) return
        if (!ContentInteraction.tryAcquireMs(winner, ContentInteraction.DUEL_ACCEPT_WIN, 1000L)) return

        val winnerOffer = ExchangeRuntime.offers(winner).map { GameItem(it.id, it.amount) }
        val loserOffer = ExchangeRuntime.offers(loser).map { GameItem(it.id, it.amount) }
        if (session.beginSettlement(sessionId) is
            net.dodian.uber.game.api.plugin.social.ExchangeCommandResult.Rejected
        ) return
        val projection = OfferTransactions.projectReservedDuelPayout(winner, loser)
        if (projection == null) {
            session.abortSettlement(sessionId)
            winner.sendMessage("Not enough inventory space to collect your winnings. Free up space and try again.")
            return
        }
        val queued = PlayerSaveService.requestPairedInventorySettlement(
            winner,
            projection.firstAfter.itemIds,
            projection.firstAfter.amounts,
            loser,
            projection.secondAfter.itemIds,
            projection.secondAfter.amounts,
            PlayerSaveReason.DUEL,
            Runnable {
                if (!OfferTransactions.publishProjection(winner, loser, projection)) {
                    winner.logout()
                    loser.logout()
                    return@Runnable
                }
                session.complete(sessionId)
                finishVictory(winner, loser, sessionId, winnerOffer, loserOffer)
            },
            Runnable {
                session.abortSettlement(sessionId)
                winner.sendMessage("Your winnings could not be saved safely. Please try again.")
            },
        )
        if (!queued) {
            session.abortSettlement(sessionId)
            winner.sendMessage("Your winnings are already being processed.")
        }
    }

    private fun finishVictory(
        winner: Client,
        loser: Client,
        sessionId: UUID,
        winnerOffer: List<GameItem>,
        loserOffer: List<GameItem>,
    ) {
        completedSessions += sessionId
        while (completedSessions.size > 4096) completedSessions.remove(completedSessions.first())
        winner.duelWin = false

        val winnerStakeText = winnerOffer.joinToString("") { "(${it.id}, ${it.amount})" }
        val loserStakeText = loserOffer.joinToString("") { "(${it.id}, ${it.amount})" }
        if (winnerOffer.isNotEmpty() || loserOffer.isNotEmpty()) {
            DuelLog.recordDuel(winner.playerName, loser.playerName, winnerStakeText, loserStakeText, winner.playerName)
            if (winner.dbId > loser.dbId) {
                TradeLog.recordTrade(
                    winner.dbId,
                    loser.dbId,
                    java.util.concurrent.CopyOnWriteArrayList(winnerOffer),
                    java.util.concurrent.CopyOnWriteArrayList(loserOffer),
                    false,
                )
            }
        }
        winner.resetAttack()
        winner.sendMessage("You have defeated ${loser.playerName}!")
        winner.sendString(loser.playerName, 31706)
        winner.sendString("${loser.determineCombatLevel()}", 31707)
        winner.send(DuelItemsUpdate(31708, loserOffer, false))
        val totalValue = loserOffer.sumOf { Server.itemManager.getShopSellValue(it.id).toLong() * it.amount }

        reset(loser)
        reset(winner)
        winner.GetBonus(true)
        loser.GetBonus(true)
        winner.faceTarget(-1)
        loser.faceTarget(-1)
        winner.checkItemUpdate()
        loser.checkItemUpdate()
        winner.sendString("<col=E1981F>Total Value: ${String.format("%,d", totalValue)}", 31709)
        winner.openInterface(31700)
        winner.heal(winner.maxHealth)
        winner.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    }
}
