package net.dodian.uber.game.model.combat.impl.styles;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;

public class WhipHandler extends CombatStyleHandler {

    @Override
    public void handleWeaponInterface(Entity entity, int buttonId) {
        Client client = (Client) entity;
        if (buttonId == -1) {
            switch (client.getCombatStyle()) {
                case ACCURATE_MELEE:
                case ACCURATE_RANGED:
                    client.setCombatStyle(CombatStyles.ACCURATE_MELEE);
                    client.varbit(43, 0);
                    break;
                case CONTROLLED_MELEE:
                case AGGRESSIVE_MELEE:
                case RAPID_RANGED:
                    client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
                    client.varbit(43, 1);
                    break;
                case DEFENSIVE_MELEE:
                case LONGRANGE_RANGED:
                    client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
                    client.varbit(43, 2);
                    break;
            }
        }
        switch (buttonId) {
            case 48010:
                client.setCombatStyle(CombatStyles.ACCURATE_MELEE);
                break;
            case 48009:
                client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
                break;
            case 48008:
                client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
                break;
        }
    }

}
