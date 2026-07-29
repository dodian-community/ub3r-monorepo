package net.dodian.uber.game.model.item.transaction

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.ui.bank.PlayerBankService
import net.dodian.uber.game.social.exchange.ExchangeRuntime

// ---------------------------------------------------------------------------
// Container types
// ---------------------------------------------------------------------------

/** Identifies which container a SelectInv wraps. Used during commit to call the right refresh. */
enum class InvContainerKind { INVENTORY, BANK }

/**
 * A thin wrapper returned by [Client.selectInv] / [Client.selectBank] / [Client.selectOffer].
 * Holds the [TransactionInventory] image that the transaction engine will work on, plus a
 * closure that knows how to push that image back to the client's live container. Call [commit]
 * after a successful transaction to apply it.
 */
class SelectInv(
    val client: Client,
    val kind: InvContainerKind,
    val transactionInv: TransactionInventory<GameItem?>,
    private val applyCommit: (Array<TransactionObj?>) -> Unit,
    private val validateCommit: (Array<TransactionObj?>) -> Boolean = { true },
) {
    /**
     * Applies the transaction image back to the live container and notifies the client.
     * Only call this after a successful transaction.
     */
    fun commit() {
        applyCommit(transactionInv.image)
        when (kind) {
            InvContainerKind.INVENTORY -> {
                client.markSaveDirty(PlayerSaveSegment.INVENTORY.mask)
                client.checkItemUpdate()
            }
            InvContainerKind.BANK -> {
                client.markSaveDirty(PlayerSaveSegment.BANK.mask)
                PlayerBankService.checkItemUpdate(client)
            }
        }
    }

    fun canCommit(): Boolean = validateCommit(transactionInv.image)
}

// ---------------------------------------------------------------------------
// Extension functions to create SelectInv wrappers on Client
// ---------------------------------------------------------------------------

/**
 * Wraps the player's live inventory arrays as a [SelectInv] ready for use with
 * the [Transaction] engine. The image is a copy — changes are staged until you call
 * [SelectInv.commit].
 */
fun Client.selectInv(): SelectInv {
    val size = playerItems.size
    val image = Array<TransactionObj?>(size) { slot ->
        val raw = playerItems[slot]
        if (raw > 0) TransactionObj(raw - 1, playerItemsN[slot]) else null
    }
    val stackType = TransactionInventory.NormalStack
    @Suppress("UNCHECKED_CAST")
    val outputArr: Array<GameItem?> = arrayOfNulls<GameItem>(size) as Array<GameItem?>
    val txInv = TransactionInventory<GameItem?>(stackType, outputArr, image)
    val applyCommit: (Array<TransactionObj?>) -> Unit = { finalImage ->
        for (slot in finalImage.indices) {
            val obj = finalImage[slot]
            if (obj != null) {
                playerItems[slot] = obj.id + 1
                playerItemsN[slot] = obj.count
            } else {
                playerItems[slot] = 0
                playerItemsN[slot] = 0
            }
        }
    }
    val validateCommit: (Array<TransactionObj?>) -> Boolean = { finalImage ->
        ExchangeRuntime.mayMutateInventory(this) && finalImage.indices.all { slot ->
            val beforeId = playerItems[slot] - 1
            val beforeAmount = if (beforeId >= 0) playerItemsN[slot] else 0
            val after = finalImage[slot]
            val removed =
                if (beforeId >= 0 && (after == null || after.id != beforeId)) beforeAmount
                else if (beforeId >= 0) (beforeAmount - after!!.count).coerceAtLeast(0)
                else 0
            removed == 0 || ExchangeRuntime.mayRemove(this, slot, beforeId, removed)
        }
    }
    return SelectInv(this, InvContainerKind.INVENTORY, txInv, applyCommit, validateCommit)
}

/**
 * Wraps the player's live bank arrays as a [SelectInv] ready for use with
 * the [Transaction] engine.
 */
fun Client.selectBank(): SelectInv {
    val size = bankSize()
    val image = Array<TransactionObj?>(size) { slot ->
        val raw = bankItems[slot]
        if (raw > 0) TransactionObj(raw - 1, bankItemsN[slot]) else null
    }
    // Bank always stacks — each item occupies one slot with a count.
    val stackType = TransactionInventory.AlwaysStack
    @Suppress("UNCHECKED_CAST")
    val outputArr: Array<GameItem?> = arrayOfNulls<GameItem>(size) as Array<GameItem?>
    val txInv = TransactionInventory<GameItem?>(stackType, outputArr, image)
    val applyCommit: (Array<TransactionObj?>) -> Unit = { finalImage ->
        for (slot in finalImage.indices) {
            val obj = finalImage[slot]
            if (obj != null) {
                bankItems[slot] = obj.id + 1
                bankItemsN[slot] = obj.count
            } else {
                bankItems[slot] = 0
                bankItemsN[slot] = 0
            }
        }
    }
    return SelectInv(this, InvContainerKind.BANK, txInv, applyCommit)
}

// ---------------------------------------------------------------------------
// Build a Transaction pre-configured for this server's item data
// ---------------------------------------------------------------------------

/**
 * Creates a [Transaction] configured with the stackable-item lookup from [Server.itemManager].
 * The cert/placeholder/transform lookups are left empty — this server does not use those
 * RS3-style systems. Wire them in here if/when they are introduced.
 */
fun buildTransaction(): Transaction<GameItem?> {
    val tx = Transaction<GameItem?>(
        input = { gameItem: GameItem? ->
            if (gameItem != null) TransactionObj(gameItem.id, gameItem.amount) else null
        },
        output = { obj: TransactionObj? ->
            if (obj != null) GameItem(obj.id, obj.count) else null
        },
    )
    // Populate the stackable set from ItemManager so the engine knows which items stack.
    val mgr = Server.itemManager
    if (mgr != null) {
        tx.stackableLookup = buildStackableSet(mgr)
    }
    return tx
}

/**
 * Lazily-built stackable set, keyed by [ItemManager] identity so swapping the manager
 * (a fresh instance in tests, or a reload) invalidates the cache instead of leaking a stale
 * set from a previous instance.
 */
@Volatile
private var cachedStackableSet: Pair<net.dodian.uber.game.item.ItemManager, Set<Int>>? = null

private fun buildStackableSet(mgr: net.dodian.uber.game.item.ItemManager): Set<Int> {
    cachedStackableSet?.let { (cachedMgr, set) -> if (cachedMgr === mgr) return set }
    val set = mutableSetOf<Int>()
    // ItemManager stores definitions 0..N. Walk through and collect stackable IDs.
    for (id in 0 until 30_000) {  // RS2 item IDs go well below 30k
        try {
            if (mgr.isStackable(id)) set.add(id)
        } catch (_: Exception) {
            break   // Reached end of defined items
        }
    }
    return set.also { cachedStackableSet = mgr to it }
}

// ---------------------------------------------------------------------------
// High-level convenience functions on Client
// ---------------------------------------------------------------------------

/**
 * Runs [block] inside a single dupe-safe atomic transaction against every [containers] entry.
 * Containers may belong to different [Client]s — this is what makes cross-player transfers
 * (trade settlement, duel stake payout/refund) safe: nothing is written to *any* container
 * until every query in [block] has succeeded. Returns `true` if the transaction committed.
 */
fun transaction(
    vararg containers: SelectInv,
    block: Transaction<GameItem?>.() -> Unit,
): Boolean {
    val tx = buildTransaction()
    containers.forEach { tx.register(it.transactionInv) }
    return try {
        tx.block()
        if (containers.any { !it.canCommit() }) return false
        containers.forEach { it.commit() }
        true
    } catch (_: TransactionCancellation) {
        false
    }
}

/**
 * Runs [block] inside a dupe-safe atomic transaction against the player's **inventory**.
 * On success all changes are committed and the inventory update packet is sent.
 * Returns `true` if the transaction succeeded without errors.
 *
 * ```kotlin
 * val ok = client.invTransaction { inv ->
 *     insert { into = inv.transactionInv; obj = itemId; strictCount = amount }
 * }
 * ```
 */
fun Client.invTransaction(
    block: Transaction<GameItem?>.(inv: SelectInv) -> Unit,
): Boolean {
    val inv = selectInv()
    return transaction(inv) { block(inv) }
}

/**
 * Runs [block] against **both** the player's inventory and bank in a single atomic transaction.
 * Both containers are only written to if the whole transaction succeeds.
 */
fun Client.invBankTransaction(
    block: Transaction<GameItem?>.(inv: SelectInv, bank: SelectInv) -> Unit,
): Boolean {
    val inv = selectInv()
    val bank = selectBank()
    return transaction(inv, bank) { block(inv, bank) }
}

// ---------------------------------------------------------------------------
// Simple named helpers — the primary API callers should use
// ---------------------------------------------------------------------------

/**
 * Atomically adds [amount] of [itemId] to the player's inventory.
 * Returns `true` on success.
 */
fun Client.invAdd(itemId: Int, amount: Int = 1): Boolean =
    invTransaction { inv ->
        insert {
            into = inv.transactionInv
            obj = itemId
            strictCount = amount
        }
    }

/**
 * Atomically removes [amount] of [itemId] from the player's inventory.
 * Returns `true` on success.
 */
fun Client.invDel(itemId: Int, amount: Int = 1): Boolean =
    invTransaction { inv ->
        delete {
            from = inv.transactionInv
            obj = itemId
            strictCount = amount
        }
    }

/**
 * Atomically removes [amount] of [itemId] from the player's inventory at the given [slot].
 * Returns `true` on success.
 */
fun Client.invDelAt(itemId: Int, slot: Int, amount: Int = 1): Boolean =
    invTransaction { inv ->
        delete {
            from = inv.transactionInv
            obj = itemId
            strictCount = amount
            strictSlot = slot
        }
    }

/**
 * Returns the total count of [itemId] in the player's inventory without modifying anything.
 */
fun Client.invCount(itemId: Int): Int {
    var total = 0L
    for (slot in playerItems.indices) {
        if (playerItems[slot] == itemId + 1) {
            total += playerItemsN[slot]
        }
    }
    return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/**
 * Returns `true` if the player has at least [amount] of [itemId] in their inventory.
 */
fun Client.invContains(itemId: Int, amount: Int = 1): Boolean = invCount(itemId) >= amount

/**
 * Atomically swaps item in [fromSlot] with item in [intoSlot] within the player's inventory.
 * Returns `true` on success.
 */
fun Client.invSwap(fromSlot: Int, intoSlot: Int): Boolean =
    invTransaction { inv ->
        swap {
            from = inv.transactionInv
            into = inv.transactionInv
            this.fromSlot = fromSlot
            this.intoSlot = intoSlot
            merge = true
        }
    }

/**
 * Atomically deposits an item from the player's inventory into their bank.
 * [fromSlot] is the inventory slot; [amount] is how many to deposit.
 * Returns `true` on success.
 */
fun Client.invDeposit(fromSlot: Int, amount: Int = 1): Boolean =
    invBankTransaction { inv, bank ->
        transfer {
            from = inv.transactionInv
            into = bank.transactionInv
            this.fromSlot = fromSlot
            this.count = amount
        }
    }

/**
 * Atomically withdraws an item from the player's bank into their inventory.
 * [bankSlot] is the bank slot; [amount] is how many to withdraw.
 * Returns `true` on success.
 */
fun Client.invWithdraw(bankSlot: Int, amount: Int = 1): Boolean =
    invBankTransaction { inv, bank ->
        transfer {
            from = bank.transactionInv
            into = inv.transactionInv
            this.fromSlot = bankSlot
            this.count = amount
        }
    }
