package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.combat.CombatLogoutLockService

object LogoutButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(2458, 9154)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (client.disconnected) return true
        if (System.currentTimeMillis() < client.walkBlock && !client.UsingAgility) {
            client.send(SendMessage("You are unable to logout right now."))
            return true
        }
        if (CombatLogoutLockService.isLocked(client)) {
            val seconds = CombatLogoutLockService.remainingSeconds(client)
            client.send(SendMessage("You must wait $seconds seconds before you can logout!"))
            return true
        }
        if (System.currentTimeMillis() - client.lastPlayerCombat <= 30000 && client.inWildy()) {
            client.send(SendMessage("You must wait 30 seconds after combat in the wilderness to logout."))
            client.send(SendMessage("If you X out or disconnect you will stay online for up to a minute"))
            return true
        }
        client.logout()
        return true
    }
}
