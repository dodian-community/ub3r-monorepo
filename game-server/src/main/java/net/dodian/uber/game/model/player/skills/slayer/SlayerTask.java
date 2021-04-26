package net.dodian.uber.game.model.player.skills.slayer;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Range;

import java.util.ArrayList;

public class SlayerTask {

    public enum slayerTasks {
        //Regular npcs
        CRAWLING_HAND("Crawling Hands", true, new Range(1, 40), new Range(20, 100), 448, 449),
        PYREFIENDS("Pyrefiends", true, new Range(20, 60), new Range(20, 60), 433),
        DEATH_SPAWN("Death Spawns", true, new Range(30, 60), new Range(20, 60), 10),
        JELLY("Jellies", true, new Range(40, 75), new Range(30, 70), 437),
        HEAD_MOURNER("Head Mourners", true, new Range(45, 200), new Range(10, 30), 5311),
        HILL_GIANT("Hill Giants", false, new Range(1, 50), new Range(20, 40), 2098),
        CHAOS_DWARF("Chaos Dwarves", true, new Range(50, 200), new Range(20, 40), 291),
        LESSER_DEMON("Lesser Demon", true, new Range(50, 200), new Range(30, 80), 2005),
        FIRE_GIANTS("Fire Giants", false, new Range(1, 200), new Range(30, 80), 2075),
        MUMMY("Mummy", true, new Range(1, 200), new Range(30, 80), 950),
        ICE_GIANT("Ice Giants", false, new Range(30, 60), new Range(30, 50), 2085),
        DRUID("Druids", false, new Range(1, 30), new Range(20, 35), 3098),
        GREATER_DEMON("Greater Demon", true, new Range(55, 200), new Range(30, 60), 2025),
        BERSERK_BARBARIAN_SPIRIT("Berserk Barbarian Spirits", true, new Range(70, 200), new Range(25, 60), 5565),
        MITHRIL_DRAGON("Mithril Dragons", true, new Range(83, 200), new Range(15, 40), 2919),
        BLOODVELD("Bloodveld", true, new Range(53, 93), new Range(25, 60), 484),
        GARGOYLES("Gargoyles", true, new Range(63, 200), new Range(25, 60), 412),
        ABERRANT_SPECTRE("Aberrant spectre", true, new Range(73, 200), new Range(25, 60), 2),
        SKELE_HELLHOUNDS("Skeleton HellHound", true, new Range(50, 200), new Range(30, 80), 5054),
        TZHAAR("TzHaar-Ket", true, new Range(80, 200), new Range(30, 60), 2173),
        ABYSSAL_DEMONS("Abyssal Demons", true, new Range(85, 200), new Range(30, 60), 415),
        GREEN_DRAGONS("Green Dragons", false, new Range(50, 200), new Range(30, 60), 260),
        BLUE_DRAGONS("Blue Dragons", false, new Range(50, 200), new Range(30, 60), 265),
        //Boss npcs
        DAD("Dad", false, new Range(1, 200), new Range(10, 30), 4130),
        SAN_TOJALON("San Tojalon", false, new Range(1, 200), new Range(10, 30), 3964),
        BLACK_KNIGHT_TITAN("Black knight titan", false, new Range(1, 200), new Range(10, 30), 4067),
        JUNGLE_DEMON("Jungle demon", false, new Range(1, 200), new Range(10, 30), 1443),
        BLACK_DEMON("Black Demon", true, new Range(60, 200), new Range(10, 30), 1432),
        DAGANNOTH_PRIME("Dagannoth prime", false, new Range(90, 200), new Range(10, 30), 2266),
        UNGADULU("Ungadulu", false, new Range(1, 200), new Range(10, 30), 3957),
        ICE_QUEEN("Ice queen", false, new Range(1, 200), new Range(10, 30), 4922),
        NECHRYAEL("Nechryael", false, new Range(1, 200), new Range(10, 30), 8),
        KING_BLACK_DRAGON("King black dragon", false, new Range(1, 200), new Range(10, 30), 239),
        ABYSSAL_GUARDIAN("Abyssal guardian", false, new Range(1, 200), new Range(10, 30), 2585),
        //Other npc's that got added late
        //DEFAULT("Default", false, new Range(1337, 4200), new Range(10, 30), 2264)
        ;

        private String textRepresentation;
        private boolean slayerOnly;
        private Range levelAssign;
        private Range taskAmount;
        private int[] npcId;

        //Name, slayerOnly, levelRange, amtRange, npcId[]

        slayerTasks(String textRepresentation, boolean slayerOnly, Range levelAssign, Range taskAmount, int... npcId) {
            this.textRepresentation = textRepresentation;
            this.slayerOnly = slayerOnly;
            this.levelAssign = levelAssign;
            this.taskAmount = taskAmount;
            this.npcId = npcId;
        }

        public int[] getNpcId() {
            return this.npcId;
        }

        public String getTextRepresentation() {
            return this.textRepresentation;
        }

        public Range getAssignedAmountRange() {
            return this.taskAmount;
        }

        public Range getAssignedLevelRange() {
            return this.levelAssign;
        }

        public boolean isSlayerOnly() {
            return this.slayerOnly;
        }

        public static slayerTasks getSlayerNpc(int npcId) {
            for (slayerTasks task : values()) {
                for (int i = 0; i < task.getNpcId().length; i++)
                    if (task.getNpcId()[i] == npcId)
                        return task;
            }
            return null;
        }

        public static slayerTasks getTask(int slot) {
            return slot < 0 || slot >= slayerTasks.values().length ? null : slayerTasks.values()[slot];
        }

        public static SlayerTask hasTask(int slot) {
            return null;
        }

        public static boolean isSlayerNpc(int npcId) {
            return getSlayerNpc(npcId) != null;
        }

    }

    private static slayerTasks[] mazchna = {
            slayerTasks.CRAWLING_HAND, slayerTasks.PYREFIENDS, slayerTasks.DEATH_SPAWN,
            slayerTasks.JELLY, slayerTasks.HEAD_MOURNER, slayerTasks.HILL_GIANT,
            slayerTasks.CHAOS_DWARF, slayerTasks.LESSER_DEMON, slayerTasks.ICE_GIANT,
            slayerTasks.BERSERK_BARBARIAN_SPIRIT, slayerTasks.MITHRIL_DRAGON, slayerTasks.SKELE_HELLHOUNDS,
            slayerTasks.FIRE_GIANTS, slayerTasks.BLOODVELD
    };

    private static slayerTasks[] vannaka = {
            slayerTasks.GREATER_DEMON, slayerTasks.BLACK_DEMON, slayerTasks.BERSERK_BARBARIAN_SPIRIT,
            slayerTasks.MITHRIL_DRAGON, slayerTasks.TZHAAR, slayerTasks.MUMMY,
            slayerTasks.ABYSSAL_DEMONS, slayerTasks.GREEN_DRAGONS, slayerTasks.BLUE_DRAGONS,
            slayerTasks.GARGOYLES, slayerTasks.BLOODVELD, slayerTasks.ABERRANT_SPECTRE
    };

    private static slayerTasks[] duradel = {
            slayerTasks.DAD, slayerTasks.SAN_TOJALON, slayerTasks.BLACK_KNIGHT_TITAN,
            slayerTasks.JUNGLE_DEMON, slayerTasks.BLACK_DEMON, slayerTasks.UNGADULU,
            slayerTasks.ICE_QUEEN, slayerTasks.NECHRYAEL, slayerTasks.KING_BLACK_DRAGON,
            slayerTasks.DAGANNOTH_PRIME, slayerTasks.HEAD_MOURNER, slayerTasks.ABYSSAL_GUARDIAN
    };

    public static ArrayList<slayerTasks> mazchnaTasks(Client c) {
        ArrayList<slayerTasks> slayer = new ArrayList<slayerTasks>();
        for (int i = 0; i < mazchna.length; i++) {
            int slayerLevel = c.getLevel(Skill.SLAYER);
            if (c.getSlayerData().get(1) != -1 && c.getSlayerData().get(1) == mazchna[i].ordinal()) {
                i++; //Skip task we already have got before!
            } else if (mazchna[i].getAssignedLevelRange().getFloor() <= slayerLevel
                    && slayerLevel <= mazchna[i].getAssignedLevelRange().getCeiling()) {
                if (mazchna[i] == slayerTasks.LESSER_DEMON && !c.checkItem(2383) && !c.checkItem(989)) {
                    slayer.add(mazchna[i]);
                    slayer.add(mazchna[i]);
                } else if (mazchna[i] == slayerTasks.SKELE_HELLHOUNDS && !c.checkItem(2382) && !c.checkItem(989)) {
                    slayer.add(mazchna[i]);
                    slayer.add(mazchna[i]);
                } else if (mazchna[i] == slayerTasks.FIRE_GIANTS) {
                    if (c.checkItem(1543))
                        slayer.add(mazchna[i]);
                } else
                    slayer.add(mazchna[i]);
            }
        }
        return slayer;
    }

    public static ArrayList<slayerTasks> vannakaTasks(Client c) {
        ArrayList<slayerTasks> slayer = new ArrayList<slayerTasks>();
        for (int i = 0; i < vannaka.length; i++) {
            int slayerLevel = c.getLevel(Skill.SLAYER);
            if (c.getSlayerData().get(1) != -1 && c.getSlayerData().get(1) == vannaka[i].ordinal()) {
                i++; //Skip task we already have got before!
            } else if (vannaka[i].getAssignedLevelRange().getFloor() <= slayerLevel
                    && slayerLevel <= vannaka[i].getAssignedLevelRange().getCeiling()) {
                if (vannaka[i] == slayerTasks.MUMMY) {
                    if (c.checkItem(1544))
                        slayer.add(vannaka[i]);
                } else
                    slayer.add(vannaka[i]);
            }
        }
        return slayer;
    }

    public static ArrayList<slayerTasks> duradelTasks(Client c) {
        ArrayList<slayerTasks> slayer = new ArrayList<slayerTasks>();
        for (int i = 0; i < duradel.length; i++) {
            int slayerLevel = c.getLevel(Skill.SLAYER);
            if (c.getSlayerData().get(1) != -1 && c.getSlayerData().get(1) == duradel[i].ordinal()) {
                i++; //Skip task we already have got before!
            } else if (duradel[i].getAssignedLevelRange().getFloor() <= slayerLevel
                    && slayerLevel <= duradel[i].getAssignedLevelRange().getCeiling()) {
                if (duradel[i] == slayerTasks.SAN_TOJALON) {
                    if (c.checkItem(1544))
                        slayer.add(duradel[i]);
                } else if (duradel[i] == slayerTasks.BLACK_KNIGHT_TITAN) {
                    if (c.checkItem(1544))
                        slayer.add(duradel[i]);
                } else if (duradel[i] == slayerTasks.JUNGLE_DEMON) {
                    if (c.checkItem(1545))
                        slayer.add(duradel[i]);
                } else if (duradel[i] == slayerTasks.BLACK_DEMON) {
                    if (c.checkItem(989))
                        slayer.add(duradel[i]);
                } else
                    slayer.add(duradel[i]);
            }
        }
        return slayer;
    }

}
