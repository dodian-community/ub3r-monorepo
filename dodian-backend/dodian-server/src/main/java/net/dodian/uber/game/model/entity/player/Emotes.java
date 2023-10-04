package net.dodian.uber.game.model.entity.player;

/**
 * @author Ryan Augustynowicz
 */
public enum Emotes {

    YES(168, 0x357), NO(169, 0x358), THINK(162, 0x359), BOW(164, 0x35A), ANGRY(165, 0x35B), CRY(161, 0x35C), LAUGH(170,
            0x35D), CHEER(171, 0x35E), WAVE(163, 0x35F), BECKON(167, 0x360), CLAP(172, 0x361), DANCE(166, 920), PANIC(52050,
            0x839), JIG(52051, 0x83A), SPIN(52052, 0x83B), HEADBANG(52053, 0x83C), JUMP_FOR_JOY(52054,
            0x83D), RASP_BERRY(52055, 0x83E), YAWN(52056, 0x83F), SALUTE(52057, 0x840), SHRUG(52058,
            0x841), BLOW_KISS(43092, 0x558), GLASS_BOX(2155, 0x46B), CLIMB_ROPE(25103, 0x46A), LEAN(25106,
            0x469), GLASS_WALL(2154, 0x468), GOBLIN_BOW(52071, 0x84F), GOBLIN_DANCE(52072, 0x850);

    private final int buttonId, animationId;

    Emotes(int buttonId, int animationId) {
        this.buttonId = buttonId;
        this.animationId = animationId;
    }

    public int getButtonId() {
        return buttonId;
    }

    public int getAnimationId() {
        return animationId;
    }

    public static void doEmote(int buttonId, Player player) {
        for (Emotes emote : Emotes.values()) {
            if (emote.getButtonId() == buttonId) {
                player.sendAnimation(emote.getAnimationId());
                return;
            }
        }
    }

}
