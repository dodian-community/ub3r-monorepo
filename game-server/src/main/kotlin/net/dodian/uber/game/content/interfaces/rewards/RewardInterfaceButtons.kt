package net.dodian.uber.game.content.interfaces.rewards

import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object RewardInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "rewards.skill_choice", RewardComponents.skillSelectionButtons) { client, request ->
                if (client.genie) {
                    client.send(RemoveInterfaces())
                    client.genie = false
                    if (client.isBusy || client.checkBankInterface || !client.playerHasItem(2528)) {
                        return@buttonBinding true
                    }
                    val skillIndex = RewardComponents.skillSelectionButtons.indexOf(request.rawButtonId)
                    val trainedSkill = Skill.getSkill(skillIndex) ?: return@buttonBinding true
                    if (request.rawButtonId != 54090) {
                        client.deleteItem(2528, 1)
                        client.checkItemUpdate()
                        val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                        val experience = 100 * level
                        SkillProgressionService.gainXp(client, experience, trainedSkill)
                        client.sendMessage("You rub the lamp and gained $experience experience in ${trainedSkill.getName()}.")
                    } else {
                        client.sendMessage("Experience for ${trainedSkill.getName()} is disabled until 10th of July!")
                    }
                    return@buttonBinding true
                }

                if (client.antique) {
                    client.send(RemoveInterfaces())
                    client.antique = false
                    if (client.inDuel || client.duelFight || client.IsBanking || client.checkBankInterface || !client.playerHasItem(6543)) {
                        return@buttonBinding true
                    }
                    val skillIndex = RewardComponents.skillSelectionButtons.indexOf(request.rawButtonId)
                    val trainedSkill = Skill.getSkill(skillIndex) ?: return@buttonBinding true
                    client.deleteItem(6543, 1)
                    client.checkItemUpdate()
                    val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                    val experience = 250 * level
                    SkillProgressionService.gainXp(client, experience, trainedSkill)
                    client.sendMessage("You rub the lamp and gained $experience experience in ${trainedSkill.getName()}.")
                    return@buttonBinding true
                }

                if (client.randomed && request.rawButtonId == client.statId[client.random_skill]) {
                    client.randomed = false
                    client.resetTabs()
                    client.send(RemoveInterfaces())
                    if (!client.addItem(2528, 1)) {
                        Ground.addFloorItem(client, 2528, 1)
                        client.sendMessage("You dropped the lamp on the floor!")
                    } else {
                        client.checkItemUpdate()
                    }
                    return@buttonBinding true
                }
                false
            },
        )
}
