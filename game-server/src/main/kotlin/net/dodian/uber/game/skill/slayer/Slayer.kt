package net.dodian.uber.game.skill.slayer

import net.dodian.uber.game.npc.SlayerMasterDialogue
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.ui.QuestTabEntry
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset

object Slayer {
    @JvmStatic
    fun sendCurrentTask(client: Client) = sendTask(client)

    @JvmStatic
    fun tasksForMaster(client: Client, masterNpcId: Int): ArrayList<SlayerTaskDefinition> =
        when (masterNpcId) {
            402 -> mazchnaTasks(client)
            403 -> vannakaTasks(client)
            405 -> duradelTasks(client)
            else -> arrayListOf()
        }

    @JvmStatic
    fun mazchnaTasks(client: Client): ArrayList<SlayerTaskDefinition> =
        collectTasks(client, SlayerData.mazchna) { task ->
            when (task) {
                SlayerTaskDefinition.HEAD_MOURNER -> client.determineCombatLevel() >= 80 || client.getLevel(Skill.MAGIC) >= 80 || client.getLevel(Skill.RANGED) >= 80
                SlayerTaskDefinition.LESSER_DEMON -> !client.checkItem(2383) && !client.checkItem(989)
                SlayerTaskDefinition.SKELE_HELLHOUNDS -> !client.checkItem(2382) && !client.checkItem(989)
                SlayerTaskDefinition.FIRE_GIANTS -> client.checkItem(1543)
                else -> true
            }
        }

    @JvmStatic
    fun vannakaTasks(client: Client): ArrayList<SlayerTaskDefinition> =
        collectTasks(client, SlayerData.vannaka) { task ->
            task != SlayerTaskDefinition.MUMMY || client.checkItem(1544)
        }

    @JvmStatic
    fun duradelTasks(client: Client): ArrayList<SlayerTaskDefinition> =
        collectTasks(client, SlayerData.duradel) { task ->
            when (task) {
                SlayerTaskDefinition.HEAD_MOURNER -> client.determineCombatLevel() >= 80 || client.getLevel(Skill.MAGIC) >= 80 || client.getLevel(Skill.RANGED) >= 80
                SlayerTaskDefinition.SAN_TOJALON, SlayerTaskDefinition.BLACK_KNIGHT_TITAN -> client.checkItem(1544)
                SlayerTaskDefinition.JUNGLE_DEMON -> client.checkItem(1545)
                SlayerTaskDefinition.BLACK_DEMON -> client.checkItem(989)
                else -> true
            }
        }

    @JvmStatic
    fun sendTask(client: Client) {
        val state = client.slayerTaskState
        val checkTask = SlayerTaskDefinition.forOrdinal(state.taskOrdinal)
        if (checkTask != null && state.remainingAmount > 0) {
            client.sendMessage("You need to kill ${state.remainingAmount} more of ${checkTask.textRepresentation} <col=FF0000>|</col> Current streak is ${state.streak}.")
        } else {
            client.sendMessage("You need to be assigned a task!")
        }
    }

    private inline fun collectTasks(
        client: Client,
        tasks: Array<SlayerTaskDefinition>,
        allow: (SlayerTaskDefinition) -> Boolean,
    ): ArrayList<SlayerTaskDefinition> {
        val slayer = ArrayList<SlayerTaskDefinition>()
        var index = 0
        while (index < tasks.size) {
            val task = tasks[index]
            val slayerLevel = client.getLevel(Skill.SLAYER)
            val state = client.slayerTaskState
            if (state.taskOrdinal != -1 && state.taskOrdinal == task.ordinal) {
                index++
            } else if (task.assignedLevelRange.floor <= slayerLevel && slayerLevel <= task.assignedLevelRange.ceiling && allow(task)) {
                slayer.add(task)
                if (task == SlayerTaskDefinition.LESSER_DEMON || task == SlayerTaskDefinition.SKELE_HELLHOUNDS) {
                    slayer.add(task)
                }
            }
            index++
        }
        return slayer
    }
}

