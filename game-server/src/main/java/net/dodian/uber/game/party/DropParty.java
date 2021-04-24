package net.dodian.uber.game.party;

import java.util.ArrayList;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;

/**
 * 
 * @author Dashboard
 *
 */
public class DropParty implements Runnable {

  private int minutes;
  private long startTime;
  private boolean started;
  ArrayList<GameItem> dropItems;

  public DropParty(int minutes) {
    this.setMinutes(minutes);
    this.startTime = System.currentTimeMillis() + (minutes * 60000);
    Client.publicyell("<col=FF0000>[S] There will be a drop party @ Ardougne in aproximately " + minutes + " minutes.");
    dropItems = DropPartyItems.getDropPartyItems();
  }

  @Override
  public void run() {
    while (!dropItems.isEmpty()) {

      if (System.currentTimeMillis() < startTime) {
        try {
          Thread.sleep(1000);
          continue;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      if (!started) {
        Client.publicyell("<col=FF0000>[S] The drop party in Ardougne has now started!");
        started = true;
      }

      GameItem dropItem = dropItems.remove((int) (Math.random() * dropItems.size()));

      int dropX = 2658 + (int) (Math.random() * 9);
      int dropY = 3299 + (int) (Math.random() * 15);
      Ground.items.add(new GroundItem(dropX, dropY, dropItem.getId(), dropItem.getAmount(), -1, -1));

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    try {
      Thread.sleep(180000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Client.publicyell("<col=FF0000>[S] The drop party has now ended.");
  }

  public int getMinutes() {
	return minutes;
}

public void setMinutes(int minutes) {
	this.minutes = minutes;
}

enum DropPartyItems {

    FANCY_BOOTS(9005, 1, 1), DRAGON_MED_HELM(1149, 1, 3), NATURE_RUNES(561, 250, 3), BLOOD_RUNE(565, 200,
        2), MAGIC_LOGS(1514, 100, 1), RUNE_FULL_HELM(1164, 2, 2), UNCUT_DIAMONDS(1618, 25, 2), AMULET_OF_POWER(1731, 1,
            8), RUNE_SCIMITAR(1333, 1, 4), COAL(454, 100, 5), ADAMANT_BOOTS(4129, 1, 5), DRAGON_BONES(537, 45,
                2), IRON_BARS(2352, 50, 4), CASH(995, 175000, 20);

    private int id, amount, occurances;

    DropPartyItems(int id, int amount, int occurances) {
      this.id = id;
      this.amount = amount;
      this.occurances = occurances;
    }

    public int getId() {
      return this.id;
    }

    public int getAmount() {
      return this.amount;
    }

    public int getOccurances() {
      return this.occurances;
    }

    public static ArrayList<GameItem> getDropPartyItems() {
      ArrayList<GameItem> items = new ArrayList<GameItem>();
      for (DropPartyItems item : values()) {
        for (int i = 0; i < item.getOccurances(); i++) {
          items.add(new GameItem(item.getId(), item.getAmount()));
        }
      }
      return items;
    }

  }

}
