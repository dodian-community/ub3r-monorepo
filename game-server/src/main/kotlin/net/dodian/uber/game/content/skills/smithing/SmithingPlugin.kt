package net.dodian.uber.game.content.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.content.skills.smithing.SmithingInterfaceService
import net.dodian.uber.game.content.skills.smithing.SuperheatService

object SmithingPlugin {
    @JvmStatic
    fun openSmelting(client: Client) = SmeltingInterfaceService.open(client)

    @JvmStatic
    fun startSmelting(client: Client, amount: Int) = SmeltingInterfaceService.startFromPending(client, amount)

    @JvmStatic
    fun startSmeltingFromItem(client: Client, itemId: Int, amount: Int) =
        SmeltingInterfaceService.startFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun openSmithing(client: Client, barId: Int, anvilX: Int, anvilY: Int) =
        SmithingInterfaceService.openForBar(client, barId, anvilX, anvilY)

    @JvmStatic
    fun startSmithing(client: Client, itemId: Int, amount: Int) =
        SmithingInterfaceService.startFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun castSuperheat(client: Client, itemId: Int) = SuperheatService.cast(client, itemId)
}
