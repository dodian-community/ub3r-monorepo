package net.dodian.uber.game.content.buttons.skills

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object SkillGuideButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        33206, 94167,
        33207, 94168,
        33208, 94169,
        33209, 94170,
        33210, 94171,
        33212, 94173,
        33215, 94176,
        94179,
        33216, 94177,
        33213, 94174,
        33219, 94180,
        33211, 94172,
        33220, 94184,
        33221, 94182,
        33222, 94181,
        33223, 94178,
        33224, 95053,
        33214, 94183,
        33217, 94175,
        34142, 34119, 34120, 34123, 34133, 34136, 34139, 34155,
        47130, 95061,
        95068,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            33206, 94167 -> client.showSkillMenu(Skill.ATTACK.getId(), 0)
            33207, 94168 -> client.showSkillMenu(Skill.HITPOINTS.getId(), 0)
            33208, 94169 -> client.showSkillMenu(Skill.MINING.getId(), 0)
            33209, 94170 -> client.showSkillMenu(Skill.STRENGTH.getId(), 0)
            33210, 94171 -> client.showSkillMenu(Skill.AGILITY.getId(), 0)
            33212, 94173 -> client.showSkillMenu(Skill.DEFENCE.getId(), 0)
            33215, 94176 -> client.showSkillMenu(Skill.RANGED.getId(), 0)
            94179 -> client.showSkillMenu(Skill.PRAYER.getId(), 0)
            33216, 94177 -> client.showSkillMenu(Skill.THIEVING.getId(), 0)
            33213, 94174 -> client.showSkillMenu(Skill.HERBLORE.getId(), 0)
            33219, 94180 -> client.showSkillMenu(Skill.CRAFTING.getId(), 0)
            33211, 94172 -> client.showSkillMenu(Skill.SMITHING.getId(), 0)
            33220, 94184 -> client.showSkillMenu(Skill.WOODCUTTING.getId(), 0)
            33221, 94182 -> client.showSkillMenu(Skill.MAGIC.getId(), 0)
            33222, 94181 -> client.showSkillMenu(Skill.FIREMAKING.getId(), 0)
            33223, 94178 -> client.showSkillMenu(Skill.COOKING.getId(), 0)
            33224, 95053 -> client.showSkillMenu(Skill.RUNECRAFTING.getId(), 0)
            33214, 94183 -> client.showSkillMenu(Skill.FLETCHING.getId(), 0)
            33217, 94175 -> client.showSkillMenu(Skill.FISHING.getId(), 0)
            34142 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.ATTACK.getId(), 0) else client.showSkillMenu(client.currentSkill, 0)
            34119 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.DEFENCE.getId(), 0) else client.showSkillMenu(client.currentSkill, 1)
            34120 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.RANGED.getId(), 0) else client.showSkillMenu(client.currentSkill, 2)
            34123 -> if (client.currentSkill < 2) client.send(SendMessage("Coming soon!")) else client.showSkillMenu(client.currentSkill, 3)
            34133 -> client.showSkillMenu(client.currentSkill, 4)
            34136 -> client.showSkillMenu(client.currentSkill, 5)
            34139 -> client.showSkillMenu(client.currentSkill, 6)
            34155 -> client.showSkillMenu(client.currentSkill, 7)
            47130, 95061 -> client.showSkillMenu(Skill.SLAYER.getId(), 0)
            95068 -> client.showSkillMenu(Skill.FARMING.getId(), 0)
            else -> return false
        }
        return true
    }
}

