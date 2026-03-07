package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions
import net.dodian.uber.game.skills.agility.Agility

object WildernessCourseObjects : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(23137) { client, _, _, _ ->
            Agility(client).WildyPipe()
            true
        }
        objectAction(23132) { client, _, _, _ ->
            Agility(client).WildyRope()
            true
        }
        objectAction(23556) { client, _, _, _ ->
            Agility(client).WildyStones()
            true
        }
        objectAction(23542) { client, _, _, _ ->
            Agility(client).WildyLog()
            true
        }
        objectAction(23640) { client, _, _, _ ->
            Agility(client).WildyClimb()
            true
        }
    },
)
