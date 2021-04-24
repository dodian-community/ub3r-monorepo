package net.dodian.uber.game.model.player.skills.prayer;

/**
 * @author Dashboard
 */
public enum Bones {

  BONES(526, 45), 
  BIG_BONES(532, 150), 
  JOGRE_BONES(3125, 300), 
  DRAGON_BONES(536, 720), 
  OURG_BONES(4834, 920),
  DAGANNOTH_BONES(6729, 1200)
  ;

  private int itemId, experience;

  Bones(int itemId, int experience) {
    this.itemId = itemId;
    this.experience = experience;
  }

  public int getItemId() {
    return this.itemId;
  }

  public int getExperience() {
    return this.experience;
  }

  public static Bones getBone(int itemId) {
    for (Bones bone : values()) {
      if (bone.getItemId() == itemId) {
        return bone;
      }
    }
    return null;
  }

}
