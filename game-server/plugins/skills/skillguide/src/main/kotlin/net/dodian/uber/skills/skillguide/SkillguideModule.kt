package net.dodian.uber.skills.skillguide

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillSupportDefinition
import net.dodian.uber.game.api.plugin.skills.SkillSupportModule
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillSupportModule
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor

// Each skill's guide page is fully self-contained in its own
// game-server/plugins/skills/skillguide/src/main/resources/skillguide/<skill>.toml file (read
// by the relocated TomlSkillGuideLoader, not this module) - there is deliberately no top-level
// index file to keep every skill's guide independently authorable.
object SkillguideModule : SkillSupportModule {
    val descriptor = SkillModuleDescriptor("skill.skillguide", "Skillguide")

    override val definition: SkillSupportDefinition by lazy { skillSupportModule("Skillguide") {
        itemClick(PolicyPreset.DIALOGUE, option = 1, 1856) { interaction ->
            interaction.player.ui.guideBook()
            true
        }
        skillButtons.forEach { (skillId, rawButtonIds) ->
            // These are side-tab buttons in the legacy client, so no modal
            // interface is active when their packets arrive.
            button(PolicyPreset.DIALOGUE, requiredInterfaceId = -1, rawButtonIds = rawButtonIds) { interaction ->
                interaction.player.ui.skillGuide(skillId, 0)
                true
            }
        }
        subTabs.forEach { (targetTab, rawButtonIds) ->
            button(PolicyPreset.DIALOGUE, requiredInterfaceId = INTERFACE_ID, rawButtonIds = rawButtonIds) { interaction ->
                val player = interaction.player
                val selected = player.ui.currentSkillGuideSkillId().takeIf { it >= 0 } ?: Skill.ATTACK.id
                when (targetTab) {
                    0 -> player.ui.skillGuide(if (selected < 2) Skill.ATTACK.id else selected, 0)
                    1 -> player.ui.skillGuide(if (selected < 2) Skill.DEFENCE.id else selected, 1)
                    2 -> player.ui.skillGuide(if (selected < 2) Skill.RANGED.id else selected, 2)
                    3 -> if (selected < 2) player.ui.message("Coming soon!") else player.ui.skillGuide(selected, 3)
                    else -> player.ui.skillGuide(selected, targetTab)
                }
                true
            }
        }
    } }

    private const val INTERFACE_ID = 8714
    private val skillButtons = listOf(
        Skill.ATTACK.id to intArrayOf(8654, 33206, 94167), Skill.HITPOINTS.id to intArrayOf(8655, 33207, 94168),
        Skill.MINING.id to intArrayOf(8656, 33208, 94169), Skill.STRENGTH.id to intArrayOf(8657, 33209, 94170),
        Skill.AGILITY.id to intArrayOf(8658, 33210, 94171), Skill.SMITHING.id to intArrayOf(8659, 33211, 94172),
        Skill.DEFENCE.id to intArrayOf(8660, 33212, 94173), Skill.HERBLORE.id to intArrayOf(8661, 33213, 94174),
        Skill.FLETCHING.id to intArrayOf(8662, 33214, 94183), Skill.RANGED.id to intArrayOf(8663, 33215, 94176),
        Skill.THIEVING.id to intArrayOf(8664, 33216, 94177), Skill.FISHING.id to intArrayOf(8665, 33217, 94175),
        Skill.PRAYER.id to intArrayOf(8666, 94179), Skill.CRAFTING.id to intArrayOf(8667, 33219, 94180),
        Skill.WOODCUTTING.id to intArrayOf(8668, 33220, 94184), Skill.MAGIC.id to intArrayOf(8669, 33221, 94182),
        Skill.FIREMAKING.id to intArrayOf(8670, 33222, 94181), Skill.COOKING.id to intArrayOf(8671, 33223, 94178),
        Skill.RUNECRAFTING.id to intArrayOf(8672, 33224, 95053), Skill.SLAYER.id to intArrayOf(12162, 47130, 95061),
        Skill.FARMING.id to intArrayOf(13928, 95068),
    )
    private val subTabs = listOf(
        0 to intArrayOf(8846, 34142), 1 to intArrayOf(8823, 34119), 2 to intArrayOf(8824, 34120),
        3 to intArrayOf(8827, 34123), 4 to intArrayOf(34133), 5 to intArrayOf(34136),
        6 to intArrayOf(34139), 7 to intArrayOf(34155),
    )

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "support",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )
}
