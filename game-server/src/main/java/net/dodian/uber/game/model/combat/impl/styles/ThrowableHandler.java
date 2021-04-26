package net.dodian.uber.game.model.combat.impl.styles;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;

public class ThrowableHandler extends CombatStyleHandler {

    @Override
    public void handleWeaponInterface(Entity entity, int buttonId) {
        Client client = (Client) entity;

        if (buttonId == -1) {
            switch (client.getCombatStyle()) {
                case ACCURATE_MELEE:
                case ACCURATE_RANGED:
                    client.setCombatStyle(CombatStyles.ACCURATE_RANGED);
                    client.frame87(43, 0);
                    break;
                case AGGRESSIVE_MELEE:
                case RAPID_RANGED:
                case CONTROLLED_MELEE:
                    client.setCombatStyle(CombatStyles.RAPID_RANGED);
                    client.frame87(43, 1);
                    break;
                case DEFENSIVE_MELEE:
                case LONGRANGE_RANGED:
                    client.setCombatStyle(CombatStyles.LONGRANGE_RANGED);
                    client.frame87(43, 2);
                    break;

            }
        }

        switch (buttonId) {
            case 17102:
                client.setCombatStyle(CombatStyles.ACCURATE_RANGED);
                break;
            case 17101:
                client.setCombatStyle(CombatStyles.RAPID_RANGED);
                break;
            case 17100:
                client.setCombatStyle(CombatStyles.LONGRANGE_RANGED);
                break;
        }
    }

}
