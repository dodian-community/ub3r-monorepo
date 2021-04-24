package net.dodian.uber.game.model.player.quests;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;

public enum QuestSend {
  JUST_STARTED(0, 7332, 10, "Hello noob!"), THE_LOST_HAT(1, 7333, 10, ""), EMPTY_2(2, 7334, 10,
      ""), EMPTY_3(3, 7336, 10, ""), EMPTY_4(4, 7383, 10, ""), EMPTY_5(5, 7339, 10, ""), EMPTY_6(6, 7338, 10,
          ""), EMPTY_7(7, 7340, 10, ""), EMPTY_8(8, 7346, 10, ""), EMPTY_9(9, 7341, 10, ""), EMPTY_10(10, 7342, 10,
              ""), EMPTY_11(11, 7337, 10, ""), EMPTY_12(12, 7343, 10, ""), EMPTY_13(13, 7335, 10, ""), EMPTY_14(14,
                  7344, 10, ""), EMPTY_15(15, 7345, 10, ""), EMPTY_16(16, 7347, 10, ""), EMPTY_17(17, 7348, 10, "");

  private int id;
  private int config;
  private int end;
  private String name;

  QuestSend(int id, int config, int end, String name) {
    this.id = id;
    this.config = config;
    this.end = end;
    this.name = name;
  }

  public int getId() {
    return this.id;
  }

  public int getConfig() {
    return this.config;
  }

  public int getEnd() {
    return this.end;
  }

  public String getName() {
    return this.name;
  }

  public static QuestSend questInterface(Client c) {
    c.send(new SendString("Dodian Quests", 640));
    c.send(new SendString("", 663));
    // c.send(new SendString("", 673)); Still need members quest interface id
    for (QuestSend quest : values()) {
      if (c.quests[quest.getId()] == 0)
        c.send(new SendString("@red@" + quest.getName(), quest.getConfig()));
      if (c.quests[quest.getId()] > 0 && c.quests[quest.getId()] < quest.getEnd())
        c.send(new SendString("@yel@" + quest.getName(), quest.getConfig()));
      if (c.quests[quest.getId()] == quest.getEnd())
        c.send(new SendString("@gre@" + quest.getName(), quest.getConfig()));
    }
    return null;
  }

}
