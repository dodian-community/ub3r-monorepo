package net.dodian.uber.game.model.player.skills;

public class Skills {
  
  public static int getLevelForExperience(int experience) {
    int points = 0;
    int output = 0;
    if (experience > 13034430) {
      return 99;
    }
    for (int lvl = 1; lvl <= 99; lvl++) {
      points += Math.floor((double) lvl + 300.0 * Math.pow(2.0, (double) lvl / 7.0));
      output = (int) Math.floor(points / 4);
      if (output >= experience) {
        return lvl;
      }
    }
    return 0;
  }

}
