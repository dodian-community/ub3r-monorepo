package net.dodian.uber.game.model.item;

import java.sql.ResultSet;

public class Item {

  private int id;
  private int slot;
  private int standAnim;
  private int walkAnim;
  private int runAnim;
  private int attackAnim = 0;
  private int shopSellValue = 1, shopBuyValue = 0;
  private int[] bonuses = new int[12];
  private boolean stackable = false;
  private boolean noteable = false;
  private boolean tradeable = true;
  private boolean twoHanded = false;
  protected boolean full = false, mask = false;
  private boolean premium = false;
  private String name = "Unnamed Item";
  private String description = "No Description";
  private int Alchemy;

  public Item(ResultSet row) {
    try {
      id = row.getInt("id");
      description = row.getString("description");
      slot = row.getInt("slot");
      noteable = row.getBoolean("noteable");
      tradeable = row.getBoolean("tradeable");
      twoHanded = row.getBoolean("twohanded");
      full = row.getInt("full") == 1 == true;
      mask = row.getInt("full") == 2 == true;
      stackable = row.getBoolean("stackable");
      name = row.getString("name").replace("_", " ");
      for (int i = 0; i < bonuses.length; i++) {
        bonuses[i] = row.getInt("bonus" + (i + 1));
      }
      shopSellValue = row.getInt("shopSellValue");
      shopBuyValue = row.getInt("shopBuyValue");
      Alchemy = row.getInt("Alchemy");

      standAnim = row.getInt("standAnim");
      walkAnim = row.getInt("walkAnim");
      runAnim = row.getInt("runAnim");
      attackAnim = row.getInt("attackAnim");
      if (attackAnim == 806)
        attackAnim = 451;
      premium = row.getInt("premium") == 1;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public int getSlot() {
    return this.slot;
  }

  public boolean getStackable() {
    return this.stackable;
  }

  public boolean getTradeable() {
    return this.tradeable;
  }

  public boolean getNoteable() {
    return this.noteable;
  }

  public int getShopSellValue() {
    return this.shopSellValue;
  }

  public int getShopBuyValue() {
    return this.shopBuyValue;
  }

  public int getAlchemy() {
    return this.Alchemy;
  }

  public int getStandAnim() {
    return this.standAnim;
  }

  public int getWalkAnim() {
    return this.walkAnim;
  }

  public int getRunAnim() {
    return this.runAnim;
  }

  public int getAttackAnim() {
    return this.attackAnim;
  }

  public boolean getPremium() {
    return this.premium;
  }

  public boolean getTwoHanded() {
    return this.twoHanded;
  }

  public boolean getFull() {
    return this.full;
  }

  public int[] getBonuses() {
    return this.bonuses;
  }

  public String toString() {
    return name + " (" + id + "); slot " + slot + "; standAnim" + standAnim + "; walkAnim " + walkAnim + "; runAnim "
        + runAnim + "; attackAnim " + attackAnim;
  }

}