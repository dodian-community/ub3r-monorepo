package net.dodian.uber.game.content.events.partyroom

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.PartyItemsDisplay
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils

object Balloons {
    private val balloons = ArrayList<GameObject>()
    private val partyEventPos = ArrayList<Position>()
    private val partyItems = ArrayList<RewardItem>()
    private val droppedItems = ArrayList<RewardItem>()

    private var eventActive = false

    private const val DEFAULT_BALLOONS = 63
    private const val DEFAULT_INCREMENT = 9
    private const val MAX_LENGTH = 200

    private var totalBalloons = DEFAULT_BALLOONS
    private var balloonIncrement = DEFAULT_INCREMENT

    @JvmStatic
    fun triggerBalloonEvent(client: Client) {
        if (spawnBalloon(client.position.copy())) {
            Client.publicyell("${client.playerName} just spawned a balloon! Go and pop it!")
        }
    }

    private fun canAddPos(pos: Position): Boolean {
        if (pos.x in 3042..3049 && pos.y == 3378) {
            return false
        }
        if ((pos.x == 3051 && pos.y == 3375) ||
            (pos.x == 3040 && pos.y == 3375) ||
            (pos.x == 3040 && pos.y == 3381) ||
            (pos.x == 3051 && pos.y == 3381)
        ) {
            return false
        }
        if ((pos.x == 3045 && pos.y == 3384) || (pos.x == 3048 && pos.y == 3372)) {
            return false
        }
        return balloons.none { balloon -> balloon.x == pos.x && balloon.y == pos.y }
    }

    private fun inPartyRoom(pos: Position): Boolean =
        pos.x in 3036..3055 && pos.y in 3370..3385

    private fun setPartyPos() {
        for (x in 3039..3052) {
            for (y in 3372..3384) {
                val checkPos = Position(x, y, 0)
                if (canAddPos(checkPos)) {
                    partyEventPos.add(checkPos)
                }
            }
        }
    }

    private fun sendPartyTimer(message: String) {
        PlayerRegistry.snapshotActivePlayers().forEach { player ->
            if (inPartyRoom(player.position)) {
                player.sendMessage(message)
            }
        }
    }

    @JvmStatic
    fun spawnPartyEventBalloon() {
        val waveSize = if (totalBalloons > balloonIncrement) balloonIncrement else totalBalloons
        repeat(waveSize) {
            if (partyEventPos.isEmpty()) {
                totalBalloons = 0
                return
            }
            val random = Misc.random(partyEventPos.size - 1)
            val pos = partyEventPos.removeAt(random)
            spawnBalloon(pos)
        }
    }

    @JvmStatic
    fun triggerPartyEvent(client: Client) {
        if (eventActive) {
            client.sendMessage("Event is already active!")
            return
        }
        eventActive = true
        Client.publicyell("<col=664400>A drop party has been started in the Partyroom! Talk to Pete or teleport with Aubury!")
        partyEventPos.clear()
        setPartyPos()

        var timer = 9
        GameEventScheduler.runRepeatingMs(600) outer@{
            if (timer == 0) {
                sendPartyTimer("PARTYYYYYYYYYYYYYYYYYYYYYYYYYY! Get popping!")
                spawnPartyEventBalloon()
                totalBalloons -= balloonIncrement

                var waveTimer = 4
                GameEventScheduler.runRepeatingMs(600) inner@{
                    if (totalBalloons <= 0) {
                        eventActive = false
                        totalBalloons = DEFAULT_BALLOONS
                        balloonIncrement = DEFAULT_INCREMENT
                        Client.publicyell("<col=664400>The drop party in the Partyroom has just concluded!")
                        return@inner false
                    }
                    if (waveTimer == 0) {
                        spawnPartyEventBalloon()
                        totalBalloons -= balloonIncrement
                        waveTimer = 4
                    } else {
                        waveTimer--
                    }
                    true
                }
                return@outer false
            }

            sendPartyTimer("Partyroom drops commencing in: $timer")
            timer--
            true
        }
    }

    @JvmStatic
    fun spawnBalloon(pos: Position): Boolean {
        if (balloons.any { balloon -> balloon.x == pos.x && balloon.y == pos.y }) {
            return false
        }

        val id = 115 + Utils.random(7)
        val obj = GameObject(id, pos.x, pos.y, pos.z, 10, 0)
        balloons.add(obj)

        val shouldAttachLoot = Misc.random(99) < 75
        if (partyItems.isNotEmpty() && shouldAttachLoot) {
            val random = Misc.random(partyItems.size - 1)
            val reward = partyItems.removeAt(random)
            reward.setPosition(pos)
            droppedItems.add(reward)
        }

        PlayerRegistry.snapshotActivePlayers().forEach { person ->
            if (person.distanceToPoint(pos.x, pos.y) <= 104) {
                person.ReplaceObject2(pos, id, 0, 10)
                if (person.isPartyInterface) {
                    displayItems(person)
                }
            }
        }
        return true
    }

    @JvmStatic
    fun lootBalloon(client: Client, pos: Position): Boolean {
        val balloonIndex = balloons.indexOfFirst { balloon -> balloon.x == pos.x && balloon.y == pos.y }
        if (balloonIndex == -1) {
            return false
        }

        val balloon = balloons.removeAt(balloonIndex)
        client.performAnimation(794, 0)
        client.ReplaceObject2(Position(balloon.x, balloon.y, balloon.z), balloon.id + 8, 0, 10)

        GameEventScheduler.runLaterMs(600) {
            val droppedIndex = droppedItems.indexOfFirst { dropped -> dropped.getPosition() == pos }
            if (droppedIndex != -1) {
                val dropped = droppedItems.removeAt(droppedIndex)
                Ground.addFloorItem(client, dropped.getId(), dropped.getAmount())
                client.sendMessage("<col=664400>Something odd appears on the ground.")
            } else {
                client.sendMessage("<col=664400>The balloon bursts open and yields nothing.")
            }
            if (inPartyRoom(pos) && !partyEventPos.contains(pos)) {
                partyEventPos.add(pos)
            }
        }

        PlayerRegistry.snapshotActivePlayers().forEach { person ->
            if (person.distanceToPoint(balloon.x, balloon.y) <= 104 && person.position.z == balloon.z) {
                val balloonPos = Position(balloon.x, balloon.y, balloon.z)
                person.ReplaceObject2(balloonPos, balloon.id + 8, 0, 10)
                GameEventScheduler.runLaterMs(1200) {
                    person.ReplaceObject2(balloonPos, -1, 0, 10)
                }
            }
        }

        return true
    }

    @JvmStatic
    fun openInterface(client: Client) {
        displayItems(client)
        displayOfferItems(client)
        client.resetItems(5064)
        client.send(InventoryInterface(2156, 5063))
        client.isPartyInterface = true
    }

    @JvmStatic
    fun displayItems(client: Client) {
        client.send(PartyItemsDisplay(2273, partyItems))
    }

    @JvmStatic
    fun displayOfferItems(client: Client) {
        client.send(PartyItemsDisplay(2274, client.offeredPartyItems))
    }

    @JvmStatic
    fun offerItems(client: Client, id: Int, amount: Int, _slot: Int) {
        if (client.playerRights < 2) {
            return
        }

        var adjustedAmount = amount
        val stackable = Server.itemManager.isStackable(id)
        if (!stackable && adjustedAmount >= 8 - client.offeredPartyItems.size) {
            adjustedAmount = 8 - client.offeredPartyItems.size
        }

        if (!stackable) {
            adjustedAmount = adjustedAmount.coerceAtMost(client.getInvAmt(id))
            repeat(adjustedAmount) {
                client.deleteItem(id, 1)
                client.offeredPartyItems.add(RewardItem(id, 1))
            }
        } else {
            adjustedAmount = adjustedAmount.coerceAtMost(client.getInvAmt(id))
            client.deleteItem(id, adjustedAmount)
            var found = false
            client.offeredPartyItems.forEach { item ->
                if (item.getId() == id) {
                    found = true
                    item.setAmount(adjustedAmount)
                }
            }
            if (!found) {
                if (client.offeredPartyItems.size < 8) {
                    client.offeredPartyItems.add(RewardItem(id, adjustedAmount))
                } else {
                    adjustedAmount = 0
                }
            }
        }

        if (adjustedAmount > 0) {
            displayOfferItems(client)
        }
    }

    @JvmStatic
    fun removeOfferItems(client: Client, id: Int, amount: Int, slot: Int) {
        if (client.playerRights < 2) {
            return
        }

        var adjustedAmount = amount
        val stackable = Server.itemManager.isStackable(id)

        if (!stackable) {
            val checkAmount = client.offeredPartyItems.count { item -> item.getId() == id }
            adjustedAmount = adjustedAmount.coerceAtMost(checkAmount)
            repeat(adjustedAmount) {
                client.addItem(id, 1)
                val removeIndex = client.offeredPartyItems.indexOfFirst { item -> item.getId() == id }
                if (removeIndex != -1) {
                    client.offeredPartyItems.removeAt(removeIndex)
                }
            }
        } else {
            val offeredItem = client.offeredPartyItems.getOrNull(slot) ?: return
            if (adjustedAmount >= offeredItem.getAmount()) {
                adjustedAmount = offeredItem.getAmount()
                client.offeredPartyItems.removeAt(slot)
            } else {
                offeredItem.setAmount(-adjustedAmount)
            }
            client.addItem(id, adjustedAmount)
        }

        if (adjustedAmount > 0) {
            displayOfferItems(client)
            client.checkItemUpdate()
        }
    }

    @JvmStatic
    fun acceptItems(client: Client) {
        if (partyItems.size >= MAX_LENGTH) {
            client.sendMessage("You cant put in more items!")
            return
        }

        var index = client.offeredPartyItems.size - 1
        while (client.offeredPartyItems.isNotEmpty() && partyItems.size < MAX_LENGTH && index >= 0) {
            partyItems.add(client.offeredPartyItems[index])
            client.offeredPartyItems.removeAt(index)
            index--
        }

        displayItems(client)
        displayOfferItems(client)
        client.checkItemUpdate()
    }

    @JvmStatic
    fun eventActive(): Boolean = eventActive

    @JvmStatic
    fun spawnedBalloons(): Boolean = totalBalloons < DEFAULT_BALLOONS

    @JvmStatic
    fun updateBalloons(client: Client) {
        balloons.forEach { balloon ->
            if (client.distanceToPoint(balloon.x, balloon.y) <= 104 && client.position.z == balloon.z) {
                client.ReplaceObject2(Position(balloon.x, balloon.y, balloon.z), balloon.id, 0, 10)
            }
        }
    }
}
