package net.dodian.uber.game.model.player.skills.prayer;

/**
 * @author Dashboard
 */
public enum Bones {

    BONES(526, 45),
    BAT_BONES(530, 85),
    BIG_BONES(532, 150),
    ZOGRE_BONES(4812, 265),
    JOGRE_BONES(3125, 395),
    RAURG_BONES(4832, 585),
    DRAGON_BONES(536, 735),
    DAGANNOTH_BONES(6729, 1050),
    OURG_BONES(4834, 1200);

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
