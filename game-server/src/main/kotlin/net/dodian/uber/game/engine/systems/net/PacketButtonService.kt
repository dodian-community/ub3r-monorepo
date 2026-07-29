package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SkillMultiButtonService
import net.dodian.uber.game.engine.systems.skills.SmithingSmeltingBridge

object PacketButtonService {

    private val combatConfigButtons: Set<Int> by lazy {
        val styleIds = net.dodian.uber.game.ui.CombatInterface.bindings.flatMap { it.rawButtonIds.toList() }
        styleIds.toSet()
    }

    @JvmStatic
    fun recordLastActionIndex(client: Client, actionIndex: Int) {
        client.lastButtonActionIndex = actionIndex
    }

    @JvmStatic
    fun isSmeltingInterfaceActive(client: Client): Boolean = client.activeInterfaceId == 2400

    @JvmStatic
    fun tryHandleSkillMulti(client: Client, actionButton: Int): Boolean =
        SkillMultiButtonService.tryHandle(client, actionButton)

    @JvmStatic
    fun prepareAction(client: Client, actionButton: Int) {
        if (!(actionButton >= 9157 && actionButton <= 9194)) {
            client.actionButtonId = actionButton
        }
        val preserveSmeltingSelection = client.activeInterfaceId == 2400 &&
            SmithingSmeltingBridge.isFurnaceButton(actionButton)
        val preserveSmithingSelection = client.activeInterfaceId in 1119..1123
        val preserveCombatConfig = actionButton in combatConfigButtons
        val preserveSkillMulti = SkillMultiButtonService.isSelectionButton(actionButton)
        // Teleporting has its own external busy-guard (Client.doingTeleport(), checked by
        // triggerTele before it starts a new cast). Resetting the action here would clear that
        // guard's state a step before the click's own handler runs, letting a second teleport
        // click restart the cast indefinitely instead of being rejected as busy.
        val preserveActiveTeleport = client.doingTeleport()
        if (!preserveSmeltingSelection && !preserveSmithingSelection && !preserveCombatConfig && !preserveSkillMulti &&
            !preserveActiveTeleport &&
            actionButton != 10239 && actionButton != 10238 &&
            actionButton != 6212 && actionButton != 6211
        ) {
            client.resetAction(false)
        }
    }
}
