package net.dodian.uber.game.content.skills.agility.objects

import net.dodian.uber.game.content.skills.agility.AgilityCourseService
import net.dodian.uber.game.content.objects.dsl.FirstClickDslObjectContent
import net.dodian.uber.game.content.objects.dsl.firstClickObjectActions

object WildernessCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(WildernessCourseObjectComponents.PIPE) { client, _, _, _ ->
            AgilityCourseService(client).WildyPipe()
            true
        }
        objectAction(WildernessCourseObjectComponents.ROPE) { client, _, _, _ ->
            AgilityCourseService(client).WildyRope()
            true
        }
        objectAction(WildernessCourseObjectComponents.STONES) { client, _, _, _ ->
            AgilityCourseService(client).WildyStones()
            true
        }
        objectAction(WildernessCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            AgilityCourseService(client).WildyLog()
            true
        }
        objectAction(WildernessCourseObjectComponents.CLIFF) { client, _, _, _ ->
            AgilityCourseService(client).WildyClimb()
            true
        }
    },
)
