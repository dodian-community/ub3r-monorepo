package net.dodian.uber.game.model.player.skills.prayer;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;

import java.util.HashMap;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class Prayers {

    /**
     * Prayer statuses
     */
    private final boolean[] prayerstatus = new boolean[Prayer.values().length];

    /**
     * The Player this manager belongs to
     */
    private final Player p;
    private final Client c;
    private long lastClicked = System.currentTimeMillis();

    /**
     * Create a prayermanager instance for a player
     *
     * @param player The player to create for
     */
    public Prayers(Player player) {
        this.p = player;
        this.c = (Client) player;
    }

    /**
     * Prayer masks
     */
    private static final int OVERHEAD_PRAYER = 1;
    private static final int ATTACK_PRAYER = 2;
    private static final int STRENGTH_PRAYER = 4;
    private static final int RANGE_PRAYER = 8;
    private static final int MAGIC_PRAYER = 16;
    private static final int DEFENCE_PRAYER = 32;

    public enum Prayer {
        /**
         * Low level prayers
         */
        THICK_SKIN(5, 3, 83, 21233, DEFENCE_PRAYER),
        BURST_OF_STRENGTH(5, 3, 84, 21234, STRENGTH_PRAYER | MAGIC_PRAYER | RANGE_PRAYER),
        CLARITY_OF_THOUGHT(5,3, 85, 21235, ATTACK_PRAYER),
        SHARP_EYE(10, 5, 700, 77100, RANGE_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),
        MYSTIC_WILL(10,3, 701, 77102, MAGIC_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),

        /**
         * Medium level prayers
         */
        ROCK_SKIN(20, 6, 86, 21236, DEFENCE_PRAYER),
        SUPERHUMAN_STRENGTH(20, 6, 87, 21237, STRENGTH_PRAYER | MAGIC_PRAYER | RANGE_PRAYER),
        IMPROVED_REFLEXES(20,6, 88, 21238, ATTACK_PRAYER),
        HAWK_EYE(25, 10, 702, 77104, RANGE_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),
        MYSTIC_LORE(25, 10, 703, 77106,MAGIC_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),

        /**
         * Misc prayers like protect item
         */
        RAPID_RESTORE(-1, 1, 89, 21239),
        RAPID_HEAL(-1, 1, 90, 21240),
        PROTECT_ITEM(-1, 1, 91, 21241),

        /**
         * High level prayers
         */
        STEEL_SKIN(40, 12, 92, 21242, DEFENCE_PRAYER),
        ULTIMATE_STRENGTH(40, 12, 93, 21243, STRENGTH_PRAYER | MAGIC_PRAYER | RANGE_PRAYER),
        INCREDIBLE_REFLEXES(40, 12, 94, 21244, ATTACK_PRAYER),
        EAGLE_EYE(45, 18, 704, 77109, RANGE_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),
        MYSTIC_MIGHT(45, 18, 705, 77111, MAGIC_PRAYER | STRENGTH_PRAYER | ATTACK_PRAYER),

        /**
         * Protect prayers
         */
        PROTECT_MAGIC(55, 15, 95, 21245, OVERHEAD_PRAYER, HeadIcon.PROTECT_MAGIC),
        PROTECT_RANGE(55, 15, 96, 21246, OVERHEAD_PRAYER, HeadIcon.PROTECT_MISSLES),
        PROTECT_MELEE(55, 15, 97, 21247, OVERHEAD_PRAYER, HeadIcon.PROTECT_MELEE),

        /**
         * Damage dealing/stat recovering/prayer "stealing" prayers
         */
        RETRIBUTION(-1, 15,98, 2171, OVERHEAD_PRAYER, HeadIcon.RETRIBUTION),
        REDEMPTION(-1, 15, 99, 2172, OVERHEAD_PRAYER, HeadIcon.REDEMPTION),
        SMITE(-1, 15, 100, 2173, OVERHEAD_PRAYER, HeadIcon.SMITE),

        /**
         * Highest level prayers available
         */
        CHIVALRY(70, 24, 706, 77113, ATTACK_PRAYER | STRENGTH_PRAYER | DEFENCE_PRAYER | MAGIC_PRAYER | RANGE_PRAYER),
        PIETY(80, 24, 707, 77115, ATTACK_PRAYER | STRENGTH_PRAYER | DEFENCE_PRAYER | MAGIC_PRAYER | RANGE_PRAYER);

        /**
         * A map of Buttonid -> prayer
         */
        private static final HashMap<Integer, Prayer> prayers = new HashMap<>();

        static {
            for (Prayer prayer : Prayer.values()) {
                prayers.put(prayer.getButtonId(), prayer);
            }
        }

        private final int levelreq;
        private final int configId;
        private final int buttonId;
        private final int drainEffect;
        private int prayMask;
        private HeadIcon headIcon;

        Prayer(int praylevelreq, int drainEffect, int configId, int buttonId) {
            this.levelreq = praylevelreq;
            this.drainEffect = drainEffect;
            this.configId = configId;
            this.buttonId = buttonId;
        }

        Prayer(int praylevelreq, int drainEffect, int configId, int buttonId, int prayMask) {
            this.levelreq = praylevelreq;
            this.drainEffect = drainEffect;
            this.configId = configId;
            this.buttonId = buttonId;
            this.prayMask = prayMask;
        }

        Prayer(int praylevelreq, int drainEffect, int configId, int buttonId, int prayMask, HeadIcon headIcon) {
            this.levelreq = praylevelreq;
            this.drainEffect = drainEffect;
            this.configId = configId;
            this.buttonId = buttonId;
            this.prayMask = prayMask;
            this.headIcon = headIcon;
        }

        public int getPrayerLevel() {
            return levelreq;
        }

        public int getDrainEffect() {
            return drainEffect;
        }

        public int getConfigId() {
            return configId;
        }

        public int getButtonId() {
            return buttonId;
        }

        public int getMask() {
            return prayMask;
        }

        public HeadIcon getHeadIcon() {
            return headIcon;
        }

        public static Prayer forButton(int button) {
            return prayers.get(button);
        }
    }
    /**
     * Toggle a prayer, setting the headicon and checking level if turning on
     *
     * @param prayer The prayer to toggle
     */
    public void togglePrayer(Prayer prayer) {
        if (prayer.getPrayerLevel() != -1 && Skills.getLevelForExperience(p.getExperience(Skill.PRAYER)) < prayer.getPrayerLevel()) {
            c.send(new SendMessage(
                    "You need a prayer level of at least " + prayer.getPrayerLevel() + " to use " + formatEnum(prayer).toLowerCase()));
            c.frame87(prayer.getConfigId(), 0);
            //c.send(new Sound(447));
            return;
        }
        if(c.getCurrentPrayer() < 1) { //Can't use prayer with no prayer points!
            c.send(new SendMessage("You have no prayer points currently! Recharge at a nearby altar"));
            reset();
            return;
        }
        if(c.duelFight || prayer.getPrayerLevel() == -1 || c.deathStage > 0) { //No prayer during a duel and prayer that has yet to be added!
            reset();
            return;
        }

        if (isPrayerOn(prayer)) {
            set(prayer, false);
            c.frame87(prayer.getConfigId(), 0);
            if (!ifCheck()) {
                p.setHeadIcon(HeadIcon.NONE.asInt());
                p.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            }
        } else {
            set(prayer, true);
            if (prayer.getHeadIcon() != null) {
                p.setHeadIcon(prayer.getHeadIcon().asInt());
                p.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            }
            checkExtraPrayers(prayer);
        }
    }

    /**
     * Set a prayer on/off
     *
     * @param prayer The prayer to set
     * @param on     true if on, false if off
     */
    public void set(Prayer prayer, boolean on) {
        prayerstatus[prayer.ordinal()] = on;
    }

    /* Prayer drain rate! */
    public int getDrain() {
        int drain = 0;
        for (Prayer prayer : Prayer.values()) {
            if (isPrayerOn(prayer))
                drain += prayer.getDrainEffect();
        }
        return drain;
    }
    public double getDrainRate() {
        double drainResistance = 60.0 + (2 * c.playerBonus[8]);
        if(getDrain() == 0) return 0;
        return drainResistance / getDrain();
    }

    public double drainRate = 0.0;

    /**
     * Clear prayers/curses
     */
    public void reset() {
        for (Prayer prayer : Prayer.values()) {
            set(prayer, false);
            c.frame87(prayer.getConfigId(), 0);
        }
        p.setHeadIcon(HeadIcon.NONE.asInt());
        p.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    /**
     * Check if a prayer is on
     *
     * @param prayer The prayer to check
     * @return If the prayer is on, true
     */
    public boolean isPrayerOn(Prayer prayer) {
        return prayerstatus[prayer.ordinal()];
    }

    public boolean ifCheck() {
        for (Prayer prayer : Prayer.values()) {
            if (prayer.getMask() == 1 && prayerstatus[prayer.ordinal()])
                return true;
        }
        return false;
    }

    /**
     * Format an enum object or other object from all uppercase to first
     * uppercase.
     *
     * @param object The object to format
     * @return The formatted name
     */
    public static String formatEnum(Object object) {
        String s = object.toString().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + (s.substring(1).replaceAll("_", " "));
    }

    /**
     * Check for the extra prayers on, such as turning on Piety turns off all
     * other strength boosting
     *
     * @param prayer The prayer toggled
     */
    public void checkExtraPrayers(Prayer prayer) {
        if (prayer.getMask() == -1) {
            return;
        }
        boolean overheadPrayer = (prayer.getMask() & OVERHEAD_PRAYER) != 0;
        boolean attackPrayer = (prayer.getMask() & ATTACK_PRAYER) != 0;
        boolean strengthPrayer = (prayer.getMask() & STRENGTH_PRAYER) != 0;
        boolean defencePrayer = (prayer.getMask() & DEFENCE_PRAYER) != 0;
        boolean rangePrayer = (prayer.getMask() & RANGE_PRAYER) != 0;
        boolean magicPrayer = (prayer.getMask() & MAGIC_PRAYER) != 0;
        for (Prayer pray : Prayer.values()) {
            if (!isPrayerOn(pray) || pray == prayer) {
                continue;
            }
            if (pray.getMask() == -1)
                continue;
            if ((pray.getMask() & OVERHEAD_PRAYER) != 0 && overheadPrayer
                    || (pray.getMask() & ATTACK_PRAYER) != 0 && attackPrayer
                    || (pray.getMask() & STRENGTH_PRAYER) != 0 && strengthPrayer
                    || (pray.getMask() & DEFENCE_PRAYER) != 0 && defencePrayer
                    || (pray.getMask() & RANGE_PRAYER) != 0 && rangePrayer
                    || (pray.getMask() & MAGIC_PRAYER) != 0 && magicPrayer) {
                togglePrayer(pray);
            }
        }
    }
}
