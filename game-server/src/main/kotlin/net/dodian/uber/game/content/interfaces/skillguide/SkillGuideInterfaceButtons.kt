package net.dodian.uber.game.content.interfaces.skillguide

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.guide.SkillGuidePlugin
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object SkillGuideInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            SkillGuideComponents.skillButtons.forEach { group ->
                add(
                    buttonBinding(
                        interfaceId = SkillGuideComponents.INTERFACE_ID,
                        componentId = group.componentId,
                        componentKey = group.componentKey,
                        rawButtonIds = group.rawButtonIds,
                    ) { client, _ ->
                        SkillGuidePlugin.open(client, group.skillId, 0)
                        true
                    }
                )
            }

            SkillGuideComponents.subTabs.forEach { subTab ->
                add(
                    buttonBinding(
                        interfaceId = SkillGuideComponents.INTERFACE_ID,
                        componentId = subTab.componentId,
                        componentKey = subTab.componentKey,
                        rawButtonIds = subTab.rawButtonIds,
                    ) { client, _ ->
                        when (subTab.targetTab) {
                            0 -> if (client.currentSkill < 2) SkillGuidePlugin.open(client, Skill.ATTACK.id, 0) else SkillGuidePlugin.open(client, client.currentSkill, 0)
                            1 -> if (client.currentSkill < 2) SkillGuidePlugin.open(client, Skill.DEFENCE.id, 0) else SkillGuidePlugin.open(client, client.currentSkill, 1)
                            2 -> if (client.currentSkill < 2) SkillGuidePlugin.open(client, Skill.RANGED.id, 0) else SkillGuidePlugin.open(client, client.currentSkill, 2)
                            3 -> if (client.currentSkill < 2) client.send(SendMessage("Coming soon!")) else SkillGuidePlugin.open(client, client.currentSkill, 3)
                            else -> SkillGuidePlugin.open(client, client.currentSkill, subTab.targetTab)
                        }
                        true
                    }
                )
            }
        }
}
