package net.dodian.uber.game.content.objects.impl.farming

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object FarmingPatchGuideObjects : ObjectContent {
    private val guideObjects = intArrayOf(
        7577, 7578, 7579, 7580,
        7847, 7848, 7849, 7850,
        7962, 7963, 7964, 7965, 26579,
        8150, 8151, 8152, 8153, 27115,
        8389, 8390, 8391, 19147,
        8550, 8551, 8552, 8553, 8554, 8555, 8556, 8557, 27113, 27114,
        27111,
    )

    override val objectIds: IntArray = guideObjects.distinct().sorted().toIntArray()

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 7962) {
            client.send(SendMessage("You inspect the monolith, but can't make sense of the inscription."))
            return true
        }
        return false
    }

    override fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when {
            (objectId in 8550..8557) || objectId == 27114 || objectId == 27113 -> {
                client.showSkillMenu(Skill.FARMING.id, 0)
                true
            }
            (objectId in 7847..7850) || objectId == 27111 -> {
                client.showSkillMenu(Skill.FARMING.id, 1)
                true
            }
            objectId in 7577..7580 -> {
                client.showSkillMenu(Skill.FARMING.id, 2)
                true
            }
            (objectId in 8150..8153) || objectId == 27115 -> {
                client.showSkillMenu(Skill.FARMING.id, 3)
                true
            }
            (objectId in 8389..8391) || objectId == 19147 -> {
                client.showSkillMenu(Skill.FARMING.id, 4)
                true
            }
            (objectId in 7962..7965) || objectId == 26579 -> {
                client.showSkillMenu(Skill.FARMING.id, 5)
                true
            }
            else -> false
        }
    }

    override fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
}
