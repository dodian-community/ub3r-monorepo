package net.dodian.uber.game.model.player.skills.herblore;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;

/**
 * 
 * @author Dashboard
 *
 */
public class Herblore {

  public static boolean cleanHerb(Client client, int herbId, int herbSlot) {
    Herbs herb = Herbs.getGrimy(herbId);
    if (herb == null) {
      return false;
    }
    if (client.getLevel(Skill.HERBLORE) < herb.getCleanLevel()) {
      client.send(new SendMessage("You need a herblore level of " + herb.getCleanLevel() + " to identify this herb."));
      return true;
    }

    client.giveExperience(herb.getExperience(), Skill.HERBLORE);
    client.deleteItem(herbId, herbSlot, 1);
    client.addItemSlot(herb.getCleanId(), 1, herbSlot);
    client.send(new SendMessage("You identify the herb, it's a " + herb.getName() + "."));
    return true;
  }

}
