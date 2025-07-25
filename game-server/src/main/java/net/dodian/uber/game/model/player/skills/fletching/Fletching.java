package net.dodian.uber.game.model.player.skills.fletching;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;

public class Fletching {

    public void fletchBow(Client player, boolean shortBow, int amount) {
        player.send(new RemoveInterfaces());
        if (shortBow) {
            if (player.getLevel(Skill.FLETCHING) < Constants.shortreq[player.fletchLog]) {
                player.send(new SendMessage("Requires fletching " + Constants.shortreq[player.fletchLog] + "!"));
                player.resetAction();
                return;
            }
            player.fletchId = Constants.shortbows[player.fletchLog];
            player.fletchExp = Constants.shortexp[player.fletchLog];
        } else {
            if (player.getLevel(Skill.FLETCHING) < Constants.longreq[player.fletchLog]) {
                player.send(new SendMessage("Requires fletching " + Constants.longreq[player.fletchLog] + "!"));
                player.resetAction();
                return;
            }
            player.fletchId = Constants.longbows[player.fletchLog];
            player.fletchExp = Constants.longexp[player.fletchLog];
        }
        player.fletchings = true;
        player.fletchAmount = amount;
    }

    public void fletchBow(Client player) {
        if (player.fletchAmount < 1) {
            player.resetAction();
            return;
        }
        if (player.isBusy()) {
            player.send(new SendMessage("You are currently busy to be fletching!"));
            return;
        }
        player.fletchAmount--;
        player.send(new RemoveInterfaces());
        player.IsBanking = false;
        player.requestAnim(4433, 0);
        if (player.playerHasItem(Constants.logs[player.fletchLog])) {
            player.deleteItem(Constants.logs[player.fletchLog], 1);
            player.addItem(player.fletchId, 1);
            player.checkItemUpdate();
            player.giveExperience(player.fletchExp, Skill.FLETCHING);
            player.triggerRandom(player.fletchExp);
        } else {
            player.resetAction();
        }
    }

}
