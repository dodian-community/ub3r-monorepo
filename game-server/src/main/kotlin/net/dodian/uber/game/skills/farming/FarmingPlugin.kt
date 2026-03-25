package net.dodian.uber.game.skills.farming

import net.dodian.uber.game.model.entity.player.Client

object FarmingPlugin {
    @JvmStatic
    fun update(client: Client) = client.farming.run { client.updateFarming() }

    @JvmStatic
    fun clickPatch(client: Client, objectId: Int): Boolean = client.farming.run { client.clickPatch(objectId) }

    @JvmStatic
    fun inspectPatch(client: Client, objectId: Int) = client.farming.run { client.inspectPatch(objectId) }

    @JvmStatic
    fun interactBin(client: Client, objectId: Int, option: Int) = client.farming.run { client.interactBin(objectId, option) }

    @JvmStatic
    fun interactBinItem(client: Client, objectId: Int, itemId: Int): Boolean = client.farming.run { client.interactItemBin(objectId, itemId) }
}
