package net.dodian.uber.game.ui

import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object RewardInterface : InterfaceButtonContent {
    private val skillSelectionButtons: IntArray = intArrayOf(
        10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011,
        11013, 11014, 11010, 11012, 11006, 11009, 11008, 11004,
        11003, 11005, 47002, 54090, 11007,
    )

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "rewards.skill_choice", skillSelectionButtons) { client, request ->
                if (client.genie) {
                    client.send(RemoveInterfaces())
                    client.genie = false
                    if (client.isBusy || client.checkBankInterface || !client.playerHasItem(2528)) {
                        return@buttonBinding true
                    }
                    val skillIndex = skillSelectionButtons.indexOf(request.rawButtonId)
                    val trainedSkill = Skill.getSkill(skillIndex) ?: return@buttonBinding true
                    if (request.rawButtonId != 54090) {
                        client.deleteItem(2528, 1)
                        client.checkItemUpdate()
                        val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                        val experience = 100 * level
                        ProgressionService.addXp(client, experience, trainedSkill)
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
                    val skillIndex = skillSelectionButtons.indexOf(request.rawButtonId)
                    val trainedSkill = Skill.getSkill(skillIndex) ?: return@buttonBinding true
                    client.deleteItem(6543, 1)
                    client.checkItemUpdate()
                    val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                    val experience = 250 * level
                    ProgressionService.addXp(client, experience, trainedSkill)
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
