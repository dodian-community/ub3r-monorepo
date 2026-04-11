package net.dodian.uber.game.model.entity;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.content.combat.style.CombatStyle;
import net.dodian.uber.game.systems.pathing.collision.CollisionManager;
import net.dodian.uber.game.model.entity.npc.Npc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class Entity {

    private final Position position;
    private final Position originalPosition;
    private final int slot;
    private int gfxId, gfxHeight;
    private final Type type;

    private final UpdateFlags updateFlags;

    private CombatStyle combatStyle = CombatStyle.ACCURATE_MELEE;

    private int animationDelay;
    private int animationId;
    private String text;
    private int faceCoordinateX = 1;
    private int faceCoordinateY = 1;
    private int persistedFaceX = 0;
    private int persistedFaceY = 0;
    private volatile long currentGameCycle = 0L;
    private volatile long processedGameCycle = 0L;

    private final Map<Entity, Integer> damage = new HashMap<>();

    public Entity(Position position, int slot, Type type) {
        this.position = position.copy();
        this.originalPosition = position.copy();
        this.updateFlags = new UpdateFlags();
        this.slot = slot;
        this.type = type;
    }

    public void performAnimation(int id, int delay) {
        setAnimationId(id);
        setAnimationDelay(delay * 10);
        getUpdateFlags().setRequired(UpdateFlag.ANIM, true);
    }

    public void setGfx(int id, int height) {
        this.gfxId = id;
        this.gfxHeight = height;
        getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }

    public void performGraphic(int id, int delay) {
        setGfx(id, delay);
    }
    public int getGfxHeight() {
        return this.gfxHeight;
    }
    public int getGfxId() {
        return this.gfxId;
    }

    public void setText(String text) {
        this.text = text;
        getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
    }
    public String getText() {
        return text;
    }

    public boolean isWithinDistance(int entityX, int entityY, int otherX, int otherY, int distance) {
        int dist = (int) Math.sqrt(Math.pow(entityX - otherX, 2) + Math.pow(entityY - otherY, 2));
        return dist <= distance;
    }

    public boolean isWithinDistance(Position otherPosition, int distance) {
        return getPosition().withinDistance(otherPosition, distance);
    }

    public int getSlot() {
        return slot;
    }

    public void setFocus(int focusPointX, int focusPointY) {
        faceCoordinateX = encodeFaceCoordinate(focusPointX);
        faceCoordinateY = encodeFaceCoordinate(focusPointY);
        getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, false);
        getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
    }

    public int getFaceCoordinateX() {
        return faceCoordinateX;
    }

    public int getFaceCoordinateY() {
        return faceCoordinateY;
    }

    public int getFaceCoordinateWorldX() {
        return decodeFaceCoordinate(faceCoordinateX);
    }

    public int getFaceCoordinateWorldY() {
        return decodeFaceCoordinate(faceCoordinateY);
    }

    public void setPersistedFaceCoord(int focusPointX, int focusPointY) {
        persistedFaceX = focusPointX;
        persistedFaceY = focusPointY;
    }

    public int getPersistedFaceX() {
        return persistedFaceX;
    }

    public int getPersistedFaceY() {
        return persistedFaceY;
    }

    public boolean hasPersistedFaceCoord() {
        return persistedFaceX != 0 || persistedFaceY != 0;
    }

    public int getPersistedFaceCoordinateX() {
        return encodeFaceCoordinate(persistedFaceX);
    }

    public int getPersistedFaceCoordinateY() {
        return encodeFaceCoordinate(persistedFaceY);
    }

    public long getCurrentGameCycle() {
        return currentGameCycle;
    }

    public void setCurrentGameCycle(long currentGameCycle) {
        this.currentGameCycle = currentGameCycle;
    }

    public long getProcessedGameCycle() {
        return processedGameCycle;
    }

    public void setProcessedGameCycle(long processedGameCycle) {
        this.processedGameCycle = processedGameCycle;
    }

    private int encodeFaceCoordinate(int value) {
        long encoded = (2L * value) + 1L;
        if (encoded < 0L) {
            return 0;
        }
        if (encoded > 0xFFFFL) {
            return 0xFFFF;
        }
        return (int) encoded;
    }

    private int decodeFaceCoordinate(int value) {
        if (value <= 1) {
            return 0;
        }
        return (value - 1) / 2;
    }

    public int getAnimationDelay() {
        return animationDelay;
    }

    public void setAnimationDelay(int animationDelay) {
        this.animationDelay = animationDelay;
    }

    public int getAnimationId() {
        return animationId;
    }

    public void setAnimationId(int animationId) {
        this.animationId = animationId;
    }

    public Type getType() {
        return type;
    }

    public int getSize() {
        if (type != null && type == Type.NPC && Server.npcManager.getData(((Npc) this).getId()) != null) {
            return Server.npcManager.getData(((Npc) this).getId()).getSize();
        }
        return 1;
    }
    public int getDistanceDelay(int distance, boolean magic) {
        int delay;
        if(magic)
            delay = 1 + (int)Math.floor((1 + distance) / 3);
        else delay = 1 + (int)Math.floor((3 + distance) / 6);
        return delay;
    }

    public Position getPosition() {
        return this.position;
    }

    public Position getOriginalPosition() {
        return this.originalPosition;
    }

    public void moveTo(int x, int y, int z) {
        position.moveTo(x, y, z);
    }

    public boolean goodDistanceEntity(Entity other, int distance) {
        Rectangle thisArea = new Rectangle(getPosition().getX() - distance, getPosition().getY() - distance,
                2 * distance + getSize(), 2 * distance + getSize());
        Rectangle otherArea = new Rectangle(other.getPosition().getX(), other.getPosition().getY(), other.getSize(),
                other.getSize());
        return thisArea.intersects(otherArea);
    }

    public boolean canMove(int x, int y) {
        return CollisionManager.global().canMove(getPosition().getX(), getPosition().getY(),
                getPosition().getX() + x, getPosition().getY() + y, getPosition().getZ(), getSize(), getSize());
    }

    public CombatStyle getCombatStyle() {
        return combatStyle;
    }

    public void setCombatStyle(CombatStyle combatStyle) {
        this.combatStyle = combatStyle;
    }

    public Map<Entity, Integer> getDamage() {
        return damage;
    }

    public UpdateFlags getUpdateFlags() {
        return this.updateFlags;
    }

    /**
     * Gets the type of this entity.
     *
     * @return The entity type
     */
    public abstract EntityType getEntityType();

    public enum Type { NPC, PLAYER }

    public enum damageType {
        MELEE, RANGED, MAGIC, //Standard
        FIRE_BREATH, JAD_MAGIC, JAD_RANGED, //Special
        BLOODATTACK, TRUEDAMAGE //Unique
    }
    public enum hitType {
        STANDARD, CRIT, POISON, BURN, BLEED //Bleed is custom and thus got no hitsplat yet!
    }

}
