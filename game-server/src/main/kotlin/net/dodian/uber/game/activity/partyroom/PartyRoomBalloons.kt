package net.dodian.uber.game.activity.partyroom

import net.dodian.uber.game.Server
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.PartyItemsDisplay
import net.dodian.utilities.Utils

object PartyRoomBalloons {
    private val activeBalloons = ArrayList<WorldObject>()
    private val partyEventPositions = ArrayList<Position>()
    private val depositedItems = ArrayList<PartyRoomRewardItem>()
    private val balloonLootItems = ArrayList<PartyRoomRewardItem>()

    private var partyEventActive = false

    private const val DEFAULT_BALLOON_COUNT = 63
    private const val DEFAULT_BALLOONS_PER_WAVE = 9
    private const val MAX_DEPOSITED_ITEMS = 200

    private var remainingPartyBalloons = DEFAULT_BALLOON_COUNT
    private var balloonsPerWave = DEFAULT_BALLOONS_PER_WAVE

    @JvmStatic
    fun triggerSingleBalloonEvent(client: Client) {
        if (spawnBalloon(client.position.copy())) {
            Client.publicyell("${client.playerName} just spawned a balloon! Go and pop it!")
        }
    }

    private fun canAddPartyEventPosition(pos: Position): Boolean {
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
        return activeBalloons.none { balloon -> balloon.x == pos.x && balloon.y == pos.y }
    }

    private fun isInPartyRoom(pos: Position): Boolean =
        pos.x in 3036..3055 && pos.y in 3370..3385

    private fun rebuildPartyEventPositions() {
        for (x in 3039..3052) {
            for (y in 3372..3384) {
                val checkPos = Position(x, y, 0)
                if (canAddPartyEventPosition(checkPos)) {
                    partyEventPositions.add(checkPos)
                }
            }
        }
    }

    private fun sendPartyTimer(message: String) {
        PlayerRegistry.snapshotActivePlayers().forEach { player ->
            if (isInPartyRoom(player.position)) {
                player.sendMessage(message)
            }
        }
    }

    @JvmStatic
    fun spawnPartyEventBalloonWave() {
        val waveSize = if (remainingPartyBalloons > balloonsPerWave) balloonsPerWave else remainingPartyBalloons
        repeat(waveSize) {
            if (partyEventPositions.isEmpty()) {
                remainingPartyBalloons = 0
                return
            }
            val random = Misc.random(partyEventPositions.size - 1)
            val pos = partyEventPositions.removeAt(random)
            spawnBalloon(pos)
        }
    }

    @JvmStatic
    fun triggerPartyEvent(client: Client) {
        if (partyEventActive) {
            client.sendMessage("Event is already active!")
            return
        }
        partyEventActive = true
        Client.publicyell("<col=664400>A drop party has been started in the Partyroom! Talk to Pete or teleport with Aubury!")
        partyEventPositions.clear()
        rebuildPartyEventPositions()

        var timer = 9
        ContentTiming.runRepeatingMs(600) outer@{
            if (timer == 0) {
                sendPartyTimer("PARTYYYYYYYYYYYYYYYYYYYYYYYYYY! Get popping!")
                spawnPartyEventBalloonWave()
                remainingPartyBalloons -= balloonsPerWave

                var waveTimer = 4
                ContentTiming.runRepeatingMs(600) inner@{
                    if (remainingPartyBalloons <= 0) {
                        partyEventActive = false
                        remainingPartyBalloons = DEFAULT_BALLOON_COUNT
                        balloonsPerWave = DEFAULT_BALLOONS_PER_WAVE
                        Client.publicyell("<col=664400>The drop party in the Partyroom has just concluded!")
                        return@inner false
                    }
                    if (waveTimer == 0) {
                        spawnPartyEventBalloonWave()
                        remainingPartyBalloons -= balloonsPerWave
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
        if (activeBalloons.any { balloon -> balloon.x == pos.x && balloon.y == pos.y }) {
            return false
        }

        val id = 115 + Utils.random(7)
        val obj = WorldObject(id, pos.x, pos.y, pos.z, 10, 0)
        activeBalloons.add(obj)

        val shouldAttachLoot = Misc.random(99) < 75
        if (depositedItems.isNotEmpty() && shouldAttachLoot) {
            val random = Misc.random(depositedItems.size - 1)
            val reward = depositedItems.removeAt(random)
            reward.setPosition(pos)
            balloonLootItems.add(reward)
        }

        PlayerRegistry.snapshotActivePlayers().forEach { person ->
            if (person.distanceToPoint(pos.x, pos.y) <= 104) {
                person.ReplaceObject2(pos, id, 0, 10)
                if (person.isPartyInterface) {
                    displayDepositedItems(person)
                }
            }
        }
        return true
    }

    @JvmStatic
    fun lootBalloon(client: Client, pos: Position): Boolean {
        val balloonIndex = activeBalloons.indexOfFirst { balloon -> balloon.x == pos.x && balloon.y == pos.y }
        if (balloonIndex == -1) {
            return false
        }

        val balloon = activeBalloons.removeAt(balloonIndex)
        client.performAnimation(794, 0)
        client.ReplaceObject2(Position(balloon.x, balloon.y, balloon.z), balloon.id + 8, 0, 10)

        ContentTiming.runLaterMs(600) {
            val droppedIndex = balloonLootItems.indexOfFirst { dropped -> dropped.getPosition() == pos }
            if (droppedIndex != -1) {
                val dropped = balloonLootItems.removeAt(droppedIndex)
                Ground.addFloorItem(client, dropped.getId(), dropped.getAmount())
                client.sendMessage("<col=664400>Something odd appears on the ground.")
            } else {
                client.sendMessage("<col=664400>The balloon bursts open and yields nothing.")
            }
            if (isInPartyRoom(pos) && !partyEventPositions.contains(pos)) {
                partyEventPositions.add(pos)
            }
        }

        PlayerRegistry.snapshotActivePlayers().forEach { person ->
            if (person.distanceToPoint(balloon.x, balloon.y) <= 104 && person.position.z == balloon.z) {
                val balloonPos = Position(balloon.x, balloon.y, balloon.z)
                person.ReplaceObject2(balloonPos, balloon.id + 8, 0, 10)
                ContentTiming.runLaterMs(1200) {
                    person.ReplaceObject2(balloonPos, -1, 0, 10)
                }
            }
        }

        return true
    }

    @JvmStatic
    fun openPartyRoomInterface(client: Client) {
        displayDepositedItems(client)
        displayOfferedItems(client)
        client.resetItems(5064)
        client.send(InventoryInterface(2156, 5063))
        client.isPartyInterface = true
    }

    @JvmStatic
    fun displayDepositedItems(client: Client) {
        client.send(PartyItemsDisplay(2273, depositedItems))
    }

    @JvmStatic
    fun displayOfferedItems(client: Client) {
        client.send(PartyItemsDisplay(2274, client.offeredPartyItems))
    }

    @JvmStatic
    fun offerPartyItems(client: Client, id: Int, amount: Int, ignoredSlot: Int) {
        if (ignoredSlot < 0) {
            return
        }
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
                client.offeredPartyItems.add(PartyRoomRewardItem(id, 1))
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
                    client.offeredPartyItems.add(PartyRoomRewardItem(id, adjustedAmount))
                } else {
                    adjustedAmount = 0
                }
            }
        }

        if (adjustedAmount > 0) {
            displayOfferedItems(client)
        }
    }

    @JvmStatic
    fun removeOfferedPartyItems(client: Client, id: Int, amount: Int, slot: Int) {
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
            displayOfferedItems(client)
            client.checkItemUpdate()
        }
    }

    @JvmStatic
    fun acceptOfferedPartyItems(client: Client) {
        if (depositedItems.size >= MAX_DEPOSITED_ITEMS) {
            client.sendMessage("You cant put in more items!")
            return
        }

        var index = client.offeredPartyItems.size - 1
        while (client.offeredPartyItems.isNotEmpty() && depositedItems.size < MAX_DEPOSITED_ITEMS && index >= 0) {
            depositedItems.add(client.offeredPartyItems[index])
            client.offeredPartyItems.removeAt(index)
            index--
        }

        displayDepositedItems(client)
        displayOfferedItems(client)
        client.checkItemUpdate()
    }

    @JvmStatic
    fun isPartyEventActive(): Boolean = partyEventActive

    @JvmStatic
    fun hasSpawnedPartyBalloons(): Boolean = remainingPartyBalloons < DEFAULT_BALLOON_COUNT

    @JvmStatic
    fun updateVisibleBalloons(client: Client) {
        activeBalloons.forEach { balloon ->
            if (client.distanceToPoint(balloon.x, balloon.y) <= 104 && client.position.z == balloon.z) {
                client.ReplaceObject2(Position(balloon.x, balloon.y, balloon.z), balloon.id, 0, 10)
            }
        }
    }
}

