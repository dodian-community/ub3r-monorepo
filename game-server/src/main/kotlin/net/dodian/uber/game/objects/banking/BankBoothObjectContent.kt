package net.dodian.uber.game.objects.banking

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

import net.dodian.uber.game.api.interaction.ObjectInteractionContext

object BankBoothObjectContent : ObjectContent {
    override val objectIds: IntArray = BankingObjectIds.boothObjects

    // No clickInteractionPolicy override: falls through to ObjectInteractionPolicy.DEFAULT (REACHABLE),
    // the generic footprint+type+rotation reach in WorldRouteService — same as every other
    // walk-up-and-use object. The object's real type/rotation now comes from the cache (Geometry.getObject).

    override fun onFirstClick(context: ObjectInteractionContext): Boolean {
        context.player.openUpBankRouted()
        return true
    }

    override fun onSecondClick(context: ObjectInteractionContext): Boolean {
        context.player.openUpBankRouted()
        return true
    }

    override fun onThirdClick(context: ObjectInteractionContext): Boolean {
        net.dodian.uber.game.economy.ItemReclaimService.open(context.player)
        return true
    }

    override fun onFourthClick(context: ObjectInteractionContext): Boolean {
        context.player.sendMessage("This bank options are not working currently!")
        return true
    }
}
