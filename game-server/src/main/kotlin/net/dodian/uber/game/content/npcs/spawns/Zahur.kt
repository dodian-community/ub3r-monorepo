package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.party.RewardItem
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.security.ItemLog
import net.dodian.utilities.Utils

internal object Zahur {
    // Stats: 4753: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 4753, x = 3424, y = 2908, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Hello ${if (client.gender == 1) "miss" else "mr"} adventurer.", "What can I help you with today?")
            options(
                title = "Select an option",
                DialogueOption("Visit the store") {
                    action { c -> c.WanneShop = 22 }
                    finish()
                },
                DialogueOption("Clean herbs") {
                    action { c -> HerbloreNpcDialogue.openHerbCleaner(c, npc.id) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("Make unfinish potions") {
                    action { c -> HerbloreNpcDialogue.openUnfinishedPotionMaker(c, npc.id) }
                    finish(closeInterfaces = false)
                },
            )
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 39
        return true
    }

    fun onThirdClick(client: Client, npc: Npc): Boolean {
        HerbloreNpcDialogue.openHerbCleaner(client, npc.id)
        return true
    }

    fun onFourthClick(client: Client, npc: Npc): Boolean {
        HerbloreNpcDialogue.openUnfinishedPotionMaker(client, npc.id)
        return true
    }
}

internal object HerbloreNpcDialogue {

    @JvmStatic
    fun openDecantDoseOptions(client: Client, npcId: Int) {
        DialogueService.start(client) {
            options(
                title = "What should we decant it into?",
                DialogueOption("Four dose") {
                    action { c -> decantPotions(c, npcId, 4) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("Three dose") {
                    action { c -> decantPotions(c, npcId, 3) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("Two dose") {
                    action { c -> decantPotions(c, npcId, 2) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("One dose") {
                    action { c -> decantPotions(c, npcId, 1) }
                    finish(closeInterfaces = false)
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
        for (index in Utils.grimy_herbs.indices) {
            val notedGrimy = client.GetNotedItem(Utils.grimy_herbs[index])
            if (client.playerHasItem(notedGrimy)) {
                client.herbOptions.add(RewardItem(notedGrimy, 0))
            }
        }
        if (client.herbOptions.isEmpty()) {
            client.showNPCChat(npcId, DialogueEmote.LAUGH1.id, arrayOf("You got no herbs for me to clean!"))
            return
        }
        client.setHerbOptions()
    }

    @JvmStatic
    fun openUnfinishedPotionMaker(client: Client, npcId: Int) {
        client.herbMaking = 0
        client.herbOptions.clear()
        for (index in Utils.herbs.indices) {
            val notedHerb = client.GetNotedItem(Utils.herbs[index])
            if (client.playerHasItem(notedHerb)) {
                client.herbOptions.add(RewardItem(client.GetNotedItem(Utils.herb_unf[index]), 0))
            }
        }
        if (client.herbOptions.isEmpty()) {
            client.showNPCChat(npcId, DialogueEmote.LAUGH1.id, arrayOf("You got no herbs for me to make into unfinish potions!"))
            return
        }
        client.setHerbOptions()
    }

    @JvmStatic
    fun showBatchResultAndContinue(client: Client, npcId: Int, firstLine: String, secondLine: String) {
        if (client.herbOptions.isEmpty()) {
            client.showNPCChat(npcId, DialogueEmote.ALMOST_CRYING.id, arrayOf(firstLine, secondLine))
            return
        }
        DialogueService.start(client) {
            npcChat(npcId, DialogueEmote.ALMOST_CRYING, firstLine, secondLine)
            action { c -> c.setHerbOptions() }
            finish(closeInterfaces = false)
        }
    }

    private fun decantPotions(client: Client, npcId: Int, dose: Int) {
        val potionAmount = LongArray(Utils.pot_4_dose.size)
        val vialAmount = LongArray(Utils.pot_4_dose.size)

        fun collect(source: IntArray, sourceDose: Int) {
            for (index in source.indices) {
                val notedPotion = client.GetNotedItem(source[index])
                if (notedPotion > 0) {
                    val inventoryAmount = client.getInvAmt(notedPotion)
                    potionAmount[index] += inventoryAmount.toLong() * sourceDose
                    vialAmount[index] += inventoryAmount.toLong()
                    client.deleteItem(notedPotion, inventoryAmount)
                }
            }
        }

        collect(Utils.pot_4_dose, 4)
        collect(Utils.pot_3_dose, 3)
        collect(Utils.pot_2_dose, 2)
        collect(Utils.pot_1_dose, 1)

        for (index in potionAmount.indices) {
            val targetPotion = when (dose) {
                4 -> Utils.pot_4_dose[index]
                3 -> Utils.pot_3_dose[index]
                2 -> Utils.pot_2_dose[index]
                else -> Utils.pot_1_dose[index]
            }
            val notedTargetPotion = client.GetNotedItem(targetPotion)
            if (notedTargetPotion <= 0) {
                continue
            }

            val producedAmount = (potionAmount[index] / dose).toInt()
            var leftOverDose = (potionAmount[index] % dose).toInt()
            val emptyVials = (vialAmount[index] - producedAmount - if (leftOverDose > 0) 1 else 0).toInt()
            val currentEmptyVials = client.getInvAmt(230)
            val leftOverPotion = when (leftOverDose) {
                3 -> Utils.pot_3_dose[index]
                2 -> Utils.pot_2_dose[index]
                1 -> Utils.pot_1_dose[index]
                else -> -1
            }

            if (producedAmount > 0 && !client.addItem(notedTargetPotion, producedAmount)) {
                Ground.addFloorItem(client, notedTargetPotion, producedAmount)
                ItemLog.playerDrop(
                    client,
                    notedTargetPotion,
                    producedAmount,
                    client.position.copy(),
                    "Decant dropped ${client.GetItemName(targetPotion).lowercase()}",
                )
                client.send(SendMessage("<col=FF0000>You dropped the ${client.GetItemName(targetPotion).lowercase()} to the floor!"))
            }

            if (emptyVials > 0 && (emptyVials + currentEmptyVials) < 1000 && !client.addItem(230, emptyVials)) {
                Ground.addFloorItem(client, 230, emptyVials)
                ItemLog.playerDrop(client, 230, emptyVials, client.position.copy(), "Decant dropped ${client.GetItemName(230).lowercase()}")
                client.send(SendMessage("<col=FF0000>You dropped the ${client.GetItemName(230).lowercase()} to the floor!"))
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
                    "Decant dropped ${client.GetItemName(leftOverDose).lowercase()}",
                )
                client.send(SendMessage("<col=FF0000>You dropped the ${client.GetItemName(leftOverDose).lowercase()} to the floor!"))
            }
        }

        client.checkItemUpdate()
        client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("Enjoy your decanted potions ${client.playerName}"))
    }
}
