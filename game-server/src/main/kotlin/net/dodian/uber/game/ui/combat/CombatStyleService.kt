package net.dodian.uber.game.ui.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.CombatStyle
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerCore as Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object CombatStyleService {

    private val weaponProfiles = listOf(
        WeaponCombatProfile(listOf("unarmed"), 5855),
        WeaponCombatProfile(listOf("whip", "scythe"), 12290),
        WeaponCombatProfile(listOf("crossbow"), 1749),
        WeaponCombatProfile(listOf("bow", "seercull"), 1764),
        WeaponCombatProfile(listOf("darts", "knifes"), 4446),
        WeaponCombatProfile(listOf("wand", "staff", "toktz-mej-tal"), 328),
        WeaponCombatProfile(listOf("dart", "knife", "javelin"), 4446),
        WeaponCombatProfile(listOf("2h"), 4705),
        WeaponCombatProfile(listOf("dagger", "keris", "sword", "toktz-xil-ak", "wolfbane"), 2276),
        WeaponCombatProfile(listOf("scimitar", "longsword", "toktz-xil-ek"), 2423),
        WeaponCombatProfile(listOf("pickaxe"), 5570),
        WeaponCombatProfile(listOf("axe", "battleaxe"), 1698),
        WeaponCombatProfile(listOf("halberd"), 8460),
        WeaponCombatProfile(listOf("spear"), 4679),
        WeaponCombatProfile(listOf("mace", "flail"), 3796),
        WeaponCombatProfile(listOf("hammer", "maul", "chicken", "tzhaar-ket-om", "tzhaar-ket-em"), 425),
    )

    @JvmStatic
    fun refreshWeaponStyleUi(player: Client) {
        val weaponId = player.equipment[Equipment.Slot.WEAPON.id]
        val itemName = if (weaponId > 0) Server.itemManager.getName(weaponId) else "Unarmed"
        val profile = resolveCombatStyleForWeapon(player)

        if (profile == null) {
            val interfaceId = 5855
            player.setSidebarInterface(0, interfaceId)
            player.sendInterfaceModel(interfaceId + 1, 200, weaponId)
            player.sendString("Unhandled item!", interfaceId + 2)
            return
        }

        applySelectedFightType(player, profile.interfaceId)
        player.currentCombatInterface = profile.interfaceId
        val itemOnInterfaceId = profile.interfaceId + 1
        val textOnInterfaceId = if (itemName.equals("unarmed", ignoreCase = true)) {
            profile.interfaceId + 2
        } else if (profile.interfaceId == 328) {
            355
        } else {
            profile.interfaceId + 3
        }
        player.setSidebarInterface(0, profile.interfaceId)
        player.sendInterfaceModel(itemOnInterfaceId, 200, weaponId)
        player.sendString(itemName, textOnInterfaceId)
        player.updateAutoRetaliate()
    }

    @JvmStatic
    fun resolveCombatStyleForWeapon(player: Client): WeaponCombatProfile? {
        val weaponId = player.equipment[Equipment.Slot.WEAPON.id]
        val itemName = if (weaponId > 0) Server.itemManager.getName(weaponId).lowercase() else "unarmed"
        return weaponProfiles.firstOrNull { profile ->
            profile.nameMatchers.any(itemName::contains)
        }
    }

    @JvmStatic
    fun applySelectedFightType(player: Client, tabInterface: Int) {
        val slot = resolveSlot(player, tabInterface)

        when (tabInterface) {
            5855 -> apply3Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 1)
            425 -> apply3Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 1)
            8460 -> apply3Button(player, slot, CombatStyle.CONTROLLED_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 3, 2, 1)
            12290 -> apply3Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.CONTROLLED_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 3, 1)
            4446, 1764, 1749 -> apply3Button(player, slot, CombatStyle.ACCURATE_RANGED, CombatStyle.RAPID_RANGED, CombatStyle.LONGRANGE_RANGED, 0, 2, 1)
            2276 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 2, 1)
            2423 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.CONTROLLED_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 3, 1)
            3796 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.CONTROLLED_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 3, 1)
            4679 -> apply4Button(player, slot, CombatStyle.CONTROLLED_MELEE, CombatStyle.CONTROLLED_MELEE, CombatStyle.CONTROLLED_MELEE, CombatStyle.DEFENSIVE_MELEE, 3, 3, 3, 1)
            1698 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 2, 1)
            5570 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 2, 1)
            4705 -> apply4Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 2, 1)
            328 -> {
                player.varbit(108, if (player.autocast_spellIndex < 0) 0 else 3)
                apply3Button(player, slot, CombatStyle.ACCURATE_MELEE, CombatStyle.AGGRESSIVE_MELEE, CombatStyle.DEFENSIVE_MELEE, 0, 2, 1)
            }
            else -> player.sendMessage("Unhandled interface style!")
        }
    }

    private fun apply3Button(
        player: Client, slot: Int,
        style0: CombatStyle, style1: CombatStyle, style2: CombatStyle,
        ft0: Int, ft1: Int, ft2: Int,
    ) {
        val styles = intArrayOf(0, 1, 2)
        val combatStyles = arrayOf(style0, style1, style2)
        val fightTypes = intArrayOf(ft0, ft1, ft2)
        val idx = styles.indexOf(slot).takeIf { it >= 0 } ?: 0
        player.combatStyle = combatStyles[idx]
        player.fightType = fightTypes[idx]
        player.varbit(43, slot)
    }

    private fun apply4Button(
        player: Client, slot: Int,
        style0: CombatStyle, style1: CombatStyle, style2: CombatStyle, style3: CombatStyle,
        ft0: Int, ft1: Int, ft2: Int, ft3: Int,
    ) {
        val styles = intArrayOf(0, 1, 2, 3)
        val combatStyles = arrayOf(style0, style1, style2, style3)
        val fightTypes = intArrayOf(ft0, ft1, ft2, ft3)
        val idx = styles.indexOf(slot).takeIf { it >= 0 } ?: 0
        player.combatStyle = combatStyles[idx]
        player.fightType = fightTypes[idx]
        player.varbit(43, slot)
    }

    private fun resolveSlot(player: Client, tabInterface: Int): Int {
        val weaponStyle = player.weaponStyle
        val derived = deriveSlotFromWeaponStyle(player, tabInterface, weaponStyle)
        if (derived >= 0) return derived
        return player.fightType.coerceIn(0, 3)
    }

    private fun deriveSlotFromWeaponStyle(player: Client, tabInterface: Int, weaponStyle: Player.fightStyle): Int {
        return when (tabInterface) {
            5855 -> when (weaponStyle) { Player.fightStyle.PUNCH -> 0; Player.fightStyle.KICK -> 1; Player.fightStyle.BLOCK -> 2; else -> -1 }
            425 -> when (weaponStyle) { Player.fightStyle.POUND -> 0; Player.fightStyle.PUMMEL -> 1; Player.fightStyle.BLOCK, Player.fightStyle.BLOCK_THREE -> 2; else -> -1 }
            8460 -> when (weaponStyle) { Player.fightStyle.JAB -> 0; Player.fightStyle.SWIPE -> 1; Player.fightStyle.FEND -> 2; else -> -1 }
            12290 -> when (weaponStyle) { Player.fightStyle.FLICK -> 0; Player.fightStyle.LASH -> 1; Player.fightStyle.DEFLECT -> 2; else -> -1 }
            4446, 1764, 1749 -> when (weaponStyle) { Player.fightStyle.ACCURATE -> 0; Player.fightStyle.RAPID -> 1; Player.fightStyle.LONGRANGE -> 2; else -> -1 }
            2276 -> when (weaponStyle) { Player.fightStyle.STAB -> 0; Player.fightStyle.LUNGE_STR -> 1; Player.fightStyle.SLASH -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            2423 -> when (weaponStyle) { Player.fightStyle.CHOP -> 0; Player.fightStyle.SLASH -> 1; Player.fightStyle.CONTROLLED, Player.fightStyle.LUNGE -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            3796 -> when (weaponStyle) { Player.fightStyle.POUND -> 0; Player.fightStyle.PUMMEL -> 1; Player.fightStyle.SPIKE -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            4679 -> when (weaponStyle) { Player.fightStyle.LUNGE -> 0; Player.fightStyle.SWIPE_CON -> 1; Player.fightStyle.POUND_CON -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            1698 -> when (weaponStyle) { Player.fightStyle.CHOP -> 0; Player.fightStyle.HACK -> 1; Player.fightStyle.SMASH -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            5570 -> when (weaponStyle) { Player.fightStyle.SPIKE -> 0; Player.fightStyle.IMPALE -> 1; Player.fightStyle.SMASH -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            4705 -> when (weaponStyle) { Player.fightStyle.CHOP -> 0; Player.fightStyle.SLASH -> 1; Player.fightStyle.SMASH -> 2; Player.fightStyle.BLOCK -> 3; else -> -1 }
            328 -> {
                val currentFightType = player.fightType
                when {
                    weaponStyle == Player.fightStyle.BLOCK -> 2
                    weaponStyle == Player.fightStyle.POUND && currentFightType == 2 -> 1
                    weaponStyle == Player.fightStyle.POUND && currentFightType != 2 -> 0
                    else -> currentFightType.coerceIn(0, 2)
                }
            }
            else -> -1
        }
    }
}
