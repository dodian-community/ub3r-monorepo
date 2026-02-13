package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.dialogue.NpcDialogueService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.utilities.Misc

internal object Duradel {
    // Stats: 405: r=60 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 405, x = 2606, y = 3398, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Need help with your slayer assignment?")
            options(
                title = "Select an option",
                DialogueOption("Yes please.") {
                    action { c ->
                        c.NpcDialogue = 11
                        c.NpcDialogueSend = false
                        c.nextDiag = -1
                        NpcDialogueService.updateNpcChat(c)
                    }
                    finish(closeInterfaces = false)
                },
                DialogueOption("No thanks.") { finish() },
            )
        }
        return true
    }
}

object SlayerDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Slayer.INTRO) { c ->
            c.sendFrame200(4883, c.npcFace)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Hi there noob, what do you want?", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            c.nextDiag = DialogueIds.Slayer.OPTIONS
            true
        }

        builder.handle(DialogueIds.Slayer.OPTIONS) { c ->
            if (c.NpcTalkTo == 405 && (c.determineCombatLevel() < 50 || c.getLevel(Skill.SLAYER) < 50)) {
                c.sendFrame200(4888, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
                c.send(SendString("You need 50 combat and slayer", 4890))
                c.send(SendString("to be assign tasks from me!", 4891))
                c.send(SendString("Click here to continue", 4892))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
                c.sendFrame164(4887)
            } else if (c.NpcTalkTo == 403 && (!c.checkItem(989) || c.getLevel(Skill.SLAYER) < 50)) {
                c.sendFrame200(4888, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
                c.send(SendString("You need a crystal key and 50 slayer", 4890))
                c.send(SendString("to be assign tasks from me!", 4891))
                c.send(SendString("Click here to continue", 4892))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
                c.sendFrame164(4887)
            } else {
                val gotHelmet = c.playerHasItem(11864)
                val taskName = currentTaskName(c)
                val slayerMaster = arrayOf(
                    "What would you like to say?",
                    "I'd like a task please",
                    if (taskName.isNotEmpty()) "Cancel ${taskName.lowercase()} task" else "No task to skip",
                    "I'd like to upgrade my ${if (gotHelmet) "slayer helmet" else "black mask"}",
                    "Can you teleport me to west ardougne?"
                )
                DialogueUi.showPlayerOption(c, slayerMaster)
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.ASSIGN_TASK) { c ->
            if (c.NpcTalkTo == 405 && (c.determineCombatLevel() < 50 || c.getLevel(Skill.SLAYER) < 50)) {
                c.sendFrame200(4888, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
                c.send(SendString("You need 50 combat and slayer", 4890))
                c.send(SendString("to be assign tasks from me!", 4891))
                c.send(SendString("Click here to continue", 4892))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
                c.sendFrame164(4887)
                c.NpcDialogueSend = true
                return@handle true
            }

            if (c.NpcTalkTo == 403 && (!c.checkItem(989) || c.getLevel(Skill.SLAYER) < 50)) {
                c.sendFrame200(4888, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
                c.send(SendString("You need a crystal key and 50 slayer", 4890))
                c.send(SendString("to be assign tasks from me!", 4891))
                c.send(SendString("Click here to continue", 4892))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
                c.sendFrame164(4887)
                c.NpcDialogueSend = true
                return@handle true
            }

            val tasks = when (c.NpcTalkTo) {
                402 -> SlayerTask.mazchnaTasks(c)
                403 -> SlayerTask.vannakaTasks(c)
                405 -> SlayerTask.duradelTasks(c)
                else -> {
                    c.send(SendMessage("This npc do not wish to talk to you!"))
                    c.NpcDialogueSend = true
                    return@handle true
                }
            }

            if (tasks.isEmpty()) {
                c.send(SendMessage("You cant get any task!"))
                c.NpcDialogueSend = true
                return@handle true
            }

            if (c.getSlayerData()[3] > 0) {
                c.sendFrame200(4883, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
                c.send(SendString("You already have a task!", 4885))
                c.send(SendString("Click here to continue", 4886))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
                c.sendFrame164(4882)
                c.NpcDialogueSend = true
                return@handle true
            }

            val task = tasks[Misc.random(tasks.size - 1)]
            val amt = task.assignedAmountRange.value
            c.sendFrame200(4901, c.npcFace)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4902))
            c.send(SendString("You must go out and kill $amt ${task.textRepresentation}", 4903))
            c.send(SendString("If you want a new task that's too bad", 4904))
            c.send(SendString("Visit Dodian.net for a slayer guide", 4905))
            c.send(SendString("", 4906))
            c.send(SendString("Click here to continue", 4907))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4901))
            c.sendFrame164(4900)
            c.getSlayerData()[0] = c.NpcTalkTo
            c.getSlayerData()[1] = task.ordinal
            c.getSlayerData()[2] = amt
            c.getSlayerData()[3] = amt
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.CANCEL_INFO) { c ->
            val taskMonster = currentTaskName(c)
            val skipCost = when (c.getSlayerData()[0]) {
                403 -> 250000
                405 -> 500000
                else -> 100000
            }

            c.sendFrame200(4888, c.npcFace)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
            c.send(SendString(if (taskMonster.isEmpty()) "You do not have a task currently." else "I can cancel your ${taskMonster.lowercase()} task", 4890))
            c.send(SendString(if (taskMonster.isEmpty()) "Talk to me to get one." else "task for $skipCost coins.", 4891))
            c.send(SendString("Click here to continue", 4892))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
            c.sendFrame164(4887)
            if (taskMonster.isNotEmpty()) {
                c.nextDiag = DialogueIds.Slayer.CANCEL_CONFIRM
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.CANCEL_CONFIRM) { c ->
            val currentTaskName = currentTaskName(c)
            if (currentTaskName.isNotEmpty()) {
                DialogueUi.showPlayerOption(c, arrayOf("Do you wish to cancel your ${currentTaskName.lowercase()} task?", "Yes", "No"))
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.CANCEL_DO) { c ->
            val taskName = currentTaskName(c)
            val cost = when (c.getSlayerData()[0]) {
                403 -> 250000
                405 -> 500000
                else -> 100000
            }

            val coinAmount = c.getInvAmt(995)
            if (coinAmount >= cost) {
                c.deleteItem(995, cost)
                c.checkItemUpdate()
                c.getSlayerData()[3] = 0
                c.sendFrame200(4883, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
                c.send(SendString("I have now canceled your ${taskName.lowercase()} task!", 4885))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
                c.sendFrame164(4882)
                c.nextDiag = DialogueIds.Slayer.OPTIONS
            } else {
                c.sendFrame200(4883, c.npcFace)
                c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
                c.send(SendString("You do not have enough coins to cancel your task!", 4885))
                c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
                c.sendFrame164(4882)
            }
            c.send(SendString("Click here to continue", 4886))
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.UPGRADE_INFO) { c ->
            val slayerHelm = c.playerHasItem(11864)
            if (!c.playerHasItem(8921) && !slayerHelm) {
                c.showNPCChat(c.NpcTalkTo, 596, arrayOf("You do not have a black mask or a slayer helmet!"))
            } else {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Would you like to upgrade your ${if (slayerHelm) "slayer helmet" else "black mask"}?", "It will cost you 2 million gold pieces."))
                c.nextDiag = DialogueIds.Slayer.UPGRADE_CONFIRM
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.UPGRADE_CONFIRM) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to upgrade?", "Yes, please", "No, please"))
            true
        }

        builder.handle(DialogueIds.Slayer.UPGRADE_PLAYER_ACK) { c ->
            c.showPlayerChat(arrayOf("Yes, thank you."), 614)
            c.NpcDialogueSend = true
            c.nextDiag = DialogueIds.Slayer.UPGRADE_DO
            true
        }

        builder.handle(DialogueIds.Slayer.UPGRADE_DO) { c ->
            if (!c.playerHasItem(995, 2000000)) {
                c.showNPCChat(c.NpcTalkTo, 596, arrayOf("You do not have enough money!"))
            } else {
                val gotHelmet = c.playerHasItem(11864)
                c.deleteItem(995, 2000000)
                c.deleteItem(if (gotHelmet) 11864 else 8921, 1)
                c.addItem(if (gotHelmet) 11865 else 11784, 1)
                c.checkItemUpdate()
                c.showNPCChat(c.NpcTalkTo, 592, arrayOf("Here is your imbued ${if (gotHelmet) "slayer helmet" else "black mask"}."))
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.TELEPORT) { c ->
            c.sendFrame200(4883, c.npcFace)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Be careful out there!", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            c.triggerTele(2542, 3306, 0, false)
            true
        }

        builder.handle(DialogueIds.Slayer.CURRENT_TASK) { c ->
            if (c.getSlayerData()[0] != -1) {
                val slayerMaster = c.getSlayerData()[0]
                val checkTask = SlayerTask.slayerTasks.getTask(c.getSlayerData()[1])
                val lines = if (checkTask != null && c.getSlayerData()[3] > 0) {
                    arrayOf(
                        "You're currently assigned to ${checkTask.textRepresentation};",
                        "you have ${c.getSlayerData()[3]} more to go.",
                        "Your current streak is ${c.getSlayerData()[4]}."
                    )
                } else {
                    arrayOf("You currently have no task.", "Speak to me again to get a new one!")
                }
                c.showNPCChat(slayerMaster, c.npcFace, lines)
            } else {
                c.send(SendMessage("You have yet to get a task. Talk to a slayer master!"))
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Slayer.RESET_COUNT) { c ->
            if (c.getSlayerData().size > 1) {
                val checkTask = SlayerTask.slayerTasks.getTask(c.getSlayerData()[1])
                if (checkTask != null) {
                    DialogueUi.showPlayerOption(c, arrayOf("Reset count for ${checkTask.textRepresentation}", "Yes", "No"))
                } else {
                    DialogueUi.showPlayerOption(c, arrayOf("Reset count", "Yes", "No"))
                }
            } else {
                DialogueUi.showPlayerOption(c, arrayOf("Reset count", "Yes", "No"))
            }
            c.NpcDialogueSend = true
            true
        }
    }

    private fun currentTaskName(c: Client): String {
        if (c.getSlayerData()[0] == -1 || c.getSlayerData()[3] <= 0) {
            return ""
        }
        return SlayerTask.slayerTasks.getTask(c.getSlayerData()[1])?.textRepresentation ?: ""
    }
}
