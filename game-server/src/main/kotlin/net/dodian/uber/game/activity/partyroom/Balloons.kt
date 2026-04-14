package net.dodian.uber.game.activity.partyroom

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

@Deprecated("Use PartyRoomBalloons instead")
object Balloons {
    @JvmStatic
    fun triggerBalloonEvent(client: Client) = PartyRoomBalloons.triggerSingleBalloonEvent(client)

    @JvmStatic
    fun spawnPartyEventBalloon() = PartyRoomBalloons.spawnPartyEventBalloonWave()

    @JvmStatic
    fun triggerPartyEvent(client: Client) = PartyRoomBalloons.triggerPartyEvent(client)

    @JvmStatic
    fun spawnBalloon(pos: Position): Boolean = PartyRoomBalloons.spawnBalloon(pos)

    @JvmStatic
    fun lootBalloon(client: Client, pos: Position): Boolean = PartyRoomBalloons.lootBalloon(client, pos)

    @JvmStatic
    fun openInterface(client: Client) = PartyRoomBalloons.openPartyRoomInterface(client)

    @JvmStatic
    fun displayItems(client: Client) = PartyRoomBalloons.displayDepositedItems(client)

    @JvmStatic
    fun displayOfferItems(client: Client) = PartyRoomBalloons.displayOfferedItems(client)

    @JvmStatic
    fun offerItems(client: Client, id: Int, amount: Int, ignoredSlot: Int) =
        PartyRoomBalloons.offerPartyItems(client, id, amount, ignoredSlot)

    @JvmStatic
    fun removeOfferItems(client: Client, id: Int, amount: Int, slot: Int) =
        PartyRoomBalloons.removeOfferedPartyItems(client, id, amount, slot)

    @JvmStatic
    fun acceptItems(client: Client) = PartyRoomBalloons.acceptOfferedPartyItems(client)

    @JvmStatic
    fun eventActive(): Boolean = PartyRoomBalloons.isPartyEventActive()

    @JvmStatic
    fun spawnedBalloons(): Boolean = PartyRoomBalloons.hasSpawnedPartyBalloons()

    @JvmStatic
    fun updateBalloons(client: Client) = PartyRoomBalloons.updateVisibleBalloons(client)
}

