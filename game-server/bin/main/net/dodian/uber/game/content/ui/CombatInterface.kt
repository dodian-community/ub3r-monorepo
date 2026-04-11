package net.dodian.uber.game.content.ui

import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.content.combat.style.CombatStyleService
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

private data class CombatStyleDefinition(
    val componentId: Int,
    val componentKey: String,
    val rawButtonIds: IntArray,
    val fightType: Int,
    val styleByButton: Map<Int, Player.fightStyle>,
    val clearsAutocastOn: IntArray = intArrayOf(),
)

object CombatInterface : InterfaceButtonContent {
    private val styles =
        listOf(
            CombatStyleDefinition(0, "combat.style.primary", intArrayOf(1177, 1080, 14218, 22228, 48010, 21200, 6221, 6236, 17102, 8234, 30088, 18103, 9125, 6168), 0, mapOf(1177 to Player.fightStyle.POUND, 1080 to Player.fightStyle.POUND, 14218 to Player.fightStyle.POUND, 22228 to Player.fightStyle.PUNCH, 48010 to Player.fightStyle.FLICK, 21200 to Player.fightStyle.SPIKE, 6221 to Player.fightStyle.ACCURATE, 6236 to Player.fightStyle.ACCURATE, 17102 to Player.fightStyle.ACCURATE, 8234 to Player.fightStyle.STAB), intArrayOf(1080)),
            CombatStyleDefinition(1, "combat.style.secondary", intArrayOf(1175, 22229, 1078, 3015, 33019, 6169, 8235, 9126, 18078, 21201, 48008, 14219, 6219, 6234, 17100), 1, mapOf(1175 to Player.fightStyle.BLOCK_THREE, 22229 to Player.fightStyle.BLOCK_THREE, 33019 to Player.fightStyle.FEND, 48008 to Player.fightStyle.DEFLECT, 6219 to Player.fightStyle.LONGRANGE, 6234 to Player.fightStyle.LONGRANGE, 17100 to Player.fightStyle.LONGRANGE), intArrayOf(1078)),
            CombatStyleDefinition(2, "combat.style.controlled", intArrayOf(14220, 33018, 48009, 9127, 18077, 18080, 18079), 3, mapOf(14220 to Player.fightStyle.SPIKE, 33018 to Player.fightStyle.JAB, 18077 to Player.fightStyle.LUNGE, 18079 to Player.fightStyle.POUND_CON, 18080 to Player.fightStyle.SWIPE, 9127 to Player.fightStyle.CONTROLLED)),
            CombatStyleDefinition(3, "combat.style.tertiary", intArrayOf(1079, 1176, 14221, 18106, 30091, 22230, 21203, 21202, 18105, 9128, 6170, 6171, 33020, 6220, 6235, 17101, 8237, 8236), 2, mapOf(1079 to Player.fightStyle.PUMMEL, 1176 to Player.fightStyle.PUMMEL, 14221 to Player.fightStyle.PUMMEL, 18106 to Player.fightStyle.SLASH, 30091 to Player.fightStyle.SLASH, 8236 to Player.fightStyle.SLASH, 22230 to Player.fightStyle.KICK, 21203 to Player.fightStyle.IMPALE, 6170 to Player.fightStyle.SMASH, 21202 to Player.fightStyle.SMASH, 18105 to Player.fightStyle.SMASH, 6171 to Player.fightStyle.HACK, 33020 to Player.fightStyle.SWIPE, 6220 to Player.fightStyle.RAPID, 6235 to Player.fightStyle.RAPID, 17101 to Player.fightStyle.RAPID), intArrayOf(1079)),
        )

    override val bindings =
        styles.map { definition ->
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
