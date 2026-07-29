package net.dodian.uber.game.activity.partyroom

/** Per-player, transient deposit-chest offer list. */
class PartyRoomOfferState {
    val items = ArrayList<PartyRoomRewardItem>()
    fun clear() = items.clear()
}
