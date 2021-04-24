package net.dodian.uber.game.model.player.skills.prayer;

public enum HeadIcon {

  NONE(-1), PROTECT_MELEE(0), PROTECT_MISSLES(1), PROTECT_MAGIC(2), RETRIBUTION(3), SMITE(4), REDEMPTION(5);

  /**
   * The headicon id
   */
  private int headIconId = -1;

  /**
   * Create a headicon with the specified id
   * 
   * @param headIconId
   *          The id to set
   */
  private HeadIcon(int headIconId) {
    this.headIconId = headIconId;
  }

  /**
   * Get the headicon's id
   * 
   * @return The id associated with the headicon
   */
  public int asInt() {
    return headIconId;
  }
}
