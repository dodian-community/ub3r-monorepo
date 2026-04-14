package net.dodian.uber.game.skill.slayer

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.npc.SlayerMasterDialogue
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.ui.QuestTabEntry
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.bindItemContentClick
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
        val checkTask = SlayerTaskDefinition.forOrdinal(client.slayerData[1])
        if (checkTask != null && client.slayerData[3] > 0) {
            client.sendMessage("You need to kill ${client.slayerData[3]} more of ${checkTask.textRepresentation} <col=FF0000>|</col> Current streak is ${client.slayerData[4]}.")
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
            if (client.slayerData[1] != -1 && client.slayerData[1] == task.ordinal) {
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

object SlayerGemItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(4155)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        if (client.inTrade || client.inDuel) {
            return true
        }
        SlayerMasterDialogue.showCurrentTask(client)
        return true
    }

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        SlayerMasterDialogue.showResetCountPrompt(client)
        return true
    }

    override fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        QuestTabEntry.showMonsterLog(client)
        return true
    }
}

object SlayerMaskItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(11784, 11864, 11865)

    override fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return when (itemId) {
            11864, 11865 -> {
                val needed = 8 - client.freeSlots()
                if (needed > 0) {
                    client.send(
                        SendMessage(
                            "you need $needed empty inventory slots to disassemble the ${client.getItemName(itemId).lowercase()}."
                        )
                    )
                    true
                } else {
                    client.deleteItem(itemId, 1)
                    client.addItem(if (itemId == 11865) 11784 else 8921, 1)
                    client.addItem(4155, 1)
                    client.addItem(4156, 1)
                    client.addItem(4164, 1)
                    client.addItem(4166, 1)
                    client.addItem(4168, 1)
                    client.addItem(4551, 1)
                    client.addItem(6720, 1)
                    client.addItem(8923, 1)
                    client.checkItemUpdate()
                    client.sendMessage("you disassemble the ${client.getItemName(itemId).lowercase()}.")
                    true
                }
            }

            11784 -> {
                val amountReturn = (2_000_000.0 * 0.7).toInt()
                if (client.addItem(995, amountReturn)) {
                    client.deleteItem(itemId, itemSlot, 1)
                    client.addItemSlot(8921, 1, itemSlot)
                    client.checkItemUpdate()
                } else {
                    client.sendMessage("You either need one free space or coins to not go beyond 2147million!")
                }
                true
            }

            else -> false
        }
    }
}

object SlayerSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Slayer", skill = Skill.SLAYER) {
            bindItemContentClick(
                preset = PolicyPreset.DIALOGUE,
                option = 1,
                content = SlayerGemItems,
            )
            bindItemContentClick(
                preset = PolicyPreset.DIALOGUE,
                option = 2,
                content = SlayerGemItems,
            )
            bindItemContentClick(
                preset = PolicyPreset.DIALOGUE,
                option = 3,
                content = SlayerGemItems,
            )
            bindItemContentClick(
                preset = PolicyPreset.DIALOGUE,
                option = 3,
                content = SlayerMaskItems,
            )
        }
}
