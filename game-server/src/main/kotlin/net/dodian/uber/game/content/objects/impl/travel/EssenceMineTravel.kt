package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object EssenceMineTravel {
    private val essenceMineDestinations =
        arrayOf(
            Position(2899, 4843, 0),
            Position(2912, 4834, 0),
            Position(2921, 4846, 0),
            Position(2912, 4830, 0),
            Position(2922, 4820, 0),
            Position(2909, 4830, 0),
            Position(2899, 4820, 0),
            Position(2909, 4834, 0),
        )

    private val essenceMineReturnDestinations =
        arrayOf(
            Position(3253, 3401, 0),
            Position(3105, 9571, 0),
            Position(2681, 3325, 0),
            Position(2591, 3086, 0),
        )

    @JvmStatic
    fun sendToEssenceMine(client: Client): Boolean {
        client.queueTransport(essenceMineDestinations[Misc.random(essenceMineDestinations.size - 1)])
        return true
    }

    @JvmStatic
    fun returnFromEssenceMine(client: Client): Boolean {
        client.transport(essenceMineReturnDestinations[Misc.random(essenceMineReturnDestinations.size - 1)])
        return true
    }
}
