package net.dodian.uber.game.model.combat.impl;

import net.dodian.uber.game.model.combat.CombatAction;
import net.dodian.uber.game.model.entity.Entity;

public final class AttackAction extends CombatAction {

  @Override
  public void performCombatAction(Entity attacker, Entity victim) {

  }

  public static final void startAction(Entity attacker, Entity victim, CombatTypeHandler action) {
    action.initiateAction(attacker, victim);
  }
}
