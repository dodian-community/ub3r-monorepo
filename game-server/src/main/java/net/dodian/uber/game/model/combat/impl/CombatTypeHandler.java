package net.dodian.uber.game.model.combat.impl;

import net.dodian.uber.game.model.entity.Entity;

public abstract class CombatTypeHandler {

  public abstract void initiateAction(Entity attacker, Entity victim);

  public abstract void getMaxHit(Entity attacker);

  public abstract boolean getHitRoll(Entity attacker, Entity victim);

  public abstract void appendExperience(Entity attacker, CombatExperienceHandler style);

  public enum CombatTypes {
    MELEE, RANGED, MAGIC
  }
}
