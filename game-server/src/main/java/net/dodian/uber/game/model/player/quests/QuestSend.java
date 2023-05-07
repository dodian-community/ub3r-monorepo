package net.dodian.uber.game.model.player.quests;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.skills.Skill;

public enum QuestSend {
    PLAGUE_DOCKS(0, 7332, 28164,5,  "Mysterium of the Docks"), EMPTY_1(1, 7333, -1, 5, ""), EMPTY_2(2, 7334, -1, 10,
            ""), EMPTY_3(3, 7336, -1, 10, ""), EMPTY_4(4, 7383, -1, 10, ""), EMPTY_5(5, 7339, -1, 10, ""), EMPTY_6(6, 7338, -1, 10,
            ""), EMPTY_7(7, 7340, -1, 10, ""), EMPTY_8(8, 7346, -1, 10, ""), EMPTY_9(9, 7341, -1, 10, ""), EMPTY_10(10, 7342, -1, 10,
            ""), EMPTY_11(11, 7337, -1, 10, ""), EMPTY_12(12, 7343, -1, 10, ""), EMPTY_13(13, 7335, -1, 10, ""), EMPTY_14(14,
            7344, -1, 10, ""), EMPTY_15(15, 7345, -1, 10, ""), EMPTY_16(16, 7347, -1, 10, ""), EMPTY_17(17, 7348, -1, 10, "");

    private int id, config, clickId, end;
    private String name;

    QuestSend(int id, int config, int clickId, int end, String name) {
        this.id = id;
        this.config = config;
        this.clickId = clickId;
        this.end = end;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }
    public int getConfig() {
        return this.config;
    }
    public int getClickId() {
        return this.clickId;
    }
    public int getEnd() {
        return this.end;
    }
    public static QuestSend getSender(int button) {
        for (QuestSend quest : values()) {
            if(quest.getClickId() == button)
                return quest;
        }
        return null;
    }

    public String getName() {
        return this.name;
    }

    public static QuestSend questInterface(Client c) {
        c.send(new SendString("Dodian Quests", 640));
        c.send(new SendString("Premium", 663));
        c.send(new SendString("Other Stuff", 682));
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

    public static boolean questMenu(Client c) {
         QuestSend quest = getSender(c.actionButtonId);
         if(quest == null) return false;
         c.clearQuestInterface();
         if(quest.getId() == 0) {
             int stage = c.quests[quest.getId()];
             c.send(new SendString("@dre@"+quest.getName(), 8144));
             if(stage == 0) {
                 c.send(new SendString("I can start this quest by talking to the monk in Yanille", 8147));
                 c.send(new SendString("", 8148));
                 c.send(new SendString("", 8149));
                 c.send(new SendString("I need to have the following levels:", 8150));
                 c.send(new SendString(c.getLevel(Skill.HERBLORE) >= 15 ? "@str@15 herblore@str@" : "15 herblore", 8151)); //@str@ put a red line!
                 c.send(new SendString(c.getLevel(Skill.SMITHING) >= 20 ? "@str@20 smithing@str@" : "20 smithing", 8152)); //@str@ put a red line!
                 c.send(new SendString(c.getLevel(Skill.CRAFTING) >= 40 ? "@str@40 crafting@str@" : "40 crafting", 8153)); //@str@ put a red line!
             } else if (stage > 0) {
                 c.send(new SendString("@str@I have talked to the monk@str@", 8147));
                 c.send(new SendString("The monk told me to talk to someone on the Brimhaven docks.", 8148));
                 c.send(new SendString("I wonder who that could be.", 8149));
             }
             c.sendQuestSomething(8143);
             c.showInterface(8134);
             //c.flushOutStream();
             return true;
         }
         return false;
    }

}
