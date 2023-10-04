package net.dodian.uber.game.model.combat.impl.styles;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;

public class HalberdHandler extends CombatStyleHandler {

    @Override
    public void handleWeaponInterface(Entity entity, int buttonId) {
        Client client = (Client) entity;

        if (buttonId == -1) {
            switch (client.getCombatStyle()) {
                case ACCURATE_MELEE:
                case ACCURATE_RANGED:
                case CONTROLLED_MELEE:
                    client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
                    client.frame87(43, 0);
                    break;
                case AGGRESSIVE_MELEE:
                case RAPID_RANGED:
                    client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
                    client.frame87(43, 1);
                    break;
                case DEFENSIVE_MELEE:
                case LONGRANGE_RANGED:
                    client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
                    client.frame87(43, 2);
                    break;
            }
        }

        switch (buttonId) {
            case 33018:
                client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
                break;
            case 33020:
                client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
                break;
            case 33019:
                client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
                break;
        }
    }

}
