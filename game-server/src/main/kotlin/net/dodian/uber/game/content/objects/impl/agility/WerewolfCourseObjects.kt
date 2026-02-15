package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.agility.Werewolf

object WerewolfCourseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(11636, 11638, 11641, 11643, 11644, 11645, 11646, 11657)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val werewolf = Werewolf(client)
        return when {
            objectId == 11643 -> {
                werewolf.StepStone(position)
                true
            }
            objectId == 11638 -> {
                werewolf.hurdle(position)
                true
            }
            objectId == 11657 -> {
                werewolf.pipe(position)
                true
            }
            objectId == 11641 -> {
                werewolf.slope(position)
                true
            }
            objectId in 11644..11646 -> {
                werewolf.zipLine(position)
                true
            }
            objectId == 11636 -> {
                if (client.getLevel(Skill.AGILITY) >= 60) {
                    client.ReplaceObject(position.x, position.y, 11636, 2, 10)
                    client.showNPCChat(5928, 601, arrayOf("Welcome to the werewolf agility course!"))
                    client.transport(Position(3549, 9865, 0))
                } else {
                    client.showNPCChat(5928, 616, arrayOf("Go and train your agility!"))
                }
                true
            }
            else -> false
        }
    }
}
