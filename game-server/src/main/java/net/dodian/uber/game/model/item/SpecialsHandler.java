package net.dodian.uber.game.model.item;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.utilities.Utils;

public class SpecialsHandler {

    public static void specAction(Client player, int weapon, int dmg) {
        if (weapon == 4151) {
            player.bonusSpec = 1 + Utils.random(5);
            player.emoteSpec = player.getStandAnim();
            player.animationSpec = 341;
        } else if (weapon == 7158) {
            player.bonusSpec = 1 + Utils.random(5);

            if (player.getCurrentHealth() + player.bonusSpec < 99) {
                player.setCurrentHealth(player.getCurrentHealth() + player.bonusSpec);
            } else {
                player.setCurrentHealth(player.maxHealth);
            }
            int heal = player.bonusSpec + ((player.bonusSpec + dmg) / 4);
            player.setCurrentHealth(player.getCurrentHealth() + heal > player.maxHealth ? player.maxHealth : player.getCurrentHealth() + heal);
            player.emoteSpec = 2890;
            player.animationSpec = 377;
        }
    }
}
