package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.skills.api.SkillMultiSelection

/** Normalizes the active 317 make-1/5/10/all button layouts. */
object SkillMultiButtonService {
    /** Chatbox 4429/1746 "Make X" button — needs a numeric prompt, not a fixed amount. */
    private const val SINGLE_MAKE_X_BUTTON = 1748

    /** [Client.enterAmountId] marker for a pending skill-multi Make X entry. */
    const val ENTER_AMOUNT_SKILL_MULTI = 3

    private val singleAmounts = mapOf(
        // Chatbox 4429/1746 (Tarnish ChatBoxItemDialogue): Make 1 / Make 5 / Make All.
        2799 to 1, 2798 to 5, 1747 to Int.MAX_VALUE,
        // Chatbox 8888 layout.
        10239 to 1, 10238 to 5, 6212 to 10, 6211 to Int.MAX_VALUE,
    )
    private val twoEntryButtons = buildMap {
        intArrayOf(34174, 34173, 34172, 34171).forEachIndexed { i, id -> put(id, 0 to amount(i)) }
        intArrayOf(34170, 34169, 34168, 34167).forEachIndexed { i, id -> put(id, 1 to amount(i)) }
    }
    private val threeEntryButtons = buildMap {
        listOf(
            intArrayOf(34185, 34184, 34183, 34182),
            intArrayOf(34189, 34188, 34187, 34186),
            intArrayOf(34193, 34192, 34191, 34190),
        ).forEachIndexed { entry, ids -> ids.forEachIndexed { i, id -> put(id, entry to amount(i)) } }
    }

    private val makeAllQuantityButtons = mapOf(
        35204 to 1,
        35208 to 5,
        35212 to 10,
        35220 to Int.MAX_VALUE
    )
    private val makeAllItemButtons = mapOf(
        35304 to 0,
        35308 to 1,
        35312 to 2
    )

    @JvmStatic
    fun tryHandle(client: Client, buttonId: Int): Boolean {
        val player = client.asSkillPlayer()
        val config = player.production.pending() ?: return false
        if (buttonId in makeAllQuantityButtons) {
            client.makeAllSelectedAmount = makeAllQuantityButtons.getValue(buttonId)
            return true
        }
        if (buttonId == 35216) {
            client.enterAmountId = ENTER_AMOUNT_SKILL_MULTI
            client.send(SendFrame27())
            return true
        }
        if (buttonId in makeAllItemButtons) {
            val itemIndex = makeAllItemButtons.getValue(buttonId)
            val entry = config.entries.getOrNull(itemIndex) ?: run {
                player.production.clear()
                return false
            }
            val qty = if (client.makeAllSelectedAmount <= 0) 1 else client.makeAllSelectedAmount
            return player.production.select(SkillMultiSelection(config.key, entry.recipe.key, qty))
        }
        if (buttonId == SINGLE_MAKE_X_BUTTON) {
            // Keep the pending selection alive; the entered amount arrives through
            // PacketBankingService.handleXAmount -> completeEnteredAmount.
            client.enterAmountId = ENTER_AMOUNT_SKILL_MULTI
            client.send(SendFrame27())
            return true
        }
        val (entryIndex, amount) = when {
            buttonId in singleAmounts -> 0 to singleAmounts.getValue(buttonId)
            buttonId in twoEntryButtons -> twoEntryButtons.getValue(buttonId)
            buttonId in threeEntryButtons -> threeEntryButtons.getValue(buttonId)
            else -> return false
        }
        val entry = config.entries.getOrNull(entryIndex) ?: run {
            player.production.clear()
            return false
        }
        return player.production.select(SkillMultiSelection(config.key, entry.recipe.key, amount))
    }

    /** Completes a Make X selection once the numeric amount arrives from the client. */
    @JvmStatic
    fun completeEnteredAmount(client: Client, amount: Int): Boolean {
        val player = client.asSkillPlayer()
        val config = player.production.pending() ?: return false
        if (amount < 1) {
            player.production.clear()
            return true
        }
        client.makeAllSelectedAmount = amount
        val entry = config.entries.firstOrNull() ?: run {
            player.production.clear()
            return false
        }
        return player.production.select(SkillMultiSelection(config.key, entry.recipe.key, amount))
    }

    @JvmStatic
    fun isSelectionButton(buttonId: Int): Boolean =
        buttonId == SINGLE_MAKE_X_BUTTON || buttonId in singleAmounts ||
            buttonId in twoEntryButtons || buttonId in threeEntryButtons ||
            buttonId in makeAllQuantityButtons || buttonId in makeAllItemButtons || buttonId == 35216

    private fun amount(index: Int) = when (index) { 0 -> 1; 1 -> 5; 2 -> 10; else -> Int.MAX_VALUE }
}
