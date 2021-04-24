package net.dodian.uber.game.model.combat.impl.styles;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;

public class MaceHandler extends CombatStyleHandler {

  @Override
  public void handleWeaponInterface(Entity entity, int buttonId) {
    Client client = (Client) entity;

    if (buttonId == -1) {
      switch (client.getCombatStyle()) {
      case ACCURATE_MELEE:
      case ACCURATE_RANGED:
        client.setCombatStyle(CombatStyles.ACCURATE_MELEE);
        client.frame87(43, 0);
        break;
      case AGGRESSIVE_MELEE:
      case RAPID_RANGED:
        client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
        client.frame87(43, 1);
        break;
      case CONTROLLED_MELEE:
        client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
        client.frame87(43, 2);
        break;
      case DEFENSIVE_MELEE:
      case LONGRANGE_RANGED:
        client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
        client.frame87(43, 3);
        break;
      }
    }

    switch (buttonId) {
    case 14218:
      client.setCombatStyle(CombatStyles.ACCURATE_MELEE);
      break;
    case 14221:
      client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
      break;
    case 14220:
      client.setCombatStyle(CombatStyles.CONTROLLED_MELEE);
      break;
    case 14219:
      client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
      break;
    }
  }

}
