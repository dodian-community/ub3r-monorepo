package net.dodian.uber.game.api.content

import net.dodian.uber.game.model.Position

/**
 * Protocol-free player surface for gameplay/content modules. Implementations
 * are provided by the engine; content must never down-cast it to a network
 * session or access entity arrays directly.
 */
interface ContentPlayer {
    val inventory: ContentInventory
    val equipment: ContentEquipment
    val economy: ContentEconomy
    val actions: ContentActionControl
    val ui: ContentUi
    val world: ContentWorld
    val social: ContentSocial
    val features: ContentFeatures
    val attributes: ContentAttributes
    val variables: ContentVariables
    val dialogue: ContentDialogue
}

interface ContentInventory {
    fun contains(itemId: Int, amount: Int = 1): Boolean
    fun amount(itemId: Int): Int
    fun freeSlots(): Int
    fun add(itemId: Int, amount: Int = 1): Boolean
    fun remove(itemId: Int, amount: Int = 1): Boolean
    /** Applies all mutations together, or applies none of them. */
    fun transaction(block: ContentInventoryTransaction.() -> Unit): Boolean
    fun refresh()
}

interface ContentInventoryTransaction {
    fun require(itemId: Int, amount: Int = 1): Boolean
    fun remove(itemId: Int, amount: Int = 1): Boolean
    fun add(itemId: Int, amount: Int = 1): Boolean
}

interface ContentEquipment {
    fun item(slot: Int): Int
    fun amount(slot: Int): Int
    fun remove(slot: Int, itemId: Int, amount: Int = 1): Boolean
    fun refresh()
}

interface ContentEconomy {
    fun bankAmount(itemId: Int): Int
    fun openBank()
    fun openShop(shopId: Int)
}

interface ContentActionControl {
    fun animate(id: Int, delay: Int = 0)
    fun stop()
    fun lockMovement(locked: Boolean)
    fun beginSession(key: String): Boolean
    fun endSession(key: String)
    fun activeSessionKey(): String?
}

interface ContentUi {
    fun message(text: String)
    fun string(text: String, componentId: Int)
    fun open(interfaceId: Int)
    fun close()
    fun npcDialogue(dialogueId: Int, npcId: Int)
}

interface ContentWorld {
    val position: Position
    fun distanceTo(x: Int, y: Int): Int
    fun teleport(destination: Position)
    fun graphic(id: Int, height: Int = 0)
    /**
     * Swaps the object at [position] (currently [originalId]) for [replacementId] world-wide
     * (every nearby viewer sees it, not just this player), reverting back to [originalId] after
     * [restoreTicks] ticks (0 = permanent until something else replaces it).
     */
    fun replaceObject(position: Position, originalId: Int, replacementId: Int, restoreTicks: Int = 0)
}

fun interface ContentSocial {
    fun hasFriend(encodedName: Long): Boolean
}

interface ContentFeatures {
    val bankingEnabled: Boolean
    val shoppingEnabled: Boolean
    val tradingEnabled: Boolean
    val duelingEnabled: Boolean
}
