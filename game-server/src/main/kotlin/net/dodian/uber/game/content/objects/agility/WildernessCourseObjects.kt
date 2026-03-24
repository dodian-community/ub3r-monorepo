package net.dodian.uber.game.content.objects.agility

import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions
import net.dodian.uber.game.skills.agility.AgilityCourseService

object WildernessCourseObjects : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(23137) { client, _, _, _ ->
            AgilityCourseService(client).WildyPipe()
            true
        }
        objectAction(23132) { client, _, _, _ ->
            AgilityCourseService(client).WildyRope()
            true
        }
        objectAction(23556) { client, _, _, _ ->
            AgilityCourseService(client).WildyStones()
            true
        }
        objectAction(23542) { client, _, _, _ ->
            AgilityCourseService(client).WildyLog()
            true
        }
        objectAction(23640) { client, _, _, _ ->
            AgilityCourseService(client).WildyClimb()
            true
        }
    },
)
