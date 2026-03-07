package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions
import net.dodian.uber.game.skills.agility.Werewolf

object WerewolfCourseObjects : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(11643) { client, _, position, _ ->
            Werewolf(client).StepStone(position)
            true
        }
        objectAction(11638) { client, _, position, _ ->
            Werewolf(client).hurdle(position)
            true
        }
        objectAction(11657) { client, _, position, _ ->
            Werewolf(client).pipe(position)
            true
        }
        objectAction(11641) { client, _, position, _ ->
            Werewolf(client).slope(position)
            true
        }
        objectAction(11644, 11645, 11646) { client, _, position, _ ->
            Werewolf(client).zipLine(position)
            true
        }
        objectAction(11636) { client, _, position, _ ->
            if (client.getLevel(Skill.AGILITY) >= 60) {
                client.ReplaceObject(position.x, position.y, 11636, 2, 10)
                client.showNPCChat(5928, 601, arrayOf("Welcome to the werewolf agility course!"))
                client.transport(Position(3549, 9865, 0))
            } else {
                client.showNPCChat(5928, 616, arrayOf("Go and train your agility!"))
            }
            true
        }
    },
)
