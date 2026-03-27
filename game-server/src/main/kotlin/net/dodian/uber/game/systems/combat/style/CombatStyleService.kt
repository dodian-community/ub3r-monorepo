package net.dodian.uber.game.systems.combat.style

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
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
        when (tabInterface) {
            5855 -> {
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.PUNCH, 0, 0)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.DEFLECT &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.KICK, 2, 1)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 2)
                }
            }
            425 -> {
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.POUND, 0, 0)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.DEFLECT &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.PUMMEL, 2, 1)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 2)
                }
            }
            8460 -> {
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.JAB, 3, 0)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.DEFLECT &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SWIPE, 2, 1)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.FEND, 1, 2)
                }
            }
            12290 -> {
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.FLICK, 0, 0)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.LASH, 3, 1)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.DEFLECT, 1, 2)
                }
            }
            4446, 1764, 1749 -> {
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_RANGED, Player.fightStyle.ACCURATE, 0, 0)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.DEFLECT &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.RAPID_RANGED, Player.fightStyle.RAPID, 2, 1)
                } else {
                    setResolvedStyle(player, CombatStyle.LONGRANGE_RANGED, Player.fightStyle.LONGRANGE, 1, 2)
                }
            }
            2276 -> {
                if (player.fightType == 2 || (player.fightType == 3 &&
                        (player.weaponStyle == Player.fightStyle.DEFLECT ||
                            player.weaponStyle == Player.fightStyle.BLOCK_THREE ||
                            player.weaponStyle == Player.fightStyle.LONGRANGE))
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SLASH, 2, 2)
                } else if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.STAB, 0, 0)
                } else if (player.fightType == 1) {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                } else {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.LUNGE_STR, 2, 2)
                }
            }
            2423 -> {
                when (player.fightType) {
                    0 -> setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.CHOP, 0, 0)
                    2 -> setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SLASH, 2, 1)
                    3 -> setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.CONTROLLED, 3, 2)
                    else -> setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            3796 -> {
                when (player.fightType) {
                    0 -> setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.POUND, 0, 0)
                    2 -> setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.PUMMEL, 2, 1)
                    3 -> setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.SPIKE, 3, 2)
                    else -> setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            4679 -> {
                if (player.fightType == 3 && (player.weaponStyle == Player.fightStyle.DEFLECT ||
                        player.weaponStyle == Player.fightStyle.BLOCK_THREE ||
                        player.weaponStyle == Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.POUND, 2, 2)
                } else if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.LUNGE, 3, 2)
                } else if (player.fightType == 2) {
                    setResolvedStyle(player, CombatStyle.CONTROLLED_MELEE, Player.fightStyle.SWIPE, 3, 2)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            1698 -> {
                if (player.fightType == 3 && (player.weaponStyle == Player.fightStyle.DEFLECT ||
                        player.weaponStyle == Player.fightStyle.BLOCK_THREE ||
                        player.weaponStyle == Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SMASH, 2, 2)
                } else if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.CHOP, 0, 0)
                } else if (player.fightType == 2) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.HACK, 2, 2)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            5570 -> {
                if (player.fightType == 3 && (player.weaponStyle == Player.fightStyle.DEFLECT ||
                        player.weaponStyle == Player.fightStyle.BLOCK_THREE ||
                        player.weaponStyle == Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SMASH, 2, 2)
                } else if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.SPIKE, 0, 0)
                } else if (player.fightType == 2) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.IMPALE, 2, 2)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            4705 -> {
                if (player.fightType == 3 && (player.weaponStyle == Player.fightStyle.DEFLECT ||
                        player.weaponStyle == Player.fightStyle.BLOCK_THREE ||
                        player.weaponStyle == Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SMASH, 2, 2)
                } else if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.CHOP, 0, 0)
                } else if (player.fightType == 2) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.SLASH, 2, 2)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 3)
                }
            }
            328 -> {
                player.varbit(108, if (player.autocast_spellIndex < 0) 0 else 3)
                if (player.fightType == 0) {
                    setResolvedStyle(player, CombatStyle.ACCURATE_MELEE, Player.fightStyle.POUND, 0, 0, setVarbit43 = true)
                } else if (player.fightType == 2 || (player.fightType == 3 &&
                        player.weaponStyle != Player.fightStyle.DEFLECT &&
                        player.weaponStyle != Player.fightStyle.BLOCK_THREE &&
                        player.weaponStyle != Player.fightStyle.LONGRANGE)
                ) {
                    setResolvedStyle(player, CombatStyle.AGGRESSIVE_MELEE, Player.fightStyle.PUMMEL, 2, 1, setVarbit43 = true)
                } else {
                    setResolvedStyle(player, CombatStyle.DEFENSIVE_MELEE, Player.fightStyle.BLOCK, 1, 2, setVarbit43 = true)
                }
            }
            else -> player.sendMessage("Unhandled interface style!")
        }
    }

    private fun setResolvedStyle(
        player: Client,
        combatStyle: CombatStyle,
        weaponStyle: Player.fightStyle,
        fightType: Int,
        varbit43: Int,
        setVarbit43: Boolean = true,
    ) {
        player.combatStyle = combatStyle
        player.weaponStyle = weaponStyle
        player.fightType = fightType
        if (setVarbit43) {
            player.varbit(43, varbit43)
        }
    }
}
