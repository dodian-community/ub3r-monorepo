package net.dodian.uber.game.content.skills.agility

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.agility.AgilityWerewolf
import net.dodian.uber.game.systems.interaction.FirstClickDslObjectContent
import net.dodian.uber.game.systems.interaction.firstClickObjectActions

object WerewolfCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(WerewolfCourseObjectComponents.STEPPING_STONE) { client, _, position, _ ->
            AgilityWerewolf(client).StepStone(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.HURDLE) { client, _, position, _ ->
            AgilityWerewolf(client).hurdle(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.PIPE) { client, _, position, _ ->
            AgilityWerewolf(client).pipe(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.SLOPE) { client, _, position, _ ->
            AgilityWerewolf(client).slope(position)
            true
        }
        objectAction(*WerewolfCourseObjectComponents.ZIPLINE) { client, _, position, _ ->
            AgilityWerewolf(client).zipLine(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.ENTRY_GATE) { client, _, position, _ ->
            if (client.getLevel(Skill.AGILITY) >= 60) {
                client.ReplaceObject(position.x, position.y, WerewolfCourseObjectComponents.ENTRY_GATE, 2, 10)
                client.showNPCChat(5928, 601, arrayOf("Welcome to the werewolf agility course!"))
                client.transport(Position(3549, 9865, 0))
            } else {
                client.showNPCChat(5928, 616, arrayOf("Go and train your agility!"))
            }
            true
        }
    },
)
