package net.dodian.uber.game.content.objects.agility

import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions
import net.dodian.uber.game.skills.agility.Agility

object BarbarianCourseObjects : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(23131) { client, _, _, _ ->
            Agility(client).BarbRope()
            true
        }
        objectAction(23144) { client, _, _, _ ->
            Agility(client).BarbLog()
            true
        }
        objectAction(20211) { client, _, _, _ ->
            Agility(client).BarbNet()
            true
        }
        objectAction(23547) { client, _, _, _ ->
            Agility(client).BarbLedge()
            true
        }
        objectAction(16682) { client, _, _, _ ->
            Agility(client).BarbStairs()
            true
        }
        objectAction(1948) { client, _, position, _ ->
            val agility = Agility(client)
            when {
                position.x == 2536 && position.y == 3553 -> agility.BarbFirstWall()
                position.x == 2539 && position.y == 3553 -> agility.BarbSecondWall()
                position.x == 2542 && position.y == 3553 -> agility.BarbFinishWall()
                else -> return@objectAction false
            }
            true
        }
        objectAction(23567) { client, _, _, _ ->
            Agility(client).orangeBar()
            true
        }
        objectAction(23548) { client, _, _, _ ->
            Agility(client).yellowLedge()
            true
        }
    },
)
