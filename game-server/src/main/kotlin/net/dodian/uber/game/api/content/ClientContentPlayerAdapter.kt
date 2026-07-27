package net.dodian.uber.game.api.content

import net.dodian.uber.game.engine.systems.skills.ClientSkillPlayerAdapter
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

/**
 * Engine-only implementation of the protocol-free [ContentPlayer] contract.
 * Content receives the contract; only this adapter is allowed to touch Client.
 */
internal class ClientContentPlayerAdapter(private val client: Client) : ContentPlayer {
    private val skills = ClientSkillPlayerAdapter(client)

    override val inventory = object : ContentInventory {
        override fun contains(itemId: Int, amount: Int) = skills.inventory.contains(itemId, amount)
        override fun amount(itemId: Int) = skills.inventory.amount(itemId)
        override fun freeSlots() = skills.inventory.freeSlots()
        override fun add(itemId: Int, amount: Int) = skills.inventory.add(itemId, amount)
        override fun remove(itemId: Int, amount: Int) = skills.inventory.remove(itemId, amount)
        override fun transaction(block: ContentInventoryTransaction.() -> Unit): Boolean = skills.inventory.transaction {
                val staged = this
                block(object : ContentInventoryTransaction {
                    override fun require(itemId: Int, amount: Int) = staged.require(itemId, amount)
                    override fun remove(itemId: Int, amount: Int) = staged.remove(itemId, amount)
                    override fun add(itemId: Int, amount: Int) = staged.add(itemId, amount)
                })
            }
        override fun refresh() = skills.inventory.refresh()
    }

    override val equipment = object : ContentEquipment {
        override fun item(slot: Int) = skills.equipment.item(slot)
        override fun amount(slot: Int) = skills.equipment.amount(slot)
        override fun remove(slot: Int, itemId: Int, amount: Int) = skills.equipment.remove(slot, itemId, amount)
        override fun refresh() = skills.equipment.refresh()
    }
    override val economy: ContentEconomy get() = skills.economy
    override val social: ContentSocial get() = skills.social
    override val features: ContentFeatures get() = skills.features
    override val attributes: ContentAttributes get() = skills.attributes
    override val variables = object : ContentVariables { override fun varbit(id: Int, value: Int) = client.varbit(id, value) }
    override val dialogue = object : ContentDialogue {
        override fun message(text: String) = client.sendMessage(text)
        override fun npc(dialogueId: Int, npcId: Int) = client.startNpcDialogue(dialogueId, npcId)
        override fun close() = client.send(RemoveInterfaces())
    }
    override val actions = object : ContentActionControl {
        override fun animate(id: Int, delay: Int) = skills.actions.animate(id, delay)
        override fun stop() = skills.actions.stop()
        override fun lockMovement(locked: Boolean) = skills.actions.lockMovement(locked)
        override fun beginSession(key: String) = skills.actions.beginSession(key)
        override fun endSession(key: String) = skills.actions.endSession(key)
        override fun activeSessionKey() = skills.actions.activeSessionKey()
    }
    override val ui = object : ContentUi {
        override fun message(text: String) = client.sendMessage(text)
        override fun string(text: String, componentId: Int) = client.sendString(text, componentId)
        override fun open(interfaceId: Int) = client.openInterface(interfaceId)
        override fun close() = client.send(RemoveInterfaces())
        override fun npcDialogue(dialogueId: Int, npcId: Int) = client.startNpcDialogue(dialogueId, npcId)
    }
    override val world = object : ContentWorld {
        override val position: Position get() = client.position
        override fun distanceTo(x: Int, y: Int) = client.distanceToPoint(x, y)
        override fun teleport(destination: Position) = client.transport(destination)
        override fun graphic(id: Int, height: Int) = client.stillgfx(id, client.position, height)
        override fun replaceObject(position: Position, originalId: Int, replacementId: Int, restoreTicks: Int) {
            val worldObject = WorldObject(replacementId, position.x, position.y, position.z, 10, 0, originalId)
            GlobalObject.addGlobalObject(worldObject, restoreTicks * 600)
        }
    }
}

/** Engine boundary; content modules must accept [ContentPlayer], never [Client]. */
internal fun Client.asContentPlayer(): ContentPlayer = ClientContentPlayerAdapter(this)
