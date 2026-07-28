package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

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
    val tasks: List<SlayerTaskDef> by lazy { loadTasks() }

    // Master conversations and assignment selection remain on the legacy route until the
    // complete 34-task dialogue/state machine is transferred.  Registering a simplified
    // plugin handler here shadowed those routes (and used the wrong Duradel id, 404 vs 405).
    private val masterNpcIds = intArrayOf(402, 403, 405)
    private const val ENCHANTED_GEM_ID = 4155
    private val slayerMaskIds = intArrayOf(11784, 11864, 11865)

    override val definition: SkillPluginDefinition = skillPlugin("Slayer", Skill.SLAYER) {
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

    private fun loadTasks(): List<SlayerTaskDef> =
        TomlRecordReader.readRecords("slayer/tasks.toml", "task").map { row ->
            val npcsStr = row.getValue("npc_ids").removeSurrounding("[", "]").split(",")
            SlayerTaskDef(
                name = row.getValue("name"),
                slayerOnly = row.getValue("slayer_only").toBoolean(),
                levelMin = row.getValue("level_min").toInt(),
                levelMax = row.getValue("level_max").toInt(),
                amountMin = row.getValue("amount_min").toInt(),
                amountMax = row.getValue("amount_max").toInt(),
                npcIds = npcsStr.map { it.trim().toInt() },
            )
        }
}
