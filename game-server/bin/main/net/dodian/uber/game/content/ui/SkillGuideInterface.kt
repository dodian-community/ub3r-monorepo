package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.skills.skillguide.SkillGuide
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object SkillGuideInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 8714

    private data class SkillGuideButtonGroup(
        val skillId: Int,
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
    )

    private val skillButtons =
        listOf(
            SkillGuideButtonGroup(Skill.ATTACK.id, 0, "skillguide.attack", intArrayOf(8654, 33206, 94167)),
            SkillGuideButtonGroup(Skill.HITPOINTS.id, 1, "skillguide.hitpoints", intArrayOf(8655, 33207, 94168)),
            SkillGuideButtonGroup(Skill.MINING.id, 2, "skillguide.mining", intArrayOf(8656, 33208, 94169)),
            SkillGuideButtonGroup(Skill.STRENGTH.id, 3, "skillguide.strength", intArrayOf(8657, 33209, 94170)),
            SkillGuideButtonGroup(Skill.AGILITY.id, 4, "skillguide.agility", intArrayOf(8658, 33210, 94171)),
            SkillGuideButtonGroup(Skill.SMITHING.id, 5, "skillguide.smithing", intArrayOf(8659, 33211, 94172)),
            SkillGuideButtonGroup(Skill.DEFENCE.id, 6, "skillguide.defence", intArrayOf(8660, 33212, 94173)),
            SkillGuideButtonGroup(Skill.HERBLORE.id, 7, "skillguide.herblore", intArrayOf(8661, 33213, 94174)),
            SkillGuideButtonGroup(Skill.FLETCHING.id, 8, "skillguide.fletching", intArrayOf(8662, 33214, 94183)),
            SkillGuideButtonGroup(Skill.RANGED.id, 9, "skillguide.ranged", intArrayOf(8663, 33215, 94176)),
            SkillGuideButtonGroup(Skill.THIEVING.id, 10, "skillguide.thieving", intArrayOf(8664, 33216, 94177)),
            SkillGuideButtonGroup(Skill.FISHING.id, 11, "skillguide.fishing", intArrayOf(8665, 33217, 94175)),
            SkillGuideButtonGroup(Skill.PRAYER.id, 12, "skillguide.prayer", intArrayOf(8666, 94179)),
            SkillGuideButtonGroup(Skill.CRAFTING.id, 13, "skillguide.crafting", intArrayOf(8667, 33219, 94180)),
            SkillGuideButtonGroup(Skill.WOODCUTTING.id, 14, "skillguide.woodcutting", intArrayOf(8668, 33220, 94184)),
            SkillGuideButtonGroup(Skill.MAGIC.id, 15, "skillguide.magic", intArrayOf(8669, 33221, 94182)),
            SkillGuideButtonGroup(Skill.FIREMAKING.id, 16, "skillguide.firemaking", intArrayOf(8670, 33222, 94181)),
            SkillGuideButtonGroup(Skill.COOKING.id, 17, "skillguide.cooking", intArrayOf(8671, 33223, 94178)),
            SkillGuideButtonGroup(Skill.RUNECRAFTING.id, 18, "skillguide.runecrafting", intArrayOf(8672, 33224, 95053)),
            SkillGuideButtonGroup(Skill.SLAYER.id, 19, "skillguide.slayer", intArrayOf(12162, 47130, 95061)),
            SkillGuideButtonGroup(Skill.FARMING.id, 20, "skillguide.farming", intArrayOf(95068)),
        )

    private data class SubTabDefinition(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val targetTab: Int,
    )

    private val subTabs =
        listOf(
            SubTabDefinition(8846, "skillguide.subtab.0", intArrayOf(8846, 34142), 0),
            SubTabDefinition(8823, "skillguide.subtab.1", intArrayOf(8823, 34119), 1),
            SubTabDefinition(8824, "skillguide.subtab.2", intArrayOf(8824, 34120), 2),
            SubTabDefinition(8827, "skillguide.subtab.3", intArrayOf(8827, 34123), 3),
            SubTabDefinition(34133, "skillguide.subtab.4", intArrayOf(34133), 4),
            SubTabDefinition(34136, "skillguide.subtab.5", intArrayOf(34136), 5),
            SubTabDefinition(34139, "skillguide.subtab.6", intArrayOf(34139), 6),
            SubTabDefinition(34155, "skillguide.subtab.7", intArrayOf(34155), 7),
        )

    override val bindings =
        buildList {
            skillButtons.forEach { group ->
                add(
                    buttonBinding(
                        interfaceId = INTERFACE_ID,
                        componentId = group.componentId,
                        componentKey = group.componentKey,
                        rawButtonIds = group.rawButtonIds,
                    ) { client, _ ->
                        SkillGuide.open(client, group.skillId, 0)
                        true
                    },
                )
            }

            subTabs.forEach { subTab ->
                add(
                    buttonBinding(
                        interfaceId = INTERFACE_ID,
                        componentId = subTab.componentId,
                        componentKey = subTab.componentKey,
                        rawButtonIds = subTab.rawButtonIds,
                    ) { client, _ ->
                        when (subTab.targetTab) {
                            0 -> if (client.currentSkill < 2) SkillGuide.open(client, Skill.ATTACK.id, 0) else SkillGuide.open(client, client.currentSkill, 0)
                            1 -> if (client.currentSkill < 2) SkillGuide.open(client, Skill.DEFENCE.id, 0) else SkillGuide.open(client, client.currentSkill, 1)
                            2 -> if (client.currentSkill < 2) SkillGuide.open(client, Skill.RANGED.id, 0) else SkillGuide.open(client, client.currentSkill, 2)
                            3 -> if (client.currentSkill < 2) client.sendMessage("Coming soon!") else SkillGuide.open(client, client.currentSkill, 3)
                            else -> SkillGuide.open(client, client.currentSkill, subTab.targetTab)
                        }
                        true
                    },
                )
            }
        }
}
