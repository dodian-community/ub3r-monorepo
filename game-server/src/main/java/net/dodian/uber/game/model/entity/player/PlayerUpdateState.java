package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

import java.util.Arrays;

final class PlayerUpdateState {
    /**
     * Globally-unique appearance ticket source. Uniqueness across all player instances means a
     * viewer's remembered ticket for a slot can never falsely match after the slot is reused by
     * a different session.
     */
    private static final java.util.concurrent.atomic.AtomicLong APPEARANCE_TICKETS =
            new java.util.concurrent.atomic.AtomicLong();

    private final Player owner;
    private volatile long appearanceRevision = 0L;
    private volatile long cachedAppearanceRevision = -1L;
    private volatile int cachedAppearanceSignature = Integer.MIN_VALUE;
    private volatile byte[] cachedAppearanceBytes = null;
    private volatile long appearanceTicket = APPEARANCE_TICKETS.incrementAndGet();
    private volatile byte[] ticketedAppearanceBytes = null;
    private final byte[] chatText = new byte[4096];
    private int chatTextSize = 0;
    private int chatTextEffects = 0;
    private int chatTextColor = 0;
    private String chatTextMessage = "";
    private int faceTarget = -1;
    private int graphicId = 0;
    private int graphicHeight = 0;

    PlayerUpdateState(Player owner) {
        this.owner = owner;
    }

    void markAppearanceDirty() {
        appearanceRevision++;
        cachedAppearanceRevision = -1L;
        cachedAppearanceSignature = Integer.MIN_VALUE;
        cachedAppearanceBytes = null;
    }

    long getAppearanceRevision() {
        return appearanceRevision;
    }

    boolean isCachedAppearanceValid() {
        return cachedAppearanceBytes != null
                && cachedAppearanceRevision == appearanceRevision
                && cachedAppearanceSignature == appearanceSignature();
    }

    byte[] getCachedAppearanceBytes() {
        return cachedAppearanceBytes;
    }

    void cacheAppearanceBytes(byte[] bytes) {
        cachedAppearanceBytes = bytes;
        cachedAppearanceRevision = appearanceRevision;
        cachedAppearanceSignature = appearanceSignature();
        // A new ticket only when the visible bytes actually changed — a defensive-signature
        // rebuild that produces identical bytes must not force viewers to re-receive appearance.
        if (ticketedAppearanceBytes == null || !Arrays.equals(ticketedAppearanceBytes, bytes)) {
            ticketedAppearanceBytes = bytes;
            appearanceTicket = APPEARANCE_TICKETS.incrementAndGet();
        }
    }

    long getAppearanceTicket() {
        return appearanceTicket;
    }

    /**
     * Most appearance state is changed through PlayerAppearanceState, which
     * increments the revision above. Equipment is legacy array-backed state,
     * however, so this signature is a defensive backstop for any missed
     * writer. It prevents an old model from being reused even if a caller
     * changed an equipment array directly.
     */
    private int appearanceSignature() {
        int result = Arrays.hashCode(owner.getEquipment());
        result = 31 * result + Arrays.hashCode(owner.playerLooks);
        result = 31 * result + owner.getGender();
        result = 31 * result + owner.headIcon;
        result = 31 * result + owner.skullIcon;
        result = 31 * result + (owner.isNpc ? 1 : 0);
        result = 31 * result + owner.getPlayerNpc();
        result = 31 * result + (owner.UsingAgility ? 1 : 0);
        result = 31 * result + owner.getTorso();
        result = 31 * result + owner.getArms();
        result = 31 * result + owner.getLegs();
        result = 31 * result + owner.getHands();
        result = 31 * result + owner.getFeet();
        result = 31 * result + owner.getBeard();
        result = 31 * result + owner.getHead();
        result = 31 * result + owner.getStandAnim();
        result = 31 * result + owner.getWalkAnim();
        result = 31 * result + owner.getRunAnim();
        result = 31 * result + owner.determineCombatLevel();
        result = 31 * result + (owner.getPlayerName() == null ? 0 : owner.getPlayerName().hashCode());
        return result;
    }

    byte[] getChatText() {
        return chatText;
    }

    int getChatTextSize() {
        return chatTextSize;
    }

    void setChatTextSize(int chatTextSize) {
        this.chatTextSize = chatTextSize;
    }

    int getChatTextEffects() {
        return chatTextEffects;
    }

    void setChatTextEffects(int chatTextEffects) {
        this.chatTextEffects = chatTextEffects;
    }

    int getChatTextColor() {
        return chatTextColor;
    }

    void setChatTextColor(int chatTextColor) {
        this.chatTextColor = chatTextColor;
    }

    String getChatTextMessage() {
        return chatTextMessage;
    }

    void setChatTextMessage(String chatTextMessage) {
        this.chatTextMessage = chatTextMessage == null ? "" : chatTextMessage;
    }

    void clearUpdateFlags() {
        faceTarget = -1;
        owner.getUpdateFlags().clear();
        chatTextSize = 0;
        chatTextColor = 0;
        chatTextEffects = 0;
    }

    void faceTarget(int index) {
        faceTarget = index;
        owner.getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, false);
        owner.getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);
    }

    int getFaceTarget() {
        return faceTarget;
    }

    void gfx0(int gfx) {
        graphicId = gfx;
        graphicHeight = 65536;
        owner.getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }

    void setGraphic(int graphicId, int graphicHeight) {
        this.graphicId = graphicId;
        this.graphicHeight = graphicHeight;
    }

    int getGraphicId() {
        return graphicId;
    }

    int getGraphicHeight() {
        return graphicHeight;
    }

    void appendMask400Update(ByteMessage buf) {
        buf.putBits(8, owner.m4001);
        buf.putBits(8, owner.m4002);
        buf.putBits(8, owner.m4003);
        buf.putBits(8, owner.m4004);
        buf.putBits(16, owner.m4006);
        buf.putBits(16, owner.m4005);
        buf.putBits(8, owner.m4007);
    }
}
