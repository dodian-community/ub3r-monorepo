package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillDialogueFactory
import net.dodian.uber.game.api.plugin.skills.SkillDialogueOption
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor

data class SlayerTaskDef(
    val name: String,
    val slayerOnly: Boolean,
    val levelMin: Int,
    val levelMax: Int,
    val amountMin: Int,
    val amountMax: Int,
    val npcIds: List<Int>,
)

object SlayerModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.slayer", "Slayer")
    val tasks: List<SlayerTaskDef> = SlayerTaskRegistry.tasks

    private const val MAZCHNA_NPC_ID = 402
    private const val VANNAKA_NPC_ID = 403
    private const val DURADEL_NPC_ID = 405
    private val masterNpcIds = intArrayOf(MAZCHNA_NPC_ID, VANNAKA_NPC_ID, DURADEL_NPC_ID)
    private const val ENCHANTED_GEM_ID = 4155
    private val slayerMaskIds = intArrayOf(11784, 11864, 11865)

    override val definition: SkillPluginDefinition = skillPlugin("Slayer", Skill.SLAYER) {
        npcClick(preset = PolicyPreset.DIALOGUE, option = 1, *masterNpcIds) { interaction ->
            startIntro(interaction.player, interaction.npc.id)
            true
        }
        npcClick(preset = PolicyPreset.DIALOGUE, option = 3, *masterNpcIds) { interaction ->
            assignTask(interaction.player, interaction.npc.id)
            true
        }
        itemClick(preset = PolicyPreset.DIALOGUE, option = 1, ENCHANTED_GEM_ID) { interaction ->
            checkGemTask(interaction)
        }
        itemClick(preset = PolicyPreset.DIALOGUE, option = 2, ENCHANTED_GEM_ID) { interaction ->
            resetGemKillCount(interaction)
        }
        itemClick(preset = PolicyPreset.DIALOGUE, option = 3, ENCHANTED_GEM_ID) { interaction ->
            showGemKillLog(interaction)
        }
        itemClick(preset = PolicyPreset.DIALOGUE, option = 3, *slayerMaskIds) { interaction ->
            disassembleOrSellMask(interaction)
        }
        val helmComponents = intArrayOf(4155, 4156, 4164, 4166, 4168, 4551, 6720, 8923, 8921, 11784)
        for (i in helmComponents.indices) {
            for (j in i + 1 until helmComponents.size) {
                itemOnItem(PolicyPreset.PRODUCTION, helmComponents[i], helmComponents[j]) { interaction ->
                    assembleSlayerHelm(interaction.player)
                }
            }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun checkGemTask(interaction: SkillItemInteraction): Boolean {
        val task = interaction.player.slayerTask
        if (task.remainingAmount <= 0) {
            interaction.player.ui.message("You have yet to get a task. Talk to a slayer master!")
        } else {
            interaction.player.ui.message(
                "You're currently assigned to ${task.taskName}; you have ${task.remainingAmount} more to go. " +
                    "Your current streak is ${task.streak}.",
            )
        }
        return true
    }

    // Legacy behavior (SlayerGemItems.onSecondClick -> SlayerMasterDialogue.showResetCountPrompt):
    // resets the kill counter tracked against the current task's monsters. The legacy version
    // gates this behind a Yes/No confirmation dialogue; the clean content API has no generic
    // confirm-prompt primitive yet, so this resets immediately and reports the outcome instead.
    private fun resetGemKillCount(interaction: SkillItemInteraction): Boolean {
        val player = interaction.player
        val task = player.slayerTask
        if (task.remainingAmount <= 0) {
            player.ui.message("You do not have a task currently. Talk to me to get one.")
            return true
        }
        if (task.resetKillCount()) {
            player.ui.message("Your kill count for ${task.taskName} has been reset.")
        }
        return true
    }

    // Legacy behavior (SlayerGemItems.onThirdClick -> QuestSend.showMonsterLog) opens a full
    // kill-log interface; simplified here to a text summary since that interface isn't part of
    // the clean content API.
    private fun showGemKillLog(interaction: SkillItemInteraction): Boolean {
        val player = interaction.player
        val task = player.slayerTask
        val kills = task.killCount()
        if (task.remainingAmount <= 0 || kills < 0) {
            player.ui.message("You have no task assigned.")
        } else {
            player.ui.message("You have killed $kills ${task.taskName} this task.")
        }
        return true
    }

    // Legacy behavior (SlayerMaskItems.onThirdClick): 11864/11865 disassemble back into their
    // component parts (gem, mirror shield, face mask, earmuffs, nosepeg, spiny helm, gloves,
    // witchwood icon, and the base mask/helm); 11784 (uncharged helm) sells back for a 70% coin
    // refund of the 2,000,000gp imbue cost, downgrading to a plain black mask.
    private fun disassembleOrSellMask(interaction: SkillItemInteraction): Boolean {
        val player = interaction.player
        return when (interaction.itemId) {
            11864, 11865 -> {
                val needed = 8 - player.inventory.freeSlots()
                if (needed > 0) {
                    player.ui.message(
                        "You need $needed empty inventory slots to disassemble the " +
                            "${player.inventory.itemName(interaction.itemId).lowercase()}.",
                    )
                    true
                } else {
                    val baseItem = if (interaction.itemId == 11865) 11784 else 8921
                    val disassembled = player.inventory.transaction {
                        remove(interaction.itemId, 1)
                        add(baseItem, 1)
                        add(4155, 1)
                        add(4156, 1)
                        add(4164, 1)
                        add(4166, 1)
                        add(4168, 1)
                        add(4551, 1)
                        add(6720, 1)
                        add(8923, 1)
                    }
                    if (disassembled) {
                        player.ui.message("You disassemble the ${player.inventory.itemName(interaction.itemId).lowercase()}.")
                    }
                    true
                }
            }
            11784 -> {
                val refund = (2_000_000.0 * 0.7).toInt()
                val sold = player.inventory.transaction {
                    remove(11784, 1)
                    add(995, refund)
                    add(8921, 1)
                }
                if (!sold) {
                    player.ui.message("You either need one free space or coins to not go beyond 2147million!")
                }
                true
            }
            else -> false
        }
    }

    private fun assembleSlayerHelm(player: net.dodian.uber.game.api.plugin.skills.SkillPlayer): Boolean {
        val components = intArrayOf(4155, 4156, 4164, 4166, 4168, 4551, 6720, 8923)
        val missingComponent = components.any { player.inventory.amount(it) <= 0 }
        val hasBlackMask = player.inventory.amount(8921) > 0
        val hasBlackMaskI = player.inventory.amount(11784) > 0
        if (missingComponent || (!hasBlackMask && !hasBlackMaskI)) {
            player.ui.message("You need an enchanted gem, mirror shield, face mask, earmuffs, nosepeg, spiny helm, slayer gloves, witchwood icon and black mask to assemble.")
            return true
        }
        if (player.skills.current(Skill.CRAFTING) < 70) {
            player.ui.message("You need level 70 crafting to assemble these items together.")
            return true
        }
        val targetHelm = if (hasBlackMaskI) 11865 else 11864
        val maskId = if (hasBlackMaskI) 11784 else 8921
        val committed = player.inventory.transaction {
            components.forEach { remove(it, 1) }
            remove(maskId, 1)
            add(targetHelm, 1)
        }
        if (committed) {
            player.ui.message("You assemble the items together and make a ${player.inventory.itemName(targetHelm).lowercase()}.")
        }
        return true
    }

    // --- Slayer master conversations -------------------------------------------------------
    // Reproduces legacy SlayerMasterDialogue.kt exactly via the SkillUi.dialogue branching-tree
    // primitive. showCurrentTask/showResetCountPrompt (+ their private resetCurrentTaskCount
    // helper) were confirmed dead code - zero callers anywhere - and were not ported; the
    // capability they'd expose (resetting the kill count) is already reachable via
    // SkillSlayerTask.resetKillCount, already used by resetGemKillCount above.
    //
    // Emote ids are the engine's raw DialogueEmote values (npcChat/playerChat default to 591 =
    // DEFAULT); 596 = DISTRESSED, 614 = ANGRY1, 592 = EVIL1.

    private data class SlayerReply(val emote: Int = 591, val lines: Array<String>)

    // Public: called both by the npcClick bindings above and directly by Mazchna.kt/Vannaka.kt/
    // Duradel.kt's own talkTo/assignment option handlers (which stay registered - deleting them
    // would drop the client-visible menu label - but are shadowed in practice since
    // SkillInteractionDispatcher is tried before the legacy NpcFamily system).
    fun startIntro(player: SkillPlayer, npcId: Int) {
        player.ui.dialogue {
            npcChat(npcId, lines = arrayOf("Need help with your slayer assignment?"))
            options(
                title = "Select an option",
                SkillDialogueOption("Yes please.") { mainMenu(player, npcId) },
                SkillDialogueOption("No thanks.") { finish() },
            )
        }
    }

    fun assignTask(player: SkillPlayer, npcId: Int) {
        val reply = assignTaskReply(player, npcId) ?: return
        player.ui.dialogue {
            npcChat(npcId, reply.emote, *reply.lines)
            finish()
        }
    }

    private fun SkillDialogueFactory.mainMenu(player: SkillPlayer, npcId: Int) {
        val gate = requirementsMessage(player, npcId)
        if (gate != null) {
            npcChat(npcId, lines = gate)
            finish()
            return
        }

        val gotHelmet = player.inventory.contains(11864)
        val taskName = currentTaskName(player)

        options(
            title = "What would you like to say?",
            SkillDialogueOption("I'd like a task please") {
                // Deferred to finishThen (not called eagerly inline like the other DSL calls
                // in this branch) - the SkillDialogueFactory/DialogueFactory builder evaluates
                // every options() branch body immediately while *constructing* the menu, not
                // when the player actually picks it, so a direct call here would silently
                // assign a task just from opening this menu. See the fixes below for the same
                // reason on the cancel/upgrade branches.
                finishThen { p -> assignTaskReply(p, npcId)?.lines?.forEach { p.ui.message(it) } }
            },
            SkillDialogueOption(if (taskName.isNotEmpty()) "Cancel ${taskName.lowercase()} task" else "No task to skip") {
                cancelTaskFlow(player, npcId)
            },
            SkillDialogueOption("I'd like to upgrade my ${if (gotHelmet) "slayer helmet" else "black mask"}") {
                upgradeFlow(player, npcId)
            },
            SkillDialogueOption("Can you teleport me to west ardougne?") {
                npcChat(npcId, lines = arrayOf("Be careful out there!"))
                finishThen { p -> p.world.teleport(SkillPosition(2542, 3306, 0)) }
            },
        )
    }

    private fun SkillDialogueFactory.cancelTaskFlow(player: SkillPlayer, npcId: Int) {
        val taskName = currentTaskName(player)
        val cost = cancelTaskCost(player)

        if (taskName.isEmpty()) {
            npcChat(npcId, lines = arrayOf("You do not have a task currently.", "Talk to me to get one."))
            finish()
            return
        }

        npcChat(npcId, lines = arrayOf("I can cancel your ${taskName.lowercase()} task", "task for $cost coins."))
        options(
            title = "Do you wish to cancel your ${taskName.lowercase()} task?",
            SkillDialogueOption("Yes") {
                finishThen { p -> cancelTaskReply(p, taskName).lines.forEach { p.ui.message(it) } }
            },
            SkillDialogueOption("No") { finish() },
        )
    }

    private fun SkillDialogueFactory.upgradeFlow(player: SkillPlayer, npcId: Int) {
        val hasHelmet = player.inventory.contains(11864)
        val hasMask = player.inventory.contains(8921)
        if (!hasMask && !hasHelmet) {
            npcChat(npcId, 596, "You do not have a black mask or a slayer helmet!")
            finish()
            return
        }

        npcChat(
            npcId,
            lines = arrayOf(
                "Would you like to upgrade your ${if (hasHelmet) "slayer helmet" else "black mask"}?",
                "It will cost you 2 million gold pieces.",
            ),
        )
        options(
            title = "Do you wish to upgrade?",
            SkillDialogueOption("Yes, please") {
                playerChat(614, "Yes, thank you.")
                finishThen { p -> upgradeItemReply(p).lines.forEach { p.ui.message(it) } }
            },
            SkillDialogueOption("No, please") { finish() },
        )
    }

    private fun cancelTaskReply(player: SkillPlayer, taskName: String): SlayerReply {
        val cost = cancelTaskCost(player)
        if (player.inventory.amount(995) < cost) {
            return SlayerReply(lines = arrayOf("You do not have enough coins to cancel your task!"))
        }
        player.inventory.transaction { remove(995, cost) }
        player.slayerTask.cancel()
        return SlayerReply(lines = arrayOf("I have now canceled your ${taskName.lowercase()} task!"))
    }

    private fun upgradeItemReply(player: SkillPlayer): SlayerReply {
        if (!player.inventory.contains(995, 2_000_000)) {
            return SlayerReply(emote = 596, lines = arrayOf("You do not have enough money!"))
        }

        val hasHelmet = player.inventory.contains(11864)
        val fromItem = if (hasHelmet) 11864 else 8921
        val toItem = if (hasHelmet) 11865 else 11784
        player.inventory.transaction {
            remove(995, 2_000_000)
            remove(fromItem, 1)
            add(toItem, 1)
        }
        return SlayerReply(
            emote = 592,
            lines = arrayOf("Here is your imbued ${if (hasHelmet) "slayer helmet" else "black mask"}."),
        )
    }

    private fun assignTaskReply(player: SkillPlayer, npcId: Int): SlayerReply? {
        requirementsMessage(player, npcId)?.let { return SlayerReply(lines = it) }

        val candidateTasks = SlayerTaskRegistry.tasksForMaster(player, npcId)
        if (candidateTasks.isEmpty()) {
            return SlayerReply(lines = arrayOf("You cant get any task!"))
        }
        if (player.slayerTask.remainingAmount > 0) {
            return SlayerReply(lines = arrayOf("You already have a task!"))
        }

        val task = candidateTasks[player.random.between(0, candidateTasks.size - 1)]
        val amount = player.random.between(task.amountMin, task.amountMax)
        player.slayerTask.assign(npcId, SlayerTaskRegistry.tasks.indexOf(task), amount)
        return SlayerReply(
            lines = arrayOf(
                "You must go out and kill $amount ${task.name}",
                "If you want a new task that's too bad",
                "Visit Dodian.net for a slayer guide",
            ),
        )
    }

    private fun requirementsMessage(player: SkillPlayer, npcId: Int): Array<String>? {
        if (npcId == DURADEL_NPC_ID && (player.profile.combatLevel < 50 || player.skills.current(Skill.SLAYER) < 50)) {
            return arrayOf("You need 50 combat and slayer", "to be assign tasks from me!")
        }
        if (npcId == VANNAKA_NPC_ID && (!player.inventory.contains(989) || player.skills.current(Skill.SLAYER) < 50)) {
            return arrayOf("You need a crystal key and 50 slayer", "to be assign tasks from me!")
        }
        return null
    }

    private fun cancelTaskCost(player: SkillPlayer): Int = when (player.slayerTask.masterNpcId) {
        VANNAKA_NPC_ID -> 250_000
        DURADEL_NPC_ID -> 500_000
        else -> 100_000
    }

    private fun currentTaskName(player: SkillPlayer): String {
        val task = player.slayerTask
        if (task.masterNpcId == -1 || task.remainingAmount <= 0) return ""
        return SlayerTaskRegistry.forOrdinal(task.ordinal)?.name ?: ""
    }
}
