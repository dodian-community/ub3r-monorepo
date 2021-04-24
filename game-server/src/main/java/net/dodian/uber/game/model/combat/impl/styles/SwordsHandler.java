package net.dodian.uber.game.model.combat.impl.styles;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;

public class SwordsHandler extends CombatStyleHandler {

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
        client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
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
    case 8234:
      client.setCombatStyle(CombatStyles.ACCURATE_MELEE);
      break;
    case 8237:
    case 8236:
      client.setCombatStyle(CombatStyles.AGGRESSIVE_MELEE);
      break;
    case 8235:
      client.setCombatStyle(CombatStyles.DEFENSIVE_MELEE);
      break;
    }
  }

}
