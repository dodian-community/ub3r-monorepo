package net.dodian.uber.game.content.buttons.rewards

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

object RewardLampButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011,
        11013, 11014, 11010, 11012, 11006, 11009, 11008, 11004,
        11003, 11005, 47002, 54090, 11007,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (client.genie) {
            client.send(RemoveInterfaces())
            client.genie = false
            if (client.isBusy() || client.checkBankInterface || !client.playerHasItem(2528)) {
                return true
            }
            for (i in buttonIds.indices) {
                val trainedSkill = Skill.getSkill(i) ?: continue
                if (buttonIds[i] != buttonId) continue
                if (buttonId != 54090) {
                    client.deleteItem(2528, 1)
                    client.checkItemUpdate()
                    val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                    val experience = 100 * level
                    client.giveExperience(experience, trainedSkill)
                    client.send(SendMessage("You rub the lamp and gained $experience experience in ${trainedSkill.getName()}."))
                } else {
                    client.send(SendMessage("Experience for ${trainedSkill.getName()} is disabled until 10th of July!"))
                }
            }
            return true
        }

        if (client.antique) {
            client.send(RemoveInterfaces())
            client.antique = false
            if (client.inDuel || client.duelFight || client.IsBanking || client.checkBankInterface || !client.playerHasItem(6543)) {
                return true
            }
            for (i in buttonIds.indices) {
                val trainedSkill = Skill.getSkill(i) ?: continue
                if (buttonIds[i] != buttonId) continue
                client.deleteItem(6543, 1)
                client.checkItemUpdate()
                val level = Skills.getLevelForExperience(client.getExperience(trainedSkill))
                val experience = 250 * level
                client.giveExperience(experience, trainedSkill)
                client.send(SendMessage("You rub the lamp and gained $experience experience in ${trainedSkill.getName()}."))
            }
            return true
        }

        if (client.randomed && buttonId == client.statId[client.random_skill]) {
            client.randomed = false
            client.resetTabs()
            client.send(RemoveInterfaces())
            if (!client.addItem(2528, 1)) {
                Ground.addFloorItem(client, 2528, 1)
                client.send(SendMessage("You dropped the lamp on the floor!"))
            } else {
                client.checkItemUpdate()
            }
            return true
        }

        return false
    }
}
