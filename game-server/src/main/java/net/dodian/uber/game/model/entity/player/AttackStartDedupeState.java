package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.engine.systems.combat.CombatIntent;
import net.dodian.uber.game.model.entity.Entity;

public final class AttackStartDedupeState {
    private final CombatIntent intent;
    private final Entity.Type targetType;
    private final int targetSlot;
    private final long acceptedCycle;

    public AttackStartDedupeState(
        CombatIntent intent,
        Entity.Type targetType,
        int targetSlot,
        long acceptedCycle
    ) {
        this.intent = intent;
        this.targetType = targetType;
        this.targetSlot = targetSlot;
        this.acceptedCycle = acceptedCycle;
    }

    public CombatIntent getIntent() {
        return intent;
    }

    public Entity.Type getTargetType() {
        return targetType;
    }

    public int getTargetSlot() {
        return targetSlot;
    }

    public long getAcceptedCycle() {
        return acceptedCycle;
    }
}
