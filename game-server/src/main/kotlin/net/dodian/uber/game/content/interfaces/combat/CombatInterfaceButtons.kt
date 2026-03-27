package net.dodian.uber.game.content.interfaces.combat

import net.dodian.uber.game.systems.combat.style.CombatStyleService
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object CombatInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        CombatInterfaceComponents.styles.map { definition ->
            buttonBinding(
                interfaceId = -1,
                componentId = definition.componentId,
                componentKey = definition.componentKey,
                rawButtonIds = definition.rawButtonIds,
            ) { client, request ->
                client.weaponStyle = definition.styleByButton[request.rawButtonId] ?: defaultStyle(definition.fightType)
                client.fightType = definition.fightType
                CombatStyleService.refreshWeaponStyleUi(client)
                if (request.rawButtonId in definition.clearsAutocastOn && client.autocast_spellIndex != -1) {
                    client.resetAttack()
                    client.autocast_spellIndex = -1
                }
                true
            }
        }

    private fun defaultStyle(fightType: Int): Player.fightStyle =
        when (fightType) {
            0 -> Player.fightStyle.CHOP
            1 -> Player.fightStyle.BLOCK
            2 -> Player.fightStyle.LUNGE_STR
            else -> Player.fightStyle.LASH
        }
}

