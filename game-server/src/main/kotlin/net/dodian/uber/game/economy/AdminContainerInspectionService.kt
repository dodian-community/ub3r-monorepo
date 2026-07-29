package net.dodian.uber.game.economy

import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.admin.CommandDbService
import net.dodian.uber.game.ui.bank.PlayerBankService
import org.slf4j.LoggerFactory

/** Staff-only online/offline container inspection boundary. */
object AdminContainerInspectionService {
    private val logger = LoggerFactory.getLogger(AdminContainerInspectionService::class.java)

    @JvmStatic
    fun openInventory(viewer: Client, playerName: String) {
        if (!mayOpen(viewer)) return
        val online = PlayerRegistry.getPlayer(playerName) as? Client
        if (online != null) {
            viewer.sendInventory(3214, ArrayList(online.playerItems.indices.map { slot -> GameItem(online.playerItems[slot] - 1, online.playerItemsN[slot]) }))
            viewer.send(SendMessage("User $playerName's inventory is now being shown."))
            viewer.checkInv = true
            return
        }
        viewer.send(SendMessage("Loading $playerName's inventory..."))
        CommandDbService.submit(
            "check-inventory",
            { CommandDbService.loadOfflineContainerView(playerName, "inventory") },
            { result -> applyOfflineInventory(viewer, playerName, result) },
            { exception -> reportFailure(viewer, "inventory", exception) },
        )
    }

    @JvmStatic
    fun openBank(viewer: Client, playerName: String) {
        if (!mayOpen(viewer)) return
        viewer.IsBanking = false
        PlayerBankService.clearBankStyleView(viewer)
        val online = PlayerRegistry.getPlayer(playerName) as? Client
        if (online != null) {
            val items = online.bankItems.indices.filter { online.bankItems[it] > 0 && online.bankItemsN[it] > 0 }
            PlayerBankService.openBankStyleView(
                viewer,
                ArrayList(items.map { online.bankItems[it] - 1 }),
                ArrayList(items.map { online.bankItemsN[it] }),
                "Examine the bank of $playerName",
            )
            return
        }
        viewer.send(SendMessage("Loading $playerName's bank..."))
        CommandDbService.submit(
            "check-bank",
            { CommandDbService.loadOfflineContainerView(playerName, "bank") },
            { result -> applyOfflineBank(viewer, playerName, result) },
            { exception -> reportFailure(viewer, "bank", exception) },
        )
    }

    private fun mayOpen(viewer: Client): Boolean {
        if (!viewer.IsBanking && !viewer.isShopping && !viewer.duelFight) return true
        viewer.send(SendMessage("Please finish with what you are doing!"))
        return false
    }

    private fun applyOfflineInventory(viewer: Client, playerName: String, result: CommandDbService.OfflineContainerViewResult) {
        if (!mayApply(viewer, playerName, result, "Inventory")) return
        viewer.sendInventory(3214, result.items)
        viewer.send(SendMessage("User $playerName's inventory is now being shown."))
        viewer.checkInv = true
    }

    private fun applyOfflineBank(viewer: Client, playerName: String, result: CommandDbService.OfflineContainerViewResult) {
        if (!mayApply(viewer, playerName, result, "Bank")) return
        val items = result.items.filter { it.id >= 0 && it.amount > 0 }
        PlayerBankService.openBankStyleView(
            viewer,
            ArrayList(items.map(GameItem::getId)),
            ArrayList(items.map(GameItem::getAmount)),
            "Examine the bank of $playerName",
        )
    }

    private fun mayApply(viewer: Client, playerName: String, result: CommandDbService.OfflineContainerViewResult, container: String): Boolean {
        if (viewer.disconnected) return false
        if (viewer.IsBanking || viewer.isShopping || viewer.duelFight) {
            viewer.send(SendMessage("$container view cancelled because you started another action."))
            return false
        }
        when (result.status) {
            CommandDbService.OfflineContainerViewResult.Status.USERNAME_NOT_FOUND -> viewer.send(SendMessage("username '$playerName' do not exist in the database!"))
            CommandDbService.OfflineContainerViewResult.Status.CHARACTER_NOT_FOUND -> viewer.send(SendMessage("username '$playerName' have yet to login!"))
            else -> return true
        }
        return false
    }

    private fun reportFailure(viewer: Client, container: String, exception: Exception) {
        if (viewer.disconnected) return
        logger.debug("Unable to load {} inspection", container, exception)
        viewer.send(SendMessage("Could not load that $container right now."))
    }
}
