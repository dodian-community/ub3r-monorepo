package net.dodian.uber.game.npc

import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem
import net.dodian.uber.game.skill.herblore.HerbloreData
import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.audit.ItemLog

internal object HerbloreNpcDialogue {
    private data class NpcReply(
        val emote: DialogueEmote = DialogueEmote.DEFAULT,
        val lines: Array<String>,
    )
    private val grimyHerbs: IntArray = HerbloreData.herbDefinitions.map { it.grimyId }.toIntArray()
    private val cleanHerbs: IntArray = HerbloreData.herbDefinitions.map { it.cleanId }.toIntArray()
    private val unfinishedPotions: IntArray = HerbloreData.herbDefinitions.map { it.unfinishedPotionId }.toIntArray()
    private val pot4Dose: IntArray = HerbloreData.potionDoseDefinitions.map { it.fourDoseId }.toIntArray()
    private val pot3Dose: IntArray = HerbloreData.potionDoseDefinitions.map { it.threeDoseId }.toIntArray()
    private val pot2Dose: IntArray = HerbloreData.potionDoseDefinitions.map { it.twoDoseId }.toIntArray()
    private val pot1Dose: IntArray = HerbloreData.potionDoseDefinitions.map { it.oneDoseId }.toIntArray()

    @JvmStatic
    fun openDecantDoseOptions(client: Client, npcId: Int) {
        DialogueService.start(client) {
            options(
                title = "What should we decant it into?",
                DialogueOption("Four dose") {
                    val reply = decantPotions(client, 4)
                    npcChat(npcId, reply.emote, *reply.lines)
                    finish()
                },
                DialogueOption("Three dose") {
                    val reply = decantPotions(client, 3)
                    npcChat(npcId, reply.emote, *reply.lines)
                    finish()
                },
                DialogueOption("Two dose") {
                    val reply = decantPotions(client, 2)
                    npcChat(npcId, reply.emote, *reply.lines)
                    finish()
                },
                DialogueOption("One dose") {
                    val reply = decantPotions(client, 1)
                    npcChat(npcId, reply.emote, *reply.lines)
                    finish()
                },
                DialogueOption("Nevermind") {
                    playerChat(DialogueEmote.ANGRY1, "Nevermind, I do not need anything.")
                    finish()
                },
            )
        }
    }

    @JvmStatic
    fun openHerbCleaner(client: Client, npcId: Int) {
        client.herbMaking = 0
        client.herbOptions.clear()
        for (index in grimyHerbs.indices) {
            val notedGrimy = client.getNotedItem(grimyHerbs[index])
            if (client.playerHasItem(notedGrimy)) {
                client.herbOptions.add(PartyRoomRewardItem(notedGrimy, 0))
            }
        }
        if (client.herbOptions.isEmpty()) {
            DialogueService.start(client) {
                npcChat(npcId, DialogueEmote.LAUGH1, "You got no herbs for me to clean!")
                finish()
            }
            return
        }
        client.setHerbOptions()
    }

    @JvmStatic
    fun openUnfinishedPotionMaker(client: Client, npcId: Int) {
        client.herbMaking = 0
        client.herbOptions.clear()
        for (index in cleanHerbs.indices) {
            val notedHerb = client.getNotedItem(cleanHerbs[index])
            if (client.playerHasItem(notedHerb)) {
                client.herbOptions.add(PartyRoomRewardItem(client.getNotedItem(unfinishedPotions[index]), 0))
            }
        }
        if (client.herbOptions.isEmpty()) {
            DialogueService.start(client) {
                npcChat(npcId, DialogueEmote.LAUGH1, "You got no herbs for me to make into unfinish potions!")
                finish()
            }
            return
        }
        client.setHerbOptions()
    }

    @JvmStatic
    fun showBatchResultAndContinue(client: Client, npcId: Int, firstLine: String, secondLine: String) {
        if (client.herbOptions.isEmpty()) {
            DialogueService.start(client) {
                npcChat(npcId, DialogueEmote.ALMOST_CRYING, firstLine, secondLine)
                finish()
            }
            return
        }
        DialogueService.start(client) {
            npcChat(npcId, DialogueEmote.ALMOST_CRYING, firstLine, secondLine)
            finishThen(closeInterfaces = false) { c ->
                c.setHerbOptions()
            }
        }
    }

    private fun decantPotions(client: Client, dose: Int): NpcReply {
        val potionAmount = LongArray(pot4Dose.size)
        val vialAmount = LongArray(pot4Dose.size)

        fun collect(source: IntArray, sourceDose: Int) {
            for (index in source.indices) {
                val notedPotion = client.getNotedItem(source[index])
                if (notedPotion > 0) {
                    val inventoryAmount = client.getInvAmt(notedPotion)
                    potionAmount[index] += inventoryAmount.toLong() * sourceDose
                    vialAmount[index] += inventoryAmount.toLong()
                    client.deleteItem(notedPotion, inventoryAmount)
                }
            }
        }

        collect(pot4Dose, 4)
        collect(pot3Dose, 3)
        collect(pot2Dose, 2)
        collect(pot1Dose, 1)

        for (index in potionAmount.indices) {
            val targetPotion = when (dose) {
                4 -> pot4Dose[index]
                3 -> pot3Dose[index]
                2 -> pot2Dose[index]
                else -> pot1Dose[index]
            }
            val notedTargetPotion = client.getNotedItem(targetPotion)
            if (notedTargetPotion <= 0) {
                continue
            }

            val producedAmount = (potionAmount[index] / dose).toInt()
            var leftOverDose = (potionAmount[index] % dose).toInt()
            val emptyVials = (vialAmount[index] - producedAmount - if (leftOverDose > 0) 1 else 0).toInt()
            val currentEmptyVials = client.getInvAmt(230)
            val leftOverPotion = when (leftOverDose) {
                3 -> pot3Dose[index]
                2 -> pot2Dose[index]
                1 -> pot1Dose[index]
                else -> -1
            }

            if (producedAmount > 0 && !client.addItem(notedTargetPotion, producedAmount)) {
                Ground.addFloorItem(client, notedTargetPotion, producedAmount)
                ItemLog.playerDrop(
                    client,
                    notedTargetPotion,
                    producedAmount,
                    client.position.copy(),
                    "Decant dropped ${client.getItemName(targetPotion).lowercase()}",
                )
                client.sendMessage("<col=FF0000>You dropped the ${client.getItemName(targetPotion).lowercase()} to the floor!")
            }

            if (emptyVials > 0 && (emptyVials + currentEmptyVials) < 1000 && !client.addItem(230, emptyVials)) {
                Ground.addFloorItem(client, 230, emptyVials)
                ItemLog.playerDrop(client, 230, emptyVials, client.position.copy(), "Decant dropped ${client.getItemName(230).lowercase()}")
                client.sendMessage("<col=FF0000>You dropped the ${client.getItemName(230).lowercase()} to the floor!")
            } else if (emptyVials < 0) {
                client.deleteItem(230, emptyVials * -1)
            }

            leftOverDose = leftOverPotion
            if (leftOverDose > 0 && !client.addItem(leftOverDose, 1)) {
                Ground.addFloorItem(client, leftOverDose, 1)
                ItemLog.playerDrop(
                    client,
                    leftOverDose,
                    1,
                    client.position.copy(),
                    "Decant dropped ${client.getItemName(leftOverDose).lowercase()}",
                )
                client.sendMessage("<col=FF0000>You dropped the ${client.getItemName(leftOverDose).lowercase()} to the floor!")
            }
        }

        client.checkItemUpdate()
        return NpcReply(lines = arrayOf("Enjoy your decanted potions ${client.playerName}"))
    }
}
