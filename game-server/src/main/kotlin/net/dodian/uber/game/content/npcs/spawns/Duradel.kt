package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

internal object Duradel {
    // Stats: 405: r=60 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 405, x = 2606, y = 3398, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        SlayerMasterDialogue.startIntro(client, npc.id)
        return true
    }

    fun onSecondClick(client: Client, npc: Npc): Boolean {
        SlayerMasterDialogue.assignTask(client, npc.id)
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onThirdClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 15
        return true
    }
}

internal object SlayerMasterDialogue {

    @JvmStatic
    fun startIntro(client: Client, npcId: Int) {
        DialogueService.start(client) {
            npcChat(npcId, DialogueEmote.DEFAULT, "Need help with your slayer assignment?")
            options(
                title = "Select an option",
                DialogueOption("Yes please.") {
                    mainMenu(client, npcId)
                },
                DialogueOption("No thanks.") {
                    finish()
                },
            )
        }
    }

    @JvmStatic
    fun assignTask(client: Client, npcId: Int) {
        if (!hasRequirements(client, npcId)) {
            return
        }

        val tasks = when (npcId) {
            402 -> SlayerTask.mazchnaTasks(client)
            403 -> SlayerTask.vannakaTasks(client)
            405 -> SlayerTask.duradelTasks(client)
            else -> emptyList()
        }

        if (tasks.isEmpty()) {
            client.send(SendMessage("You cant get any task!"))
            return
        }

        if (client.getSlayerData()[3] > 0) {
            client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("You already have a task!"))
            return
        }

        val task = tasks[Misc.random(tasks.size - 1)]
        val amount = task.assignedAmountRange.value
        client.getSlayerData()[0] = npcId
        client.getSlayerData()[1] = task.ordinal
        client.getSlayerData()[2] = amount
        client.getSlayerData()[3] = amount
        client.showNPCChat(
            npcId,
            DialogueEmote.DEFAULT.id,
            arrayOf(
                "You must go out and kill $amount ${task.textRepresentation}",
                "If you want a new task that's too bad",
                "Visit Dodian.net for a slayer guide",
            ),
        )
    }

    @JvmStatic
    fun showCurrentTask(client: Client) {
        val slayerMaster = client.getSlayerData()[0]
        if (slayerMaster == -1) {
            client.send(SendMessage("You have yet to get a task. Talk to a slayer master!"))
            return
        }

        val checkTask = SlayerTask.slayerTasks.getTask(client.getSlayerData()[1])
        val lines = if (checkTask != null && client.getSlayerData()[3] > 0) {
            arrayOf(
                "You're currently assigned to ${checkTask.textRepresentation};",
                "you have ${client.getSlayerData()[3]} more to go.",
                "Your current streak is ${client.getSlayerData()[4]}.",
            )
        } else {
            arrayOf("You currently have no task.", "Speak to me again to get a new one!")
        }
        client.showNPCChat(slayerMaster, DialogueEmote.DEFAULT.id, lines)
    }

    @JvmStatic
    fun showResetCountPrompt(client: Client) {
        val checkTask = SlayerTask.slayerTasks.getTask(client.getSlayerData()[1])
        val title = if (checkTask != null) {
            "Reset count for ${checkTask.textRepresentation}"
        } else {
            "Reset count"
        }

        DialogueService.start(client) {
            options(
                title = title,
                DialogueOption("Yes") {
                    action { c -> resetCurrentTaskCount(c) }
                    finish()
                },
                DialogueOption("No") {
                    finish()
                },
            )
        }
    }

    private fun net.dodian.uber.game.content.dialogue.DialogueFactory.mainMenu(client: Client, npcId: Int) {
        if (npcId == 405 && (client.determineCombatLevel() < 50 || client.getLevel(Skill.SLAYER) < 50)) {
            npcChat(npcId, DialogueEmote.DEFAULT, "You need 50 combat and slayer", "to be assign tasks from me!")
            finish()
            return
        }
        if (npcId == 403 && (!client.checkItem(989) || client.getLevel(Skill.SLAYER) < 50)) {
            npcChat(npcId, DialogueEmote.DEFAULT, "You need a crystal key and 50 slayer", "to be assign tasks from me!")
            finish()
            return
        }

        val gotHelmet = client.playerHasItem(11864)
        val taskName = currentTaskName(client)

        options(
            title = "What would you like to say?",
            DialogueOption("I'd like a task please") {
                action { c -> assignTask(c, npcId) }
                finish(closeInterfaces = false)
            },
            DialogueOption(if (taskName.isNotEmpty()) "Cancel ${taskName.lowercase()} task" else "No task to skip") {
                cancelTaskFlow(client, npcId)
            },
            DialogueOption("I'd like to upgrade my ${if (gotHelmet) "slayer helmet" else "black mask"}") {
                upgradeFlow(client, npcId)
            },
            DialogueOption("Can you teleport me to west ardougne?") {
                npcChat(npcId, DialogueEmote.DEFAULT, "Be careful out there!")
                action { c -> c.triggerTele(2542, 3306, 0, false) }
                finish(closeInterfaces = false)
            },
        )
    }

    private fun net.dodian.uber.game.content.dialogue.DialogueFactory.cancelTaskFlow(client: Client, npcId: Int) {
        val taskName = currentTaskName(client)
        val cost = cancelTaskCost(client)

        if (taskName.isEmpty()) {
            npcChat(npcId, DialogueEmote.DEFAULT, "You do not have a task currently.", "Talk to me to get one.")
            finish()
            return
        }

        npcChat(
            npcId,
            DialogueEmote.DEFAULT,
            "I can cancel your ${taskName.lowercase()} task",
            "task for $cost coins.",
        )
        options(
            title = "Do you wish to cancel your ${taskName.lowercase()} task?",
            DialogueOption("Yes") {
                action { c -> cancelTask(c, npcId, taskName) }
                finish(closeInterfaces = false)
            },
            DialogueOption("No") {
                finish()
            },
        )
    }

    private fun net.dodian.uber.game.content.dialogue.DialogueFactory.upgradeFlow(client: Client, npcId: Int) {
        val hasHelmet = client.playerHasItem(11864)
        val hasMask = client.playerHasItem(8921)
        if (!hasMask && !hasHelmet) {
            npcChat(npcId, DialogueEmote.DISTRESSED, "You do not have a black mask or a slayer helmet!")
            finish()
            return
        }

        npcChat(
            npcId,
            DialogueEmote.DEFAULT,
            "Would you like to upgrade your ${if (hasHelmet) "slayer helmet" else "black mask"}?",
            "It will cost you 2 million gold pieces.",
        )
        options(
            title = "Do you wish to upgrade?",
            DialogueOption("Yes, please") {
                playerChat(DialogueEmote.ANGRY1, "Yes, thank you.")
                action { c -> upgradeItem(c, npcId) }
                finish(closeInterfaces = false)
            },
            DialogueOption("No, please") {
                finish()
            },
        )
    }

    private fun hasRequirements(client: Client, npcId: Int): Boolean {
        if (npcId == 405 && (client.determineCombatLevel() < 50 || client.getLevel(Skill.SLAYER) < 50)) {
            client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("You need 50 combat and slayer", "to be assign tasks from me!"))
            return false
        }
        if (npcId == 403 && (!client.checkItem(989) || client.getLevel(Skill.SLAYER) < 50)) {
            client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("You need a crystal key and 50 slayer", "to be assign tasks from me!"))
            return false
        }
        return true
    }

    private fun cancelTask(client: Client, npcId: Int, taskName: String) {
        val cost = cancelTaskCost(client)
        if (client.getInvAmt(995) < cost) {
            client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("You do not have enough coins to cancel your task!"))
            return
        }

        client.deleteItem(995, cost)
        client.checkItemUpdate()
        client.getSlayerData()[3] = 0
        client.showNPCChat(npcId, DialogueEmote.DEFAULT.id, arrayOf("I have now canceled your ${taskName.lowercase()} task!"))
    }

    private fun upgradeItem(client: Client, npcId: Int) {
        if (!client.playerHasItem(995, 2_000_000)) {
            client.showNPCChat(npcId, DialogueEmote.DISTRESSED.id, arrayOf("You do not have enough money!"))
            return
        }

        val hasHelmet = client.playerHasItem(11864)
        client.deleteItem(995, 2_000_000)
        client.deleteItem(if (hasHelmet) 11864 else 8921, 1)
        client.addItem(if (hasHelmet) 11865 else 11784, 1)
        client.checkItemUpdate()
        client.showNPCChat(
            npcId,
            DialogueEmote.EVIL1.id,
            arrayOf("Here is your imbued ${if (hasHelmet) "slayer helmet" else "black mask"}."),
        )
    }

    private fun resetCurrentTaskCount(client: Client) {
        val checkTask = SlayerTask.slayerTasks.getTask(client.slayerData[1]) ?: return
        for (npcId in checkTask.npcId) {
            val npcName = Server.npcManager.getName(npcId)
            for (slot in 0 until client.monsterName.size) {
                if (client.monsterName[slot].equals(npcName, ignoreCase = true)) {
                    client.monsterCount[slot] = 0
                }
            }
        }
        client.send(SendMessage("${checkTask.textRepresentation} have now been reseted!"))
    }

    private fun cancelTaskCost(client: Client): Int {
        return when (client.getSlayerData()[0]) {
            403 -> 250_000
            405 -> 500_000
            else -> 100_000
        }
    }

    private fun currentTaskName(client: Client): String {
        if (client.getSlayerData()[0] == -1 || client.getSlayerData()[3] <= 0) {
            return ""
        }
        return SlayerTask.slayerTasks.getTask(client.getSlayerData()[1])?.textRepresentation ?: ""
    }
}
