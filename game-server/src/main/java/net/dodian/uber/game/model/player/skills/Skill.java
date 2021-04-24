package net.dodian.uber.game.model.player.skills;

/**
 * 
 * @author Dashboard
 *
 */
public enum Skill {

  ATTACK(0, "attack"), 
  DEFENCE(1, "defence"), 
  STRENGTH(2, "strength"), 
  HITPOINTS(3, "hitpoints"), 
  RANGED(4, "ranged"), 
  PRAYER(5, "prayer"), 
  MAGIC(6, "magic"), 
  COOKING(7, "cooking"), 
  WOODCUTTING(8, "woodcutting"), 
  FLETCHING(9, "fletching"), 
  FISHING(10, "fishing"), 
  FIREMAKING(11, "firemaking"),
  CRAFTING(12, "crafting"), 
  SMITHING(13, "smithing"), 
  MINING(14, "mining"), 
  HERBLORE(15, "herblore"), 
  AGILITY(16, "agility"), 
  THIEVING(17, "thieving"), 
  SLAYER(18, "slayer"), 
  FARMING(19, "farming"), 
  RUNECRAFTING(20, "runecrafting");

  private int id;
  private String name;

  Skill(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static Skill getSkill(int id) {
    for (Skill skill : values()) {
      if (skill.getId() == id)
        return skill;
    }
    return null;
  }

}
