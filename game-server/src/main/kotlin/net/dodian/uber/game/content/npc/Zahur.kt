package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.party.RewardItem
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.skills.herblore.HerbloreDefinitions

internal object Zahur {
    // Stats: 4753: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 4753, x = 3424, y = 2908, z = 0, face = 0),
    )

    val npcIds: IntArray = npcIdsFromEntries(entries)

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Hello ${if (client.gender == 1) "miss" else "mr"} adventurer.", "What can I help you with today?")
            options(
                title = "Select an option",
                DialogueOption("Visit the store") {
                    action { c -> c.openUpShopRouted(22) }
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
        client.openUpShopRouted(39)
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
        for (definition in HerbloreDefinitions.herbDefinitions) {
            val notedGrimy = client.GetNotedItem(definition.grimyId)
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
        for (definition in HerbloreDefinitions.herbDefinitions) {
            val notedHerb = client.GetNotedItem(definition.cleanId)
            if (client.playerHasItem(notedHerb)) {
                client.herbOptions.add(RewardItem(client.GetNotedItem(definition.unfinishedPotionId), 0))
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
        val potionAmount = LongArray(HerbloreDefinitions.potionDoseDefinitions.size)
        val vialAmount = LongArray(HerbloreDefinitions.potionDoseDefinitions.size)

        fun collect(source: List<Int>, sourceDose: Int) {
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

        collect(HerbloreDefinitions.potionDoseDefinitions.map { it.fourDoseId }, 4)
        collect(HerbloreDefinitions.potionDoseDefinitions.map { it.threeDoseId }, 3)
        collect(HerbloreDefinitions.potionDoseDefinitions.map { it.twoDoseId }, 2)
        collect(HerbloreDefinitions.potionDoseDefinitions.map { it.oneDoseId }, 1)

        for (index in potionAmount.indices) {
            val definition = HerbloreDefinitions.potionDoseDefinitions[index]
            val targetPotion = when (dose) {
                4 -> definition.fourDoseId
                3 -> definition.threeDoseId
                2 -> definition.twoDoseId
                else -> definition.oneDoseId
            }
            val notedTargetPotion = client.GetNotedItem(targetPotion)
            if (notedTargetPotion <= 0) {
                continue
            }

            val producedAmount = (potionAmount[index] / dose).toInt()
            val leftOverDose = (potionAmount[index] % dose).toInt()
            val emptyVials = (vialAmount[index] - producedAmount - if (leftOverDose > 0) 1 else 0).toInt()
            val currentEmptyVials = client.getInvAmt(230)
            val leftOverPotion = when (leftOverDose) {
                3 -> definition.threeDoseId
                2 -> definition.twoDoseId
                1 -> definition.oneDoseId
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

            if (leftOverPotion > 0 && !client.addItem(leftOverPotion, 1)) {
                Ground.addFloorItem(client, leftOverPotion, 1)
                ItemLog.playerDrop(
                    client,
                    leftOverPotion,
                    1,
                    client.position.copy(),
                    "Decant dropped ${client.GetItemName(leftOverPotion).lowercase()}",
                )
                client.send(SendMessage("<col=FF0000>You dropped the ${client.GetItemName(leftOverPotion).lowercase()} to the floor!"))
            }
        }

        client.checkItemUpdate()
        client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("Enjoy your decanted potions ${client.playerName}"))
    }
}
