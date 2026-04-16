package net.dodian.uber.game.npc

import net.dodian.uber.game.Server
import net.dodian.uber.game.skill.slayer.Slayer
import net.dodian.uber.game.skill.slayer.SlayerTaskDefinition
import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueFactory
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.util.Misc

internal object SlayerMasterDialogue {
    private data class NpcReply(
        val emote: DialogueEmote = DialogueEmote.DEFAULT,
        val lines: Array<String>,
    )

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
        val reply = assignTaskReply(client, npcId) ?: return
        client.showNPCChat(npcId, reply.emote.id, reply.lines)
    }

    @JvmStatic
    fun showCurrentTask(client: Client) {
        val state = client.slayerTaskState
        val slayerMaster = state.masterNpcId
        if (slayerMaster == -1) {
            client.sendMessage("You have yet to get a task. Talk to a slayer master!")
            return
        }

        val checkTask = SlayerTaskDefinition.forOrdinal(state.taskOrdinal)
        val lines = if (checkTask != null && state.remainingAmount > 0) {
            arrayOf(
                "You're currently assigned to ${checkTask.textRepresentation};",
                "you have ${state.remainingAmount} more to go.",
                "Your current streak is ${state.streak}.",
            )
        } else {
            arrayOf("You currently have no task.", "Speak to me again to get a new one!")
        }
        client.showNPCChat(slayerMaster, DialogueEmote.DEFAULT.id, lines)
    }

    @JvmStatic
    fun showResetCountPrompt(client: Client) {
        val checkTask = SlayerTaskDefinition.forOrdinal(client.slayerTaskState.taskOrdinal)
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

    private fun DialogueFactory.mainMenu(client: Client, npcId: Int) {
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
                val reply = assignTaskReply(client, npcId)
                if (reply != null) {
                    npcChat(npcId, reply.emote, *reply.lines)
                }
                finish()
            },
            DialogueOption(if (taskName.isNotEmpty()) "Cancel ${taskName.lowercase()} task" else "No task to skip") {
                cancelTaskFlow(client, npcId)
            },
            DialogueOption("I'd like to upgrade my ${if (gotHelmet) "slayer helmet" else "black mask"}") {
                upgradeFlow(client, npcId)
            },
            DialogueOption("Can you teleport me to west ardougne?") {
                npcChat(npcId, DialogueEmote.DEFAULT, "Be careful out there!")
                finishThen {
                    it.triggerTele(2542, 3306, 0, false)
                }
            },
        )
    }

    private fun DialogueFactory.cancelTaskFlow(client: Client, npcId: Int) {
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
                val reply = cancelTaskReply(client, taskName)
                npcChat(npcId, reply.emote, *reply.lines)
                finish()
            },
            DialogueOption("No") {
                finish()
            },
        )
    }

    private fun DialogueFactory.upgradeFlow(client: Client, npcId: Int) {
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
                val reply = upgradeItemReply(client)
                npcChat(npcId, reply.emote, *reply.lines)
                finish()
            },
            DialogueOption("No, please") {
                finish()
            },
        )
    }

    private fun cancelTaskReply(client: Client, taskName: String): NpcReply {
        val cost = cancelTaskCost(client)
        if (client.getInvAmt(995) < cost) {
            return NpcReply(lines = arrayOf("You do not have enough coins to cancel your task!"))
        }

        client.deleteItem(995, cost)
        client.checkItemUpdate()
        client.slayerTaskState = client.slayerTaskState.withRemainingAmount(0)
        return NpcReply(lines = arrayOf("I have now canceled your ${taskName.lowercase()} task!"))
    }

    private fun upgradeItemReply(client: Client): NpcReply {
        if (!client.playerHasItem(995, 2_000_000)) {
            return NpcReply(emote = DialogueEmote.DISTRESSED, lines = arrayOf("You do not have enough money!"))
        }

        val hasHelmet = client.playerHasItem(11864)
        client.deleteItem(995, 2_000_000)
        client.deleteItem(if (hasHelmet) 11864 else 8921, 1)
        client.addItem(if (hasHelmet) 11865 else 11784, 1)
        client.checkItemUpdate()
        return NpcReply(
            emote = DialogueEmote.EVIL1,
            lines = arrayOf("Here is your imbued ${if (hasHelmet) "slayer helmet" else "black mask"}."),
        )
    }

    private fun assignTaskReply(client: Client, npcId: Int): NpcReply? {
        requirementsReply(client, npcId)?.let { return it }

        val tasks = when (npcId) {
            402 -> Slayer.tasksForMaster(client, 402)
            403 -> Slayer.tasksForMaster(client, 403)
            405 -> Slayer.tasksForMaster(client, 405)
            else -> emptyList()
        }
        if (tasks.isEmpty()) {
            return NpcReply(lines = arrayOf("You cant get any task!"))
        }

        val current = client.slayerTaskState
        if (current.remainingAmount > 0) {
            return NpcReply(lines = arrayOf("You already have a task!"))
        }

        val task = tasks[Misc.random(tasks.size - 1)]
        val amount = task.assignedAmountRange.value
        client.slayerTaskState = current.withAssignment(npcId, task.ordinal, amount, amount)
        return NpcReply(
            lines =
                arrayOf(
                    "You must go out and kill $amount ${task.textRepresentation}",
                    "If you want a new task that's too bad",
                    "Visit Dodian.net for a slayer guide",
                ),
        )
    }

    private fun requirementsReply(client: Client, npcId: Int): NpcReply? {
        if (npcId == 405 && (client.determineCombatLevel() < 50 || client.getLevel(Skill.SLAYER) < 50)) {
            return NpcReply(lines = arrayOf("You need 50 combat and slayer", "to be assign tasks from me!"))
        }
        if (npcId == 403 && (!client.checkItem(989) || client.getLevel(Skill.SLAYER) < 50)) {
            return NpcReply(lines = arrayOf("You need a crystal key and 50 slayer", "to be assign tasks from me!"))
        }
        return null
    }

    private fun resetCurrentTaskCount(client: Client) {
        val checkTask = SlayerTaskDefinition.forOrdinal(client.slayerTaskState.taskOrdinal) ?: return
        for (npcId in checkTask.npcId) {
            val npcName = Server.npcManager.getName(npcId)
            for (slot in 0 until client.monsterName.size) {
                if (client.monsterName[slot].equals(npcName, ignoreCase = true)) {
                    client.monsterCount[slot] = 0
                }
            }
        }
        client.sendMessage("${checkTask.textRepresentation} have now been reseted!")
    }

    private fun cancelTaskCost(client: Client): Int {
        return when (client.slayerTaskState.masterNpcId) {
            403 -> 250_000
            405 -> 500_000
            else -> 100_000
        }
    }

    private fun currentTaskName(client: Client): String {
        val state = client.slayerTaskState
        if (state.masterNpcId == -1 || state.remainingAmount <= 0) {
            return ""
        }
        return SlayerTaskDefinition.forOrdinal(state.taskOrdinal)?.textRepresentation ?: ""
    }
}
