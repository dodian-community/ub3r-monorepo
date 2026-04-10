package net.dodian.uber.game.content.objects.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.PersonalObjectService
import net.dodian.uber.game.systems.interaction.PersonalPassageService

object LegendsGuildGateService {
    private val leftGate = Position(2728, 3349, 0)
    private val rightGate = Position(2729, 3349, 0)
    private val southLeft = Position(2728, 3348, 0)
    private val southRight = Position(2729, 3348, 0)
    private val northLeft = Position(2728, 3350, 0)
    private val northRight = Position(2729, 3350, 0)
    private const val GATE_OBJECT_LEFT = 2391
    private const val GATE_OBJECT_RIGHT = 2392
    private const val GATE_TYPE = 0
    private const val LEFT_OPEN_FACE = 0
    private const val RIGHT_OPEN_FACE = 2
    private const val PASSAGE_DURATION_MS = 5_000L

    @JvmStatic
    fun allowPassage(client: Client): Boolean {
        if (!client.premium) {
            return false
        }
        showOpenedGate(client)
        grantPassage(client)
        return true
    }

    private fun showOpenedGate(client: Client) {
        PersonalObjectService.show(client, leftGate, GATE_OBJECT_LEFT, LEFT_OPEN_FACE, GATE_TYPE)
        PersonalObjectService.show(client, rightGate, GATE_OBJECT_RIGHT, RIGHT_OPEN_FACE, GATE_TYPE)
    }

    private fun grantPassage(client: Client) {
        PersonalPassageService.grantBidirectionalEdges(
            client,
            edges =
                listOf(
                    southLeft to leftGate,
                    leftGate to northLeft,
                    southRight to rightGate,
                    rightGate to northRight,
                ),
            durationMs = PASSAGE_DURATION_MS,
        )
    }
}

