package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

final class PlayerUpdateState {
    private final Player owner;
    private ByteMessage cachedUpdateBlock = null;
    private boolean cachedUpdateBlockValid = false;
    private volatile long appearanceRevision = 0L;
    private volatile long cachedAppearanceRevision = -1L;
    private volatile byte[] cachedAppearanceBytes = null;
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

    boolean isCachedUpdateBlockValid() {
        return cachedUpdateBlockValid && cachedUpdateBlock != null && cachedUpdateBlock.getBuffer().refCnt() > 0;
    }

    void writeCachedUpdateBlock(ByteMessage dst) {
        dst.putBytes(cachedUpdateBlock);
    }

    void cacheUpdateBlock(ByteMessage src) {
        releaseCachedUpdateBlock();
        if (src != null) {
            src.retain();
            cachedUpdateBlock = src;
            cachedUpdateBlockValid = true;
            return;
        }
        cachedUpdateBlockValid = false;
    }

    void invalidateCachedUpdateBlock() {
        cachedUpdateBlockValid = false;
        releaseCachedUpdateBlock();
    }

    void markAppearanceDirty() {
        appearanceRevision++;
        cachedAppearanceRevision = -1L;
        cachedAppearanceBytes = null;
    }

    long getAppearanceRevision() {
        return appearanceRevision;
    }

    boolean isCachedAppearanceValid() {
        return cachedAppearanceBytes != null && cachedAppearanceRevision == appearanceRevision;
    }

    byte[] getCachedAppearanceBytes() {
        return cachedAppearanceBytes;
    }

    void cacheAppearanceBytes(byte[] bytes) {
        cachedAppearanceBytes = bytes;
        cachedAppearanceRevision = appearanceRevision;
    }

    void releaseCachedUpdateBlock() {
        if (cachedUpdateBlock == null) {
            return;
        }
        if (cachedUpdateBlock.getBuffer().refCnt() > 0) {
            cachedUpdateBlock.release();
        }
        cachedUpdateBlock = null;
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
        faceTarget(-1);
        owner.getUpdateFlags().clear();
        chatTextSize = 0;
        chatTextColor = 0;
        chatTextEffects = 0;
        invalidateCachedUpdateBlock();
    }

    void faceTarget(int index) {
        faceTarget = index;
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
        buf.put(owner.m4001, ValueType.SUBTRACT);
        buf.put(owner.m4002, ValueType.SUBTRACT);
        buf.put(owner.m4003, ValueType.SUBTRACT);
        buf.put(owner.m4004, ValueType.SUBTRACT);
        buf.putShort(owner.m4006, ByteOrder.BIG, ValueType.ADD);
        buf.putShort(owner.m4005, ValueType.ADD);
        buf.put(owner.m4007, ValueType.SUBTRACT);
    }
}
