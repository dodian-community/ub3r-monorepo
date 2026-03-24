package net.dodian.uber.game.content.objects.agility

import net.dodian.uber.game.skills.agility.AgilityCourseService
import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions

object BarbarianCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(BarbarianCourseObjectComponents.ROPE_SWING) { client, _, _, _ ->
            AgilityCourseService(client).BarbRope()
            true
        }
        objectAction(BarbarianCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            AgilityCourseService(client).BarbLog()
            true
        }
        objectAction(BarbarianCourseObjectComponents.NET) { client, _, _, _ ->
            AgilityCourseService(client).BarbNet()
            true
        }
        objectAction(BarbarianCourseObjectComponents.LEDGE) { client, _, _, _ ->
            AgilityCourseService(client).BarbLedge()
            true
        }
        objectAction(BarbarianCourseObjectComponents.STAIRS) { client, _, _, _ ->
            AgilityCourseService(client).BarbStairs()
            true
        }
        objectAction(BarbarianCourseObjectComponents.CRUMBLING_WALL) { client, _, position, _ ->
            val agility = AgilityCourseService(client)
            when {
                position.x == 2536 && position.y == 3553 -> agility.BarbFirstWall()
                position.x == 2539 && position.y == 3553 -> agility.BarbSecondWall()
                position.x == 2542 && position.y == 3553 -> agility.BarbFinishWall()
                else -> return@objectAction false
            }
            true
        }
        objectAction(BarbarianCourseObjectComponents.ORANGE_BAR) { client, _, _, _ ->
            AgilityCourseService(client).orangeBar()
            true
        }
        objectAction(BarbarianCourseObjectComponents.YELLOW_LEDGE) { client, _, _, _ ->
            AgilityCourseService(client).yellowLedge()
            true
        }
    },
)
