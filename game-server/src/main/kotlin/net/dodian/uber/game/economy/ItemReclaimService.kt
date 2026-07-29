package net.dodian.uber.game.economy

import net.dodian.uber.game.model.entity.player.Client

/** Refund UI and claim boundary; durable claim records continue to use MySQL. */
object ItemReclaimService {
    @JvmStatic fun open(client: Client) = client.performSetRefundList()
    @JvmStatic fun showOptions(client: Client) = client.performSetRefundOptions()
    @JvmStatic fun claim(client: Client, position: Int) = client.performReclaim(position)
}
