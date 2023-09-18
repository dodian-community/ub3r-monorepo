package net.dodian.uber.game.model.player.skills;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Dashboard
 */
public enum Skill {
    ATTACK(0, "attack", 24138, 24137),
    DEFENCE(1, "defence", 24170, 24169),
    STRENGTH(2, "strength", 24154, 24153),
    HITPOINTS(3, "hitpoints", 24139, 24140),
    RANGED(4, "ranged", 24186, 24185),
    PRAYER(5, "prayer", 24202, 24201),
    MAGIC(6, "magic", 24219, 24218),
    COOKING(7, "cooking", 24190, 24189),
    WOODCUTTING(8, "woodcutting", 24222, 24221),
    FLETCHING(9, "fletching", 24220, 24219),
    FISHING(10, "fishing", 24174, 23173),
    FIREMAKING(11, "firemaking", 24206, 24205),
    CRAFTING(12, "crafting", 24204, 24203),
    SMITHING(13, "smithing", 24158, 24157),
    MINING(14, "mining", 24142, 24141),
    HERBLORE(15, "herblore", 24172, 24171),
    AGILITY(16, "agility", 24156, 24155),
    THIEVING(17, "thieving", 24188, 24187),
    SLAYER(18, "slayer", 24367, 24366),
    FARMING(19, "farming", 24372, 24371, false),
    RUNECRAFTING(20, "runecrafting", 24362, 24361);

    private final int id;
    private final String name;
    private final boolean enabled;
    private final int levelComponent;
    private final int currentComponent;

    Skill(int id, String name, int levelComponent, int currentComponent) {
        this(id, name, levelComponent, currentComponent, true);
    }

    Skill(int id, String name, int levelComponent, int currentComponent, boolean enabled) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.levelComponent = levelComponent;
        this.currentComponent = currentComponent;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCurrentComponent() {
        return currentComponent;
    }

    public int getLevelComponent() {
        return levelComponent;
    }

    public static Skill getSkill(int id) {
        for (Skill skill : values()) {
            if (skill.getId() == id)
                return skill;
        }
        return null;
    }

    public static Stream<Skill> enabledSkills() {
        return Arrays.stream(Skill.values()).filter(Skill::isEnabled);
    }

    public static Stream<Skill> disabledSkills() {
        return Arrays.stream(Skill.values()).filter(skill -> !skill.isEnabled());
    }
}
