package net.dodian.uber.game.model.entity;

public final class PendingHitBuffer {

    public enum AppendResult {
        PRIMARY,
        SECONDARY,
        DROPPED
    }

    private int primaryDamage = 0;
    private int secondaryDamage = 0;
    private Entity.hitType primaryType = Entity.hitType.STANDARD;
    private Entity.hitType secondaryType = Entity.hitType.STANDARD;
    private boolean primaryPending = false;
    private boolean secondaryPending = false;

    public AppendResult appendHit(int damage, Entity.hitType type) {
        Entity.hitType resolvedType = type == null ? Entity.hitType.STANDARD : type;
        if (!primaryPending) {
            primaryDamage = damage;
            primaryType = resolvedType;
            primaryPending = true;
            return AppendResult.PRIMARY;
        }
        if (!secondaryPending) {
            secondaryDamage = damage;
            secondaryType = resolvedType;
            secondaryPending = true;
            return AppendResult.SECONDARY;
        }
        return AppendResult.DROPPED;
    }

    public void clear() {
        primaryDamage = 0;
        secondaryDamage = 0;
        primaryType = Entity.hitType.STANDARD;
        secondaryType = Entity.hitType.STANDARD;
        primaryPending = false;
        secondaryPending = false;
    }

    public boolean hasPrimary() {
        return primaryPending;
    }

    public boolean hasSecondary() {
        return secondaryPending;
    }

    public int getPrimaryDamage() {
        return primaryDamage;
    }

    public int getSecondaryDamage() {
        return secondaryDamage;
    }

    public Entity.hitType getPrimaryType() {
        return primaryType;
    }

    public Entity.hitType getSecondaryType() {
        return secondaryType;
    }
}
