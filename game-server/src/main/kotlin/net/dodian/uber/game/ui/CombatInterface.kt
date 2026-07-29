package net.dodian.uber.game.ui

import net.dodian.uber.game.model.entity.player.PlayerCore as Player
import net.dodian.uber.game.ui.combat.CombatStyleService
import net.dodian.uber.game.engine.systems.combat.CombatSpecialService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

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
            // Slot 0: first/accurate style for each weapon interface. Old IDs: 1177, 1080, 14218, 22228, 48010, 21200, 6221, 6236, 17102, 8234, 30088, 18103, 9125, 6168
            CombatStyleDefinition(
                0, "combat.style.primary",
                intArrayOf(433, 782, 1704, 1772, 2282, 2429, 3802, 4454, 4685, 4711, 5567, 5860, 6137, 7768, 8466, 12298),
                0,
                mapOf(
                    433 to Player.fightStyle.POUND,      // warhammer bash/accurate
                    782 to Player.fightStyle.CHOP,       // scythe reap
                    1704 to Player.fightStyle.CHOP,      // battleaxe chop
                    1772 to Player.fightStyle.ACCURATE,  // crossbow/bow accurate
                    2282 to Player.fightStyle.STAB,      // dagger stab
                    2429 to Player.fightStyle.CHOP,      // scimitar/longsword chop
                    3802 to Player.fightStyle.POUND,     // mace pound
                    4454 to Player.fightStyle.ACCURATE,  // knife/dart accurate
                    4685 to Player.fightStyle.CONTROLLED, // spear lunge (controlled)
                    4711 to Player.fightStyle.CHOP,      // 2h sword/godsword chop
                    5567 to Player.fightStyle.SPIKE,     // pickaxe spike
                    5860 to Player.fightStyle.PUNCH,     // unarmed punch
                    6137 to Player.fightStyle.POUND,     // staff bash
                    7768 to Player.fightStyle.CHOP,      // claws chop
                    8466 to Player.fightStyle.JAB,       // halberd jab (controlled)
                    12298 to Player.fightStyle.FLICK,    // whip flick
                ),
                intArrayOf(6137), // clear autocast when staff accurate clicked
            ),
            // Slot 1: second/aggressive style. Old IDs: 1175, 22229, 1078, 3015, 33019, 6169, 8235, 9126, 18078, 21201, 48008, 14219, 6219, 6234, 17100
            CombatStyleDefinition(
                1, "combat.style.secondary",
                intArrayOf(432, 784, 1707, 1771, 2285, 2432, 3805, 4453, 4688, 4713, 4714, 5579, 5862, 6136, 7771, 8468, 12297),
                1,
                mapOf(
                    432 to Player.fightStyle.PUMMEL,     // warhammer pummel
                    784 to Player.fightStyle.SLASH,      // scythe chop/aggressive
                    1707 to Player.fightStyle.HACK,      // battleaxe hack
                    1771 to Player.fightStyle.RAPID,     // crossbow/bow rapid
                    2285 to Player.fightStyle.LUNGE_STR, // dagger lunge
                    2432 to Player.fightStyle.SLASH,     // scimitar/longsword slash
                    3805 to Player.fightStyle.PUMMEL,    // mace pummel
                    4453 to Player.fightStyle.RAPID,     // knife/dart rapid
                    4688 to Player.fightStyle.SWIPE_CON, // spear swipe (controlled)
                    4713 to Player.fightStyle.SLASH,     // 2h sword slash
                    4714 to Player.fightStyle.SLASH,     // godsword slash
                    5579 to Player.fightStyle.IMPALE,    // pickaxe impale
                    5862 to Player.fightStyle.KICK,      // unarmed kick
                    6136 to Player.fightStyle.POUND,     // staff pound
                    7771 to Player.fightStyle.SLASH,     // claws slash
                    8468 to Player.fightStyle.SWIPE,     // halberd swipe
                    12297 to Player.fightStyle.LASH,     // whip lash (controlled)
                ),
                intArrayOf(6136), // clear autocast when staff aggressive clicked
            ),
            // Slot 2: third style (controlled for 4-button weapons; defensive for 3-button weapons).
            // Old IDs: 14220, 33018, 48009, 9127, 18077, 18080, 18079
            CombatStyleDefinition(
                2, "combat.style.controlled",
                intArrayOf(431, 785, 1706, 1770, 2284, 2431, 3804, 4452, 4687, 5578, 5861, 6135, 7770, 8467, 12296),
                2,
                mapOf(
                    431 to Player.fightStyle.BLOCK,      // warhammer block (3-button defensive)
                    785 to Player.fightStyle.JAB,        // scythe jab (controlled)
                    1706 to Player.fightStyle.SMASH,     // battleaxe smash (crush)
                    1770 to Player.fightStyle.LONGRANGE, // crossbow/bow longrange
                    2284 to Player.fightStyle.SLASH,     // dagger slash
                    2431 to Player.fightStyle.LUNGE,     // scimitar/longsword lunge (controlled)
                    3804 to Player.fightStyle.SPIKE,     // mace spike (controlled)
                    4452 to Player.fightStyle.LONGRANGE, // knife/dart longrange
                    4687 to Player.fightStyle.POUND_CON, // spear pound (controlled)
                    5578 to Player.fightStyle.SMASH,     // pickaxe smash
                    5861 to Player.fightStyle.BLOCK,     // unarmed block (3-button defensive)
                    6135 to Player.fightStyle.BLOCK,     // staff focus/defensive (3-button)
                    7770 to Player.fightStyle.LUNGE,     // claws lunge (controlled)
                    8467 to Player.fightStyle.FEND,      // halberd fend (defensive, 3-button)
                    12296 to Player.fightStyle.DEFLECT,  // whip deflect (defensive, 3-button)
                ),
                intArrayOf(6135), // clear autocast when staff defensive clicked
            ),
            // Slot 3: fourth/defensive style (only 4-button melee weapons have this).
            // Old IDs: 1079, 1176, 14221, 18106, 30091, 22230, 21203, 21202, 18105, 9128, 6170, 6171, 33020, 6220, 6235, 17101, 8237, 8236
            CombatStyleDefinition(
                3, "combat.style.tertiary",
                intArrayOf(783, 1705, 2283, 2430, 3803, 4686, 4712, 5577, 7769),
                3,
                mapOf(
                    783 to Player.fightStyle.BLOCK,   // scythe block
                    1705 to Player.fightStyle.BLOCK,  // battleaxe block
                    2283 to Player.fightStyle.BLOCK,  // dagger block
                    2430 to Player.fightStyle.BLOCK,  // scimitar/longsword block
                    3803 to Player.fightStyle.BLOCK,  // mace block
                    4686 to Player.fightStyle.BLOCK,  // spear block
                    4712 to Player.fightStyle.BLOCK,  // 2h sword/godsword block
                    5577 to Player.fightStyle.BLOCK,  // pickaxe block
                    7769 to Player.fightStyle.BLOCK,  // claws block
                ),
            ),
        )

    override val bindings =
        listOf(
            buttonBinding(
                interfaceId = -1,
                componentId = 4,
                componentKey = "combat.special",
                rawButtonIds = intArrayOf(
                    777,
                    7737, 7637, 6104, 7473, 7498, 7548,
                    7562, 7587, 7623, 7662, 7687, 7723,
                    7788, 8481, 12322,
                ),
            ) { client, _ ->
                CombatSpecialService.toggleSpecial(client)
                true
            },
        ) + styles.map { definition ->
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
            0 -> Player.fightStyle.STAB
            1 -> Player.fightStyle.SLASH
            2 -> Player.fightStyle.LUNGE_STR
            3 -> Player.fightStyle.BLOCK
            else -> Player.fightStyle.BLOCK
        }
}
