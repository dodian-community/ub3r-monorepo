package net.dodian.uber.game.systems.action

import net.dodian.uber.game.model.Position

sealed interface TravelDecision {
    data class Approved(
        val varbitValue: Int,
        val destination: Position,
    ) : TravelDecision

    data class Rejected(
        val message: String,
    ) : TravelDecision

    data class RequireUnlockDialogue(
        val dialogueId: Int,
    ) : TravelDecision

    object Ignored : TravelDecision
}

object TravelRouteService {
    private val posTrigger = intArrayOf(1, 3, 4, 7, 10, 2, 5, 6, 11)
    private val travel = arrayOf(
        intArrayOf(3057, 2803, 3421, 0), // Catherby
        intArrayOf(3058, -1, -1, 0), // Mountain aka Trollheim
        intArrayOf(3059, 3511, 3506, 0), // Castle aka Canifis
        intArrayOf(3060, 3274, 2798, 0), // Tent aka Sophanem
        intArrayOf(3056, 2863, 2971, 0), // Tree aka Shilo
        intArrayOf(48054, 2772, 3234, 0), // Totem aka Brimhaven
    )

    @JvmStatic
    fun resolve(
        home: Boolean,
        checkPos: Int,
        buttonId: Int,
        unlocked: (Int) -> Boolean,
    ): TravelDecision {
        val routeIndex = travel.indexOfFirst { it[0] == buttonId }
        if (routeIndex == -1) {
            return TravelDecision.Ignored
        }

        if ((!home && routeIndex == 0) || (home && routeIndex != 0)) {
            val message = if (!home) "You are already here!" else "Please select Catherby!"
            return TravelDecision.Rejected(message)
        }

        val destination = travel[routeIndex]
        if (destination[1] == -1) {
            return TravelDecision.Rejected("This will lead you to nothing!")
        }

        if (routeIndex > 0 && !unlocked(routeIndex - 1)) {
            return TravelDecision.RequireUnlockDialogue(48054)
        }

        val varbitValue = if (home) {
            val posIndex = checkPos + 3
            if (posIndex !in posTrigger.indices) {
                return TravelDecision.Rejected("This will lead you to nothing!")
            }
            posTrigger[posIndex]
        } else {
            val posIndex = routeIndex - 1
            if (posIndex !in posTrigger.indices) {
                return TravelDecision.Rejected("This will lead you to nothing!")
            }
            posTrigger[posIndex]
        }

        return TravelDecision.Approved(
            varbitValue = varbitValue,
            destination = Position(destination[1], destination[2], 0),
        )
    }
}
