package net.dodian.uber.game.model.player.skills.herblore;

/**
 * 
 * @author Dashboard
 *
 */
public enum Herbs {

  GUAM("guam", 199, 249, 1, 25), MARRENTILL("marrentill", 201, 251, 5, 38), TARROMIN("tarromin", 203, 253, 11,
      50), HARRALANDER("harralander", 205, 255, 20, 63), RANARR_WEED("ranarr weed", 207, 257, 25,
          75), TOADFLAX("toadflax", 3049, 2998, 30, 80), IRIT("irit", 209, 259, 40, 88), AVANTOE("avantoe", 211, 261,
              48, 100), KWUARM("kwuarm", 213, 263, 54, 113), SNAPDRAGON("snapdragon", 3051, 3000, 59,
                  118), CADANTINE("cadantine", 215, 265, 65, 125), LANTADYME("lantadyme", 2485, 2481, 67,
                      131), DWARF_WEED("dwarf weed", 217, 267, 70, 138), TORSTOL("torstol", 219, 269, 75, 150);

  private String name;
  private int grimyId, cleanId, cleanLevel, experience;

  Herbs(String name, int grimyId, int cleanId, int cleanLevel, int experience) {
    this.name = name;
    this.grimyId = grimyId;
    this.cleanId = cleanId;
    this.cleanLevel = cleanLevel;
    this.experience = experience;
  }

  public String getName() {
    return this.name;
  }

  public int getGrimyId() {
    return this.grimyId;
  }

  public int getCleanId() {
    return this.cleanId;
  }

  public int getCleanLevel() {
    return this.cleanLevel;
  }

  public int getExperience() {
    return this.experience;
  }

  public static Herbs getGrimy(int id) {
    for (Herbs herb : values()) {
      if (herb.getGrimyId() == id) {
        return herb;
      }
    }
    return null;
  }

}
