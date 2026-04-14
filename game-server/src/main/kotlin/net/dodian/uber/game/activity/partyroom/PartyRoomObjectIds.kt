package net.dodian.uber.game.activity.partyroom

object PartyRoomObjectIds {
    val balloonObjectIds = intArrayOf(115, 116, 117, 118, 119, 120, 121, 122)
    const val DEPOSIT_CHEST = 26193
    const val FORCE_TRIGGER = 26194
    val allObjectIds = balloonObjectIds + intArrayOf(DEPOSIT_CHEST, FORCE_TRIGGER)

    @Deprecated("Use balloonObjectIds instead")
    val balloonObjects: IntArray
        get() = balloonObjectIds

    @Deprecated("Use allObjectIds instead")
    val allObjects: IntArray
        get() = allObjectIds
}

