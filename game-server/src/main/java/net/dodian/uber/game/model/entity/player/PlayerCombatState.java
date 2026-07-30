package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.combat.PlayerAttackCombatKt;
import net.dodian.uber.game.engine.event.GameEventScheduler;
import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.PendingHitBuffer;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.engine.systems.combat.CombatDefenderReaction;
import net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService;
import net.dodian.uber.game.engine.systems.combat.CombatStartService;
import net.dodian.uber.game.engine.systems.combat.CombatIntent;
import net.dodian.uber.game.engine.systems.combat.CombatReachService;
import net.dodian.uber.game.engine.systems.skills.ProgressionService;
import net.dodian.uber.game.engine.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlayerCombatState {
    private static final Logger logger = LoggerFactory.getLogger(PlayerCombatState.class);
    private static final boolean COMBAT_TELEMETRY_ENABLED = Boolean.getBoolean("combat.telemetry.enabled");
    private final Player owner;
    private final PendingHitBuffer pendingHits = new PendingHitBuffer();

    PlayerCombatState(Player owner) {
        this.owner = owner;
    }

    void dealDamage(Entity attacker, int amt, Entity.hitType type) {
        Client player = (Client) owner;
        int originalAmt = amt;
        String mitigationBranch = "none";
        if (attacker != null && ((attacker.getType() == Entity.Type.NPC && ((Npc) attacker).getCurrentHealth() < 1)
                || (attacker.getType() == Entity.Type.PLAYER && ((Client) attacker).getCurrentHealth() < 1))) {
            setLastCombat(16);
        }
        if (owner.isDeathSequenceActive() || owner.getCurrentHealth() < 1) {
            amt = 0;
            mitigationBranch = "target_dead_or_dying";
        } else if (amt > owner.currentHealth) {
            amt = owner.currentHealth;
            mitigationBranch = "clamped_to_current_hp";
        }
        double rolledChance = Math.random();
        double chance = net.dodian.uber.skills.prayer.PrayerCombatService.damageNeglectChance(owner.getLevel(Skill.PRAYER));
        double damageNeglect = player.neglectDmg() / 10D;
        double reduceDamage = 1.0 - (damageNeglect / 100);
        int oldDamage = amt;
        if (!(player.getInDuel() && player.getDuelRule()[5]) && rolledChance <= chance && player.playerBonus[13] > 0 && oldDamage > 0) {
            amt = reduceDamage <= 0 ? 0 : (int) (amt * reduceDamage);
            mitigationBranch = "neglect";
            if (amt != oldDamage) {
                player.send(new SendMessage("<col=FFD700>You neglected " + (amt == 0 ? "all" : "some") + " of the damage!"));
            }
        }
        if (COMBAT_TELEMETRY_ENABLED) {
            logger.info(
                    "combat.telemetry phase=mitigation player={} attackerType={} branch={} before={} after={} duel={} duelProtectRule={}",
                    player.getPlayerName(),
                    attacker == null ? "none" : attacker.getType(),
                    mitigationBranch,
                    originalAmt,
                    amt,
                    player.getInDuel(),
                    player.getInDuel() && player.getDuelRule()[5]
            );
        }
        CombatLogoutLockService.refreshInteraction(attacker, owner);
        maybePlayDefenderReaction(amt, inferDamageType(attacker));
        applyDamage(attacker, amt, type);
        if (attacker != null && player.autoRetaliate && player.target == null && player.getCurrentHealth() > 0) {
            if (attacker.getType() == Entity.Type.NPC) {
                CombatStartService.startNpcAttack(player, (Npc) attacker, CombatIntent.ATTACK_NPC);
            } else if (attacker.getType() == Entity.Type.PLAYER) {
                CombatStartService.startPlayerAttack(player, (Client) attacker, CombatIntent.ATTACK_PLAYER);
            }
        }
    }

    void dealDamage(int amt, Entity.hitType type, Entity attacker, Entity.damageType damageType) {
        dealDamage(amt, type, attacker, damageType, false);
    }

    void dealDamageAfterProjectileLaunch(int amt, Entity.hitType type, Entity attacker, Entity.damageType damageType) {
        dealDamage(amt, type, attacker, damageType, true);
    }

    private void dealDamage(int amt, Entity.hitType type, Entity attacker, Entity.damageType damageType,
                            boolean projectileAlreadyValidated) {
        Client player = (Client) owner;
        net.dodian.uber.game.api.plugin.skills.SkillPlayer skillPlayer =
                net.dodian.uber.game.engine.systems.skills.SkillPlayerBridge.of(player);
        if (owner.isDeathSequenceActive() || owner.getCurrentHealth() < 1) {
            return;
        }
        if (!projectileAlreadyValidated && attacker != null && requiresProjectileLineOfSight(damageType)
                && !CombatReachService.hasProjectileLineOfSight(attacker, owner)) {
            return;
        }
        Npc npc = attacker instanceof Npc ? (Npc) attacker : null;
        int originalAmt = amt;
        String mitigationBranch = "none";
        if (damageType.equals(Entity.damageType.FIRE_BREATH)) {
            boolean hasShield = player.getEquipment()[Equipment.Slot.SHIELD.getId()] == 1540
                    || player.getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284;
            boolean hasProtect = net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMagic(skillPlayer);
            boolean hasAntifire = owner.antiFireEffect();
            boolean hasSuperAntifire = owner.superAntifireEffect();
            if (npc != null && npc.getId() == 239) {
                if (hasSuperAntifire) {
                    amt = 0;
                    mitigationBranch = "kbd_superantifire";
                } else if (hasAntifire && hasShield) {
                    amt = 0;
                    mitigationBranch = "kbd_antifire_shield";
                } else if (hasAntifire && hasProtect) {
                    amt = 0;
                    mitigationBranch = "kbd_antifire_protect";
                } else if (hasShield && hasProtect) {
                    amt = 0;
                    mitigationBranch = "kbd_shield_protect";
                } else if (hasShield) {
                    amt = Math.min(amt, 5);
                    mitigationBranch = "kbd_shield";
                } else if (hasProtect) {
                    amt = Math.min(amt, 10);
                    mitigationBranch = "kbd_protect";
                } else if (hasAntifire) {
                    amt = Math.max(amt - 15, 0);
                    mitigationBranch = "kbd_antifire";
                } else {
                    mitigationBranch = "kbd_full";
                }
                if (amt == 0 && originalAmt > 0) {
                    player.send(new SendMessage("You are protected against the dragonfire breath."));
                }
            } else {
                boolean gotAntiEffect = hasShield || hasProtect || hasAntifire;
                if (gotAntiEffect) {
                    amt *= 3;
                    amt /= 10;
                    mitigationBranch = "fire_breath_reduced";
                } else {
                    player.send(new SendMessage("You are badly burnt by the dragon fire!"));
                    mitigationBranch = "fire_breath_full";
                }
            }
        } else if (damageType.equals(Entity.damageType.MELEE)
                && net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMelee(skillPlayer)) {
            amt /= 2;
            mitigationBranch = "protect_melee";
        } else if (damageType.equals(Entity.damageType.RANGED)
                && net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromRange(skillPlayer)) {
            amt /= 2;
            mitigationBranch = "protect_range";
        } else if (damageType.equals(Entity.damageType.MAGIC)
                && net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMagic(skillPlayer)) {
            amt /= 2;
            mitigationBranch = "protect_magic";
        } else if (damageType.equals(Entity.damageType.JAD_RANGED)
                && net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromRange(skillPlayer)) {
            amt = 0;
            mitigationBranch = "protect_jad_range";
        } else if (damageType.equals(Entity.damageType.JAD_MAGIC)
                && net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMagic(skillPlayer)) {
            amt = 0;
            mitigationBranch = "protect_jad_magic";
        }
        if (COMBAT_TELEMETRY_ENABLED) {
            logger.info(
                    "combat.telemetry phase=mitigation player={} attackerType={} damageType={} branch={} before={} after={} protectMelee={} protectRange={} protectMagic={}",
                    player.getPlayerName(),
                    attacker == null ? "none" : attacker.getType(),
                    damageType,
                    mitigationBranch,
                    originalAmt,
                    amt,
                    net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMelee(skillPlayer),
                    net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromRange(skillPlayer),
                    net.dodian.uber.skills.prayer.PrayerCombatService.protectsFromMagic(skillPlayer)
            );
        }
        CombatLogoutLockService.refreshInteraction(attacker, owner);
        maybePlayDefenderReaction(amt, damageType);
        applyDamage(attacker, amt, type);
        if (attacker != null && player.autoRetaliate && player.target == null && player.getCurrentHealth() > 0) {
            if (attacker.getType() == Entity.Type.NPC) {
                CombatStartService.startNpcAttack(player, (Npc) attacker, CombatIntent.ATTACK_NPC);
            } else if (attacker.getType() == Entity.Type.PLAYER) {
                CombatStartService.startPlayerAttack(player, (Client) attacker, CombatIntent.ATTACK_PLAYER);
            }
        }
    }

    private static boolean requiresProjectileLineOfSight(Entity.damageType type) {
        return type == Entity.damageType.RANGED || type == Entity.damageType.MAGIC ||
                type == Entity.damageType.JAD_RANGED || type == Entity.damageType.JAD_MAGIC ||
                type == Entity.damageType.FIRE_BREATH;
    }

    private void applyDamage(Entity attacker, int amt, Entity.hitType type) {
        Client player = (Client) owner;
        appendHit(amt, type);
        owner.setCurrentHealth(Math.max(owner.getCurrentHealth() - amt, 0));
        ProgressionService.refresh(player, Skill.HITPOINTS);
        player.debug("Dealing " + amt + " damage to you (hp=" + owner.currentHealth + ")");
        if (attacker instanceof Player) {
            int totalDamage;
            if (owner.getDamage().containsKey(attacker)) {
                totalDamage = owner.getDamage().get(attacker) + amt;
                owner.getDamage().remove(attacker);
            } else {
                totalDamage = amt;
            }
            owner.getDamage().put(attacker, totalDamage);
        }
        boolean veracEffect = Misc.chance(8) == 1 && owner.armourSet("verac");
        if (veracEffect && amt > 0 && owner.getCurrentHealth() > 0 && attacker instanceof Player) {
            player.stillgfx(1041, attacker.getPosition(), 100);
            ((Player) attacker).dealDamage(player, amt, type);
        } else if (veracEffect && amt > 0 && owner.getCurrentHealth() > 0 && attacker instanceof Npc) {
            player.stillgfx(1041, attacker.getPosition(), 100);
            ((Npc) attacker).dealDamage(player, amt, type);
        }
    }

    boolean isInCombat() {
        return getLastCombat() > 0;
    }

    int getLastCombat() {
        return owner.lastCombat;
    }

    void setLastCombat(int lastCombat) {
        owner.lastCombat = lastCombat;
    }

    int getCombatTimer() {
        return owner.combatTimer;
    }

    void setCombatTimer(int timer) {
        owner.combatTimer = timer;
    }

    int getStunTimer() {
        return owner.stunTimer;
    }

    void setStunTimer(int timer) {
        owner.stunTimer = timer;
    }

    int getSnareTimer() {
        return owner.snareTimer;
    }

    void setSnareTimer(int timer) {
        owner.snareTimer = timer;
    }

    int getDamageDealt() {
        return pendingHits.getPrimaryDamage();
    }

    int getDamageDealt2() {
        return pendingHits.getSecondaryDamage();
    }

    Entity.hitType getHitType() {
        return pendingHits.getPrimaryType();
    }

    Entity.hitType getHitType2() {
        return pendingHits.getSecondaryType();
    }

    void clearPendingHits() {
        pendingHits.clear();
    }

    boolean isDeadOrDying() {
        return owner.isDeathSequenceActive() || owner.getCurrentHealth() < 1;
    }

    private PendingHitBuffer.AppendResult appendHit(int damage, Entity.hitType type) {
        PendingHitBuffer.AppendResult result = pendingHits.appendHit(damage, type);
        if (result == PendingHitBuffer.AppendResult.PRIMARY) {
            owner.getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        } else if (result == PendingHitBuffer.AppendResult.SECONDARY) {
            owner.getUpdateFlags().setRequired(UpdateFlag.HIT2, true);
        }
        return result;
    }

    private void maybePlayDefenderReaction(int damage, Entity.damageType damageType) {
        CombatDefenderReaction.playBlockAnimation((Client) owner, damage, damageType);
    }

    private Entity.damageType inferDamageType(Entity attacker) {
        if (attacker instanceof Client) {
            Client player = (Client) attacker;
            int style = PlayerAttackCombatKt.getAttackStyle(player);
            if (style == 1) {
                return Entity.damageType.RANGED;
            }
            if (style == 2) {
                return Entity.damageType.MAGIC;
            }
            return Entity.damageType.MELEE;
        }
        return null;
    }
}
