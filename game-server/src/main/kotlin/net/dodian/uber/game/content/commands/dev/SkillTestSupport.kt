package net.dodian.uber.game.content.commands.dev

import java.util.Arrays
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.bank.PlayerBankService
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.skills.slayer.SlayerService
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.skills.farming.FarmingData
import net.dodian.uber.game.skills.farming.FarmingJson

private const val SKILL_TEST_BANK_AMOUNT = 100_000

private val SKILLING_SKILLS =
    arrayOf(
        Skill.PRAYER,
        Skill.COOKING,
        Skill.WOODCUTTING,
        Skill.FLETCHING,
        Skill.FISHING,
        Skill.FIREMAKING,
        Skill.CRAFTING,
        Skill.SMITHING,
        Skill.MINING,
        Skill.HERBLORE,
        Skill.AGILITY,
        Skill.THIEVING,
        Skill.SLAYER,
        Skill.FARMING,
        Skill.RUNECRAFTING,
    )

internal fun handleSkillBank(context: CommandContext, skillName: String? = null): Boolean {
    val client = context.client
    if (!canUseSkillTestingCommands(client)) {
        return false
    }

    val items =
        if (skillName == null) {
            SkillTestItemCatalog.all()
        } else {
            SkillTestItemCatalog.forSkill(skillName)
                ?: return context.usage("Unknown skill set. Try ::skillbook herblore")
        }

    val inserted =
        PlayerBankService.replaceBankContentsWithItemIds(
            client,
            items,
            SKILL_TEST_BANK_AMOUNT,
        )
            ?: run {
                context.reply("Skill bank failed: ${items.size} unique items exceeds ${client.bankSize()} bank slots.")
                return true
            }

    client.openUpBank()
    context.reply("Loaded $inserted skilling items into your bank at $SKILL_TEST_BANK_AMOUNT each.")
    return true
}

internal fun handleSkillTools(context: CommandContext): Boolean {
    val client = context.client
    if (!canUseSkillTestingCommands(client)) {
        return false
    }

    clearInventory(client)
    val tools = intArrayOf(946, 590, 233, 1733, 1734, 1755, 2347, 20011, 20014, 5509, 5510, 5512, 5514, 7409)
    tools.forEach { client.addItem(it, if (it == 1734) 1_000 else 1) }
    client.checkItemUpdate()
    context.reply("Loaded skilling tools into your inventory.")
    return true
}

internal fun handleSkillSet(context: CommandContext): Boolean {
    val client = context.client
    if (!canUseSkillTestingCommands(client)) {
        return false
    }

    val level = if (context.hasArgs(1)) context.int(1).coerceIn(1, 99) else 99
    SKILLING_SKILLS.forEach { skill ->
        client.setExperience(Skills.getXPForLevel(level), skill)
        client.setLevel(level, skill)
        when (skill) {
            Skill.PRAYER -> {
                client.maxPrayer = level
                client.currentPrayer = level
                client.drainPrayer(0)
            }
            else -> client.refreshSkill(skill)
        }
    }
    client.markSaveDirty(PlayerSaveSegment.STATS.mask)
    context.reply("Set skilling levels to $level.")
    return true
}

internal fun handleFarmTest(context: CommandContext): Boolean {
    val client = context.client
    if (!canUseSkillTestingCommands(client)) {
        return false
    }

    client.farmingJson = FarmingJson().apply { farmingLoad("") }
    with(client.farming) {
        client.updateCompost()
        client.updateFarmPatch()
    }
    client.markSaveDirty(PlayerSaveSegment.FARMING.mask)

    clearInventory(client)
    intArrayOf(
        FarmingData().RAKE,
        FarmingData().SPADE,
        FarmingData().SEED_DIBBER,
        FarmingData().SECATEURS,
        6032,
        6034,
        6036,
        5318,
        5291,
        5096,
        5370,
    ).forEach { client.addItem(it, if (it in intArrayOf(6032, 6034, 6036, 5318, 5291, 5096, 5370)) 100 else 1) }
    client.checkItemUpdate()
    context.reply("Reset farming state and loaded a farming starter inventory.")
    return true
}

internal fun handleSlayerTest(context: CommandContext): Boolean {
    val client = context.client
    if (!canUseSkillTestingCommands(client)) {
        return false
    }
    if (!context.hasArgs(1)) {
        return context.usage("Usage: ::slayertest <taskId|taskName> [amount] [master]")
    }

    val task = resolveSlayerTask(context.parts[1])
        ?: return context.reply("Unknown slayer task. Use a task id or task name.").let { true }
    val amount = (if (context.hasArgs(2) && context.parts[2].toIntOrNull() != null) context.int(2) else 100).coerceAtLeast(1)
    val masterToken =
        when {
            context.hasArgs(3) -> context.parts[3]
            context.hasArgs(2) && context.parts[2].toIntOrNull() == null -> context.parts[2]
            else -> null
        }
    val masterId = resolveSlayerMasterId(masterToken) ?: currentOrDefaultMaster(client)

    client.slayerData[0] = masterId
    client.slayerData[1] = task.ordinal
    client.slayerData[2] = amount
    client.slayerData[3] = amount
    client.markSaveDirty(PlayerSaveSegment.SLAYER.mask)
    context.reply("Set slayer task to ${task.textRepresentation} x$amount (master=$masterId).")
    return true
}

private fun canUseSkillTestingCommands(client: Client): Boolean = client.playerRights > 1

private fun clearInventory(client: Client) {
    Arrays.fill(client.playerItems, 0)
    Arrays.fill(client.playerItemsN, 0)
    client.markSaveDirty(PlayerSaveSegment.INVENTORY.mask)
}

private fun resolveSlayerTask(raw: String): net.dodian.uber.game.skills.slayer.SlayerTaskDefinition? {
    raw.toIntOrNull()?.let { return net.dodian.uber.game.skills.slayer.SlayerTaskDefinition.forOrdinal(it) }
    val normalized = raw.trim().lowercase().replace(" ", "_")
    return net.dodian.uber.game.skills.slayer.SlayerTaskDefinition.values().firstOrNull { task ->
        task.name.lowercase() == normalized ||
            task.textRepresentation.lowercase().replace(" ", "_") == normalized
    }
}

private fun resolveSlayerMasterId(raw: String?): Int? =
    when (raw?.trim()?.lowercase()) {
        null -> null
        "mazchna", "402" -> 402
        "vannaka", "403" -> 403
        "duradel", "405" -> 405
        else -> raw.toIntOrNull()
    }

private fun currentOrDefaultMaster(client: Client): Int {
    val current = client.slayerData[0]
    return if (current in intArrayOf(402, 403, 405)) current else 402
}
