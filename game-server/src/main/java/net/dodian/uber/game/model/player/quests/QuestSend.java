package net.dodian.uber.game.model.player.quests;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.network.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.skills.Skill;
import java.text.DecimalFormat;

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
    public static QuestSend serverInterface(Client c) {
        long uptimeMinutes = (System.currentTimeMillis() - Server.serverStartup) / 60000;
        String hourText = uptimeMinutes % 60 == 0 ? (uptimeMinutes/60) + "" : new DecimalFormat("0.000").format(uptimeMinutes/60D);
        c.send(new SendString("Dodian Server", 640));
        c.send(new SendString("", 682)); //7332
        c.send(new SendString("@gre@[@whi@"+(uptimeMinutes < 60 ? uptimeMinutes + "@gre@] minutes" : hourText+"@gre@] hours")+" uptime", 663));
        c.send(new SendString("@lre@Boss Log", 7332));
        c.send(new SendString("@lre@Monster Log", 7333));
        c.send(new SendString("@lre@Commands", 7334));
        c.send(new SendString("@lre@------------@dre@Links@lre@------------", 7336));
        c.send(new SendString("@gre@News", 7383));
        c.send(new SendString("@gre@Guides", 7339));
        c.send(new SendString("@gre@Account Services", 7338));
        c.send(new SendString("@gre@Discord", 7340));
        if(c.playerRights > 0) {
            c.send(new SendString("@lre@---------@dre@Moderator@lre@---------", 7346));
            c.send(new SendString("@gre@Game CP", 7341));
        }
        return null;
    }

    public static boolean questMenu(Client c, int button) {
        QuestSend quest = getSender(button);
         if(c.questPage == 0 && quest != null) {
             c.clearQuestInterface();
             if (quest.getId() == 0) {
                 int stage = c.quests[quest.getId()];
                 c.send(new SendString("@dre@" + quest.getName(), 8144));
                 if (stage == 0) {
                     c.send(new SendString("I can start this quest by talking to the monk in Yanille", 8147));
                     c.send(new SendString("\\nI need to have the following levels:", 8148));
                     c.send(new SendString("", 8149));
                     c.send(new SendString(c.getLevel(Skill.HERBLORE) >= 15 ? "@str@15 herblore@str@" : "15 herblore", 8150)); //@str@ put a red line!
                     c.send(new SendString(c.getLevel(Skill.SMITHING) >= 20 ? "@str@20 smithing@str@" : "20 smithing", 8151)); //@str@ put a red line!
                     c.send(new SendString(c.getLevel(Skill.CRAFTING) >= 40 ? "@str@40 crafting@str@" : "40 crafting", 8152)); //@str@ put a red line!
                 } else if (stage > 0) {
                     c.send(new SendString("@str@I have talked to the monk@str@", 8147));
                     c.send(new SendString("The monk said f' all and I am more confused...", 8148));
                     c.send(new SendString("I wonder why no quest in Dodian exist....", 8149));
                 }
                 c.sendQuestSomething(8143);
                 c.showInterface(8134);
                 return true;
             }
         } else {
             switch(button) {
                 case 28164: //Boss log
                     c.send(new SendString("@dre@Uber Server 3.0 - Boss Log", 8144));
                     c.clearQuestInterface();
                     int line = 8145;
                     for (int i = 0; i < c.boss_name.length; i++) {
                         if (c.boss_amount[i] < 100000)
                             c.send(new SendString(c.boss_name[i].replace("_", " ") + ": " + c.boss_amount[i], line));
                         else
                             c.send(new SendString(c.boss_name[i].replace("_", " ") + ": LOTS", line));
                         line++;
                         if (line == 8196)
                             line = 12174;
                         if (line == 8146)
                             line = 8147;
                     }
                     c.sendQuestSomething(8143);
                     c.showInterface(8134);
                 return true;
                 case 28165: //Monster Log
                     c.send(new SendString("@dre@Uber Server 3.0 - Monster Log", 8144));
                     c.clearQuestInterface();
                     line = 8145;
                     for (int i = 0; i < (c.monsterName.size() >= 100 ? 100 : c.monsterName.size()); i++) { //Only 100 entries for now!!
                         int amount = c.monsterCount.get(i);
                         String newName = c.monsterName.get(i).substring(0, 1).toUpperCase() + c.monsterName.get(i).substring(1).replaceAll("_", " ");
                         c.send(new SendString(newName + ": " + (amount == 1048576 ? "LOTS" : "" + amount), line));
                         line++;
                         if (line == 8196)
                             line = 12174;
                         if (line == 8146)
                             line = 8147;
                     }
                     c.sendQuestSomething(8143);
                     c.showInterface(8134);
                     return true;
                 case 28215: //News
                     Player.openPage(c, "https://dodian.net/showthread.php?t="+c.latestNews);
                     return true;
                 case 28171: //Guides
                     Player.openPage(c, "https://dodian.net/forumdisplay.php?f=22");
                     return true;
                 case 28166: //Commands
                     c.send(new SendString("@dre@Uber Server 3.0 - Commands", 8144));
                     c.clearQuestInterface();
                     line = 8145;
                     String[] commands = {"::players or /players \\nSee which player is online.", "",
                             "::droplist, ::drops, /droplist or /drops \\nOpen up the droplist.", "",
                             "::bug or /bug \\nTake you to the forum to post a bug.", "",
                             "::suggest or /suggest \\nTake you to the forum to post a suggestion.", "",
                             "::request or /request \\nTake you to the forum to post a request to a staff.", "",
                             "::report or /report \\nTake you to the forum to post a report to a staff.", "",
                             "::rules or /rules \\nOpen up the rules for the server.", "",
                             "::news or /news \\nOpen up the latest news thread.", "",
                             "::thread id or /thread id \\nOpen up a thread with the id.", "",
                             "::highscore name_One name_Two \\nOpen up the highscore with either one name or compare two.", "",
                             "::price itemname or /price itemname \\nLook up a item's various values.", "",
                             "::max or /max \\nFigure out your maximum combat damage.", "",
                             "::yell msg, /yell msg or //msg \\nWill yell a message to people online.", "",
                             c.playerRights > 0 ? "-------------------Moderator commands ---------------------" : "",
                             c.playerRights > 0 ? "::kick username - Will kick a player with a username." : "",
                             c.playerRights > 0 ? "::toggleyell or ::toggle_yell - Will toggle yell on/off." : "",
                             c.playerRights > 0 ? "::toggletrade or ::toggle_trade - Will toggle trade on/off." : "",
                             c.playerRights > 0 ? "::toggleduel or ::toggle_duel - Will toggle trade on/off." : "",
                             c.playerRights > 0 ? "::togglepvp or ::toggle_pvp - Will toggle pvp on/off." : "",
                             c.playerRights > 0 ? "::toggledrop - Will toggle dropping of items on/off." : "",
                             c.playerRights > 0 ? "::toggleshop or ::toggle_shop - Will toggle shops on/off." : "",
                             c.playerRights > 0 ? "::togglebank or ::toggle_bank - Will toggle banking on/off." : "",
                             c.playerRights > 0 ? "::checkbank username - Will check the bank of username." : "",
                             c.playerRights > 0 ? "::checkinv username - Will check the inventory of username." : "",
                             c.playerRights > 0 ? "::mod msg - Sends a message to all online staff." : ""
                     };
                     for (int i = 0; i < commands.length; i++) {
                         c.send(new SendString(commands[i], line));
                         line++;
                         if (line == 8196)
                             line = 12174;
                         if (line == 8146)
                             line = 8147;
                     }
                     c.sendQuestSomething(8143);
                     c.showInterface(8134);
                     return true;
                 case 28170: //Account service
                     c.openPage(c, "https://dodian.net/forumdisplay.php?f=83");
                     return true;
                 case 28172: //Discord
                     c.discord = true;
                     c.showPlayerOption(new String[]{"Are you sure you wish to open discord invite?", "Yes", "No"});
                 return true;
                 case 28173:
                     Player.openPage(c, "https://dodian.net/index.php?pageid=modcp");
                 return true;
             }
         }
        return false;
    }

    public static void showMonsterLog(Client c) {
        boolean wasPage = c.questPage == 0;
        if(wasPage) {
            c.questPage = 1;
            QuestSend.questMenu(c, 28165);
            if(wasPage) c.questPage = 0;
        } else QuestSend.questMenu(c, 28165);
    }

    public static void clearQuestName(Client c) {
        for (QuestSend questClear : values()) { //Clear quest!
            c.send(new SendString("", questClear.getConfig()));
        }
    }

}
