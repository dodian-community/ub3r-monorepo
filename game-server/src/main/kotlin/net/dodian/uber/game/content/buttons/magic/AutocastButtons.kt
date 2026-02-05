package net.dodian.uber.game.content.buttons.magic

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler
import net.dodian.uber.game.model.entity.player.Client

object AutocastButtons : ButtonContent {
    private val clearAutocastButtons = intArrayOf(1097, 1094, 1093)
    private val ancientAutocastButtons = intArrayOf(
        51133,
        51185,
        51091,
        24018,
        51159,
        51211,
        51111,
        51069,
        51146,
        51198,
        51102,
        51058,
        51172,
        51224,
        51122,
        51080,
    )

    override val buttonIds: IntArray = clearAutocastButtons + ancientAutocastButtons + intArrayOf(24017)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (buttonId in clearAutocastButtons) {
            client.autocast_spellIndex = -1
            client.setSidebarInterface(0, 1689)
            return true
        }

        if (buttonId in ancientAutocastButtons) {
            for (index in client.ancientButton.indices) {
                if (client.autocast_spellIndex == -1 && buttonId == client.ancientButton[index]) {
                    client.autocast_spellIndex = index
                }
            }
            CombatStyleHandler.setWeaponHandler(client)
            return true
        }

        if (buttonId == 24017) {
            CombatStyleHandler.setWeaponHandler(client)
            return true
        }

        return false
    }
}

