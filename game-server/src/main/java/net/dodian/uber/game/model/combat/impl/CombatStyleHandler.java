package net.dodian.uber.game.model.combat.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.combat.WeaponData;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.uber.game.network.packets.outgoing.SendString;

public abstract class CombatStyleHandler {

    public abstract void handleWeaponInterface(Entity entity, int buttonId);

    public static void setWeaponHandler(Entity entity) {
        int itemId = ((Player) entity).getEquipment()[Equipment.Slot.WEAPON.getId()];
        String itemName = itemId > 0 ? Server.itemManager.getName(itemId) : "Unarmed";
        boolean foundItem = false;
        for (WeaponData weaponData : WeaponData.values()) {
            for (int i = 0; i < weaponData.getName().length && !foundItem; i++) {
                if (itemName.toLowerCase().contains(weaponData.getName()[i])) {
                    int interfaceId = weaponData.getInterface();
                    setAttackStyle(entity, interfaceId);
                    int itemOnInterfaceId = interfaceId + 1;
                    int textOnInterfaceId = itemName.equalsIgnoreCase("unarmed") ? interfaceId + 2 : interfaceId == 328 ? 355 : interfaceId + 3;
                    ((Client) entity).setSidebarInterface(0, interfaceId);
                    ((Client) entity).sendFrame246(itemOnInterfaceId, 200, itemId);
                    ((Client) entity).send(new SendString(itemName, textOnInterfaceId));
                    foundItem = true;
                }
            }
        }
        if(!foundItem) { //Unhandled items!
            int interfaceId = 5855;
            int itemOnInterfaceId = interfaceId + 1;
            int textOnInterfaceId = interfaceId + 2;
            ((Client) entity).setSidebarInterface(0, interfaceId);
            ((Client) entity).sendFrame246(itemOnInterfaceId, 200, itemId);
            ((Client) entity).send(new SendString("Unhandled item!", textOnInterfaceId));
        }
    }

    public enum CombatStyles {
        ACCURATE_MELEE, AGGRESSIVE_MELEE, DEFENSIVE_MELEE, CONTROLLED_MELEE, ACCURATE_RANGED, RAPID_RANGED, LONGRANGE_RANGED
    }

    public static void setAttackStyle(Entity entity, int tabInterface) {
        Client player = ((Client) entity);
        //System.out.println("testing style..." +  player.weaponStyle + ", " +  player.weaponStyle.ordinal() + ", " + player.fightType);
        switch (tabInterface) {
            case 5855:
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.PUNCH;
                else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.DEFLECT
                        && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.KICK;
                    player.fightType = 2;
                } else {
                    player.weaponStyle = player.weaponStyle.BLOCK;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 1 ? 2 : player.fightType == 2 ? 1 : 0);
                break;
            case 425:
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.POUND;
                else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.DEFLECT
                        && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.PUMMEL;
                    player.fightType = 2;
                } else {
                    player.weaponStyle = player.weaponStyle.BLOCK;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 1 ? 2 : player.fightType == 2 ? 1 : 0);
                break;
            case 8460:
                if (player.fightType == 0) {
                    player.weaponStyle = player.weaponStyle.JAB;
                    player.fightType = 3;
                } else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.DEFLECT
                        && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.SWIPE;
                    player.fightType = 2;
                } else {
                    player.weaponStyle = player.weaponStyle.FEND;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 1 ? 2 : player.fightType == 2 ? 1 : 0);
                break;
            case 12290:
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.FLICK;
                else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.LASH;
                    player.fightType = 3;
                } else {
                    player.weaponStyle = player.weaponStyle.DEFLECT;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 3 ? 1 : player.fightType == 1 ? 2 : 0);
                break;
            case 4446: // Dart
            case 1764: // bow
            case 1749: // Crossbow
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.ACCURATE;
                else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.DEFLECT
                        && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.RAPID;
                    player.fightType = 2;
                } else {
                    player.weaponStyle = player.weaponStyle.LONGRANGE;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 2 ? 1 : player.fightType == 1 ? 2 : 0);
                break;
            case 2276:
                if (player.fightType == 2 || (player.fightType == 3 && (player.weaponStyle == player.weaponStyle.DEFLECT
                        || player.weaponStyle == player.weaponStyle.BLOCK_THREE || player.weaponStyle == player.weaponStyle.LONGRANGE))) {
                    player.weaponStyle = player.weaponStyle.SLASH;
                    player.fightType = 2;
                } else if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.STAB;
                else if (player.fightType == 1)
                    player.weaponStyle = player.weaponStyle.BLOCK;
                else {
                    player.weaponStyle = player.weaponStyle.LUNGE_STR;
                    player.fightType = 2;
                }
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType);
                break;
            case 2423:
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.CHOP;
                else if (player.fightType == 2)
                    player.weaponStyle = player.weaponStyle.SLASH;
                else if (player.fightType == 3)
                    player.weaponStyle = player.weaponStyle.CONTROLLED;
                else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType == 0 ? 0 : player.fightType - 1);
                break;
            case 3796:
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.POUND;
                else if (player.fightType == 2)
                    player.weaponStyle = player.weaponStyle.PUMMEL;
                else if (player.fightType == 3)
                    player.weaponStyle = player.weaponStyle.SPIKE;
                else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType == 0 ? 0 : player.fightType - 1);
                break;
            /*case 7762:
                if (player.weaponOption == 4 || (player.weaponOption == 3 && (player.fightType == fightStyle.DEFLECT
                        || player.fightType == fightStyle.BLOCK_THREE || player.fightType == fightStyle.LONGRANGE))) {
                    player.fightType = fightStyle.BLOCK;
                    player.weaponOption = 4;
                } else if (player.weaponOption == 3)
                    player.fightType = fightStyle.LUNGE;
                else if (player.weaponOption == 2)
                    player.fightType = fightStyle.SLASH;
                else
                    player.fightType = fightStyle.CHOP;
                break;*/
            case 4679:
                if (player.fightType == 3 && (player.weaponStyle == player.weaponStyle.DEFLECT
                        || player.weaponStyle == player.weaponStyle.BLOCK_THREE || player.weaponStyle == player.weaponStyle.LONGRANGE))
                    player.weaponStyle = player.weaponStyle.POUND;
                else if (player.fightType == 0) {
                    player.weaponStyle = player.weaponStyle.LUNGE;
                    player.fightType = 3;
                } else if (player.fightType == 2) {
                    player.weaponStyle = player.weaponStyle.SWIPE;
                    player.fightType = 3;
                } else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : 2);
                break;
            case 1698:
                if (player.fightType == 3 && (player.weaponStyle == player.weaponStyle.DEFLECT
                        || player.weaponStyle == player.weaponStyle.BLOCK_THREE || player.weaponStyle == player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.SMASH;
                    player.fightType = 2;
                } else if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.CHOP;
                else if (player.fightType == 2)
                    player.weaponStyle = player.weaponStyle.HACK;
                else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType);
                break;
            case 5570:
                if (player.fightType == 3 && (player.weaponStyle == player.weaponStyle.DEFLECT
                        || player.weaponStyle == player.weaponStyle.BLOCK_THREE || player.weaponStyle == player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.SMASH;
                    player.fightType = 2;
                } else if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.SPIKE;
                else if (player.fightType == 2)
                    player.weaponStyle = player.weaponStyle.IMPALE;
                else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType);
                break;
            case 4705:
                if (player.fightType == 3 && (player.weaponStyle == player.weaponStyle.DEFLECT
                        || player.weaponStyle == player.weaponStyle.BLOCK_THREE || player.weaponStyle == player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.SMASH;
                    player.fightType = 2;
                } else if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.CHOP;
                else if (player.fightType == 2)
                    player.weaponStyle = player.weaponStyle.SLASH;
                else
                    player.weaponStyle = player.weaponStyle.BLOCK;
                player.varbit(43, player.fightType == 1 ? 3 : player.fightType);
                break;
            case 328: // Staff interface
                player.varbit(108, player.autocast_spellIndex < 0 ? 0 : 3); //TODO: Fix autocast for defensive trigger!
                if (player.fightType == 0)
                    player.weaponStyle = player.weaponStyle.POUND;
                else if (player.fightType == 2 || (player.fightType == 3 && player.weaponStyle != player.weaponStyle.DEFLECT
                        && player.weaponStyle != player.weaponStyle.BLOCK_THREE && player.weaponStyle != player.weaponStyle.LONGRANGE)) {
                    player.weaponStyle = player.weaponStyle.PUMMEL;
                    player.fightType = 2;
                } else {
                    player.weaponStyle = player.weaponStyle.BLOCK;
                    player.fightType = 1;
                }
                player.varbit(43, player.fightType == 2 ? 1 : player.fightType == 1 ? 2 : player.fightType);
                break;
            default:
                player.send(new SendMessage("Unhandled interface style!"));
        }
    }
}
