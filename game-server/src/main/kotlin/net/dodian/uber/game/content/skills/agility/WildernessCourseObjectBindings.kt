package net.dodian.uber.game.content.skills.agility

import net.dodian.uber.game.content.skills.agility.Agility
import net.dodian.uber.game.systems.interaction.FirstClickDslObjectContent
import net.dodian.uber.game.systems.interaction.firstClickObjectActions

object WildernessCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(WildernessCourseObjectComponents.PIPE) { client, _, _, _ ->
            Agility(client).WildyPipe()
            true
        }
        objectAction(WildernessCourseObjectComponents.ROPE) { client, _, _, _ ->
            Agility(client).WildyRope()
            true
        }
        objectAction(WildernessCourseObjectComponents.STONES) { client, _, _, _ ->
            Agility(client).WildyStones()
            true
        }
        objectAction(WildernessCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            Agility(client).WildyLog()
            true
        }
        objectAction(WildernessCourseObjectComponents.CLIFF) { client, _, _, _ ->
            Agility(client).WildyClimb()
            true
        }
    },
)
