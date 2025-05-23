package net.dodian.uber.game.model.combat.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.combat.WeaponData;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;

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
                    int itemOnInterfaceId = interfaceId + 1;
                    int textOnInterfaceId = itemName.equalsIgnoreCase("unarmed") ? interfaceId + 2 : interfaceId + 3;
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
}
