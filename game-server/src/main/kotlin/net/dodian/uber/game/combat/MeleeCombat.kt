package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.skills.prayer.PrayerCombatService
import net.dodian.uber.skills.agility.AgilityCombatService
import net.dodian.uber.skills.woodcutting.WoodcuttingCombatService
import net.dodian.uber.skills.mining.MiningCombatService
import net.dodian.uber.game.engine.systems.animation.PlayerAnimationService
import net.dodian.uber.game.engine.systems.combat.CombatAttackResult
import net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService
import net.dodian.uber.game.engine.systems.combat.CombatSpecialService
import net.dodian.uber.game.engine.systems.combat.resolveCombatTargetPlayer
import net.dodian.uber.game.engine.systems.animation.AttackAnimationService
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.engine.util.Misc
import net.dodian.utilities.Range
import net.dodian.utilities.Utils

var hit = 0
var hit2 = 0

fun Client.handleMeleeAttack(): CombatAttackResult? {
    if (hasStaff() && (autocast_spellIndex >= 0 || magicId >= 0))
        return null
    else if (!hasStaff() && magicId >= 0)
        return null
    else if (contentRuntimeState.isCombatUsingBow())
        return null
    if (stunTimer > 0 || target == null)
        return null

        CombatLogoutLockService.refreshInteraction(this, target)
        if (target is Player) {
            facePlayer(target.slot)
        } else {
            setFocus(target.position.x, target.position.y)
        }
        var maxHit = meleeMaxHit().toDouble()
         if (target is Npc) { // Slayer damage!
             val npc = Server.npcManager.getNpc(target.slot)
             val name = npc.npcName().lowercase()
             if(getSlayerDamage(npc.id, false) == 1)
                 maxHit *= 1.15
             else if(getSlayerDamage(npc.id, false) == 2)
                 maxHit *= 1.2
             val wolfBane = equipment[Equipment.Slot.WEAPON.id] == 2952 &&  when {
                 name.lowercase().contains("vampyre") -> true
                 name.lowercase().contains("werewolf") -> true
                 else -> false
             }
             if(wolfBane) maxHit *= 2
             val keris = equipment[Equipment.Slot.WEAPON.id] == 10581 &&  when {
                 name.lowercase().contains("kalphite") -> true
                 name.lowercase().contains("scarab") -> true
                 name.lowercase().contains("spider") -> true
                 else -> false
             }
             if(keris) maxHit *= 2
         }
        hit = Utils.random(maxHit.toInt())
        var criticalChance = AgilityCombatService.criticalChance(asSkillPlayer())
        val extra = getLevel(Skill.STRENGTH) * 0.195
        if(equipment[Equipment.Slot.SHIELD.id]==4224) criticalChance = (criticalChance * 1.5).toInt()
        val landCrit = Math.random() * 100 <= criticalChance
        val landHit = landHit(this, target)
        if(landHit && hit < 1) hit = 1 //Osrs style of hit, max hit is 1 or 1 - maxHit if you land a hit!
        if (target is Npc) {
            val npc = Server.npcManager.getNpc(target.slot)
            val name = npc.npcName().lowercase()
            val wolfBane = equipment[Equipment.Slot.WEAPON.id] == 2952 && landHit && when {
                name.lowercase().contains("vampyre") -> true
                name.lowercase().contains("werewolf") -> true
                else -> false
            }
            val keris = equipment[Equipment.Slot.WEAPON.id] == 10581 && landHit && when {
                name.lowercase().contains("kalphite") -> true
                name.lowercase().contains("scarab") -> true
                name.lowercase().contains("locust") -> true
                name.lowercase().contains("spider") -> true
                else -> false
            }
            if (landCrit && landHit) hit += Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if(wolfBane && Misc.chance(8) == 1) {
                hit *= 2
                send(SendMessage("<col=8B4513>You use the power of the wolf to hit higher!"))
            } //#FFD700
            if(keris && Misc.chance(8) == 1) {
                hit *= 2
                send(SendMessage("<col=8B4513>You use the power of the keris to hit higher!"))
            } else if (keris && Misc.chance(33) == 1) {
                hit *= 4
                send(SendMessage("<col=8B4513>You punch a hole in the creatures exoskeleton!"))
            }
            if(!handleSpecial(landCrit)) { //Do stuff here!
                if(hit >= npc.currentHealth) hit = npc.currentHealth
                npc.dealDamage(this, hit, if(landCrit && hit > 0) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            var chance = Misc.chance(8) == 1 && armourSet("guthan")
            if(chance && hit > 0) { //Guthan effect!
                stillgfx(398, npc.position, 100)
                heal(hit, (getMaxHealth().toDouble() * 0.15).toInt())
            }
            chance = Misc.chance(8) == 1 && armourSet("torag")
            if(chance) { //Torag effect!
                hit2 = hit / 2
                stillgfx(399, npc.position, 0)
                if(hit2 >= npc.currentHealth) hit2 = npc.currentHealth
                npc.dealDamage(this, hit2, if(landCrit && hit2 > 0) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            if(hit > 0) {
                if (fightType == 3) {
                    val xp = (13 * hit)
                    ProgressionService.addXp(this, xp, Skill.ATTACK)
                    ProgressionService.addXp(this, xp, Skill.DEFENCE)
                    ProgressionService.addXp(this, xp, Skill.STRENGTH)
                } else ProgressionService.addXp(this, 40 * hit, Skill.getSkill(fightType) ?: Skill.ATTACK)
                ProgressionService.addXp(this, 13 * hit, Skill.HITPOINTS)
            }
            if(hit2 > 0) {
                if (fightType == 3) {
                    val xp = (13 * hit2)
                    ProgressionService.addXp(this, xp, Skill.ATTACK)
                    ProgressionService.addXp(this, xp, Skill.DEFENCE)
                    ProgressionService.addXp(this, xp, Skill.STRENGTH)
                } else ProgressionService.addXp(this, 40 * hit2, Skill.getSkill(fightType) ?: Skill.ATTACK)
                ProgressionService.addXp(this, 13 * hit2, Skill.HITPOINTS)
            }
        }
        if (target is Player) {
            val player = resolveCombatTargetPlayer(target.slot) ?: return CombatAttackResult(getbattleTimer(equipment[Equipment.Slot.WEAPON.id]))
            if (landCrit && landHit) hit += Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if (PrayerCombatService.protectsFromMelee(player.asSkillPlayer())) hit = (hit * 0.6).toInt()
            if(!handleSpecial(landCrit)) { //Do stuff here!
                if(hit >= player.currentHealth) hit = player.currentHealth
                player.dealDamage(this, hit, if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            var chance = Misc.chance(8) == 1 && armourSet("guthan")
            if(chance && hit > 0) { //Guthan effect!
                stillgfx(398, player.position, 100)
                heal(hit, (getMaxHealth().toDouble() * 0.15).toInt())
            }
            chance = Misc.chance(8) == 1 && armourSet("torag")
            if(chance) { //Torag effect!
                hit2 = hit / 2
                stillgfx(399, player.position, 0)
                if(hit2 >= player.currentHealth) hit2 = player.currentHealth
                player.dealDamage(this, hit2, Entity.hitType.STANDARD)
            }
        }
        val nextDelay = getbattleTimer(equipment[Equipment.Slot.WEAPON.id])
        if (specialActivated) CombatSpecialService.drainSpecial(this)
        if (debug) send(SendMessage("hit = $hit, nextDelay = $nextDelay"))
    return CombatAttackResult(nextDelay)
}

fun highestAttackBonus(p: Client): Int {
    var bonus = 0
    for (i in 0..2) {
        if (p.playerBonus[i] > bonus)
            bonus = p.playerBonus[i]
    }
    return bonus
    }
    fun highestDefensiveBonus(p: Client): Int {
        var bonus = 0
        for (i in 5..7) {
            if (p.playerBonus[i] > bonus)
                bonus = p.playerBonus[i]
        }
            return bonus
        }
fun landHit(p: Client, t: Entity): Boolean {
    val hitChance: Double
    val chance = Misc.chance(100_000) / 1_000
    val prayerBonus = PrayerCombatService.meleeAttackMultiplier(p.asSkillPlayer())
    if(t is Client) { //Pvp
        var atkLevel = p.getLevel(Skill.ATTACK)
        val atkBonus = highestAttackBonus(p)
        var defLevel = t.getLevel(Skill.DEFENCE)
        val defBonus = highestDefensiveBonus(t)
        val prayerDefBonus = PrayerCombatService.defenceMultiplier(t.asSkillPlayer())
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        if(t.fightType == 1) defLevel += 3
        if(p.fightType == 3) atkLevel += 1
        if(t.fightType == 3) defLevel += 1
        /* Calculations */
        val playerDef = (defLevel * (defBonus + 64.0)) * prayerDefBonus
        val playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        hitChance = if (playerAccuracy > playerDef)
            1 - ((playerDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (playerDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    } else if(t is Npc) { //Pve
        val atkBonus = highestAttackBonus(p)
        var atkLevel = p.getLevel(Skill.ATTACK)
        val defLevel = t.defence
        val defBonus = 0.0
        val npcDef = (defLevel + 9) * (defBonus + 64.0)
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        if(p.fightType == 3) atkLevel += 1
        /* Calculation */
        var playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        playerAccuracy = if(p.getSlayerDamage(t.id, false) == 1) playerAccuracy * 1.15
        else if(p.getSlayerDamage(t.id, false) == 2) playerAccuracy * 1.20 else playerAccuracy
        hitChance = if (playerAccuracy > npcDef)
            1 - ((npcDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (npcDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%%")
        return chance < (hitChance*100)
    }
    return true
}

fun Client.handleSpecial(crit: Boolean): Boolean {
    val emote = AttackAnimationService.resolve(this)
    if (specialActivated) {
        val weaponId = equipment[Equipment.Slot.WEAPON.id]
        return when {
            weaponId in setOf(1215, 1231, 5680, 5698) -> handleDdsSpec(crit)
            weaponId in setOf(11802, 81, 20368) -> handleAgsSpec(crit)
            weaponId in setOf(11804, 20370) -> handleBgsSpec(crit)
            weaponId in setOf(11806, 20372) -> handleSgsSpec(crit)
            weaponId in setOf(11808, 20374) -> handleZgsSpec(crit)
            weaponId in setOf(13652, 20784) -> handleClawsSpec(crit)
            weaponId in setOf(4153, 24225) -> handleGmaulSpec(crit)
            weaponId in setOf(1434) -> handleDragonMaceSpec(crit)
            weaponId in setOf(1377) -> handleDragonBaxeSpec(crit)
            weaponId in setOf(11838, 12809) -> handleSaradominSwordSpec(crit)
            weaponId in setOf(4587) -> handleDragonScimitarSpec(crit)
            weaponId in setOf(1305) -> handleDragonLongswordSpec(crit)
            weaponId in setOf(13576) -> handleDragonWarhammerSpec(crit)
            weaponId in setOf(1249, 5730) -> handleDragonSpearSpec(crit)
            weaponId in setOf(4151, 80, 21371, 15441, 15442, 15443, 15444) -> handleAbyssalWhipSpec(crit)
            weaponId in setOf(7158) -> handleDragon2hSpec(crit)
            weaponId in setOf(3204) -> handleDragonHalberdSpec(crit)
            weaponId in setOf(11791, 12904) -> handleStaffOfDeadSpec(crit)
            weaponId in setOf(6739, 13241, 20011) -> handleDragonAxeSpec(crit)
            weaponId in setOf(11920, 12797, 23677, 25376, 27695, 13243) -> handleDragonPickaxeSpec(crit)
            weaponId in setOf(21015) -> handleDinhsBulwarkSpec(crit)
            else -> {
                PlayerAnimationService.requestAttack(this, emote)
                false
            }
        }
    }

    val chance = Range(1, 8).value
    if (chance != 1 || hit == 0) {
        PlayerAnimationService.requestAttack(this, emote)
        return false
    }
    return false
}

private fun Client.handleDdsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.15).toInt()
    hit2 = (Utils.random(meleeMaxHit().toInt()) * 1.15).toInt()
    callGfxMask(252, 100)
    PlayerAnimationService.requestAttack(this, 1062)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 >= npc.currentHealth) hit2 = npc.currentHealth
        npc.dealDamage(this, hit2, Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 >= player.currentHealth) hit2 = player.currentHealth
        player.dealDamage(this, hit2, Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleAgsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.375).toInt()
    callGfxMask(1211, 100)
    PlayerAnimationService.requestAttack(this, 7644)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleBgsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.21).toInt()
    callGfxMask(1212, 100)
    PlayerAnimationService.requestAttack(this, 7642)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        val remaining = hit
        val skills = arrayOf(Skill.DEFENCE, Skill.STRENGTH, Skill.ATTACK, Skill.PRAYER, Skill.MAGIC, Skill.RANGED)
        for (skill in skills) {
            val current = player.getLevel(skill)
            val drain = remaining.coerceAtMost(current)
            player.setLevel((current - drain).coerceAtLeast(0), skill)
        }
    }
    return true
}

private fun Client.handleSgsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.1).toInt()
    callGfxMask(1209, 100)
    PlayerAnimationService.requestAttack(this, 7640)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    heal(hit, hit / 2)
    val prayerRestore = hit / 4
    val newPrayer = (currentPrayer + prayerRestore).coerceAtMost(getLevel(Skill.PRAYER))
    currentPrayer = newPrayer
    send(SendMessage("The SGS special restores some health and prayer."))
    return true
}

private fun Client.handleZgsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.1).toInt()
    callGfxMask(1210, 100)
    PlayerAnimationService.requestAttack(this, 7638)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit > 0 && player.stunTimer == 0) {
            player.stunTimer = 20
            player.send(SendMessage("You have been frozen!"))
            send(SendMessage("You freeze your opponent!"))
        }
    }
    return true
}

private fun Client.handleClawsSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    val maxHit = meleeMaxHit()
    val h1 = Utils.random((maxHit * 1.0).toInt())
    val h2: Int
    val h3: Int
    val h4: Int
    if (h1 > 0) {
        h2 = (h1 / 2).coerceAtLeast(0)
        h3 = (h2 / 2).coerceAtLeast(0)
        h4 = (h1 - h2 - h3).coerceAtLeast(0)
    } else {
        h2 = Utils.random((maxHit * 0.75).toInt())
        if (h2 > 0) {
            h3 = (h2 / 2).coerceAtLeast(0)
            h4 = h3
        } else {
            h3 = Utils.random((maxHit * 0.75).toInt())
            h4 = if (h3 > 0) h3 else Utils.random((maxHit * 0.75).toInt()).coerceAtLeast(1)
        }
    }
    hit = h1; hit2 = h2
    callGfxMask(1171, 100)
    PlayerAnimationService.requestAttack(this, 7527)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 > 0) {
            if (hit2 >= npc.currentHealth) hit2 = npc.currentHealth
            npc.dealDamage(this, hit2, Entity.hitType.STANDARD)
        }
        if (h3 > 0) {
            val h3d = if (h3 >= npc.currentHealth) npc.currentHealth else h3
            npc.dealDamage(this, h3d, Entity.hitType.STANDARD)
        }
        if (h4 > 0) {
            val h4d = if (h4 >= npc.currentHealth) npc.currentHealth else h4
            npc.dealDamage(this, h4d, Entity.hitType.STANDARD)
        }
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 > 0) {
            if (hit2 >= player.currentHealth) hit2 = player.currentHealth
            player.dealDamage(this, hit2, Entity.hitType.STANDARD)
        }
        if (h3 > 0) {
            player.dealDamage(this, h3.coerceAtMost(player.currentHealth), Entity.hitType.STANDARD)
        }
        if (h4 > 0) {
            player.dealDamage(this, h4.coerceAtMost(player.currentHealth), Entity.hitType.STANDARD)
        }
    }
    return true
}

private fun Client.handleGmaulSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    callGfxMask(340, 100)
    PlayerAnimationService.requestAttack(this, 1667)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    combatTimer = 0
    return true
}

private fun Client.handleDragonMaceSpec(crit: Boolean): Boolean {
    hit = (hit * 1.5).toInt()
    callGfxMask(251, 100)
    PlayerAnimationService.requestAttack(this, 1060)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragonBaxeSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    callGfxMask(246, 100)
    PlayerAnimationService.requestAttack(this, 1056)
    val strBoost = 10 + (getLevel(Skill.STRENGTH) / 4)
    setLevel(getLevel(Skill.STRENGTH) + strBoost, Skill.STRENGTH)
    setLevel((getLevel(Skill.ATTACK) * 0.9).toInt(), Skill.ATTACK)
    setLevel((getLevel(Skill.DEFENCE) * 0.9).toInt(), Skill.DEFENCE)
    setLevel((getLevel(Skill.RANGED) * 0.9).toInt(), Skill.RANGED)
    setLevel((getLevel(Skill.MAGIC) * 0.9).toInt(), Skill.MAGIC)
    send(SendMessage("Your strength has been boosted!"))
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleSaradominSwordSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    hit2 = Utils.random(16)
    callGfxMask(1213, 100)
    PlayerAnimationService.requestAttack(this, 1132)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 >= npc.currentHealth) hit2 = npc.currentHealth
        npc.dealDamage(this, hit2, Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        if (hit2 >= player.currentHealth) hit2 = player.currentHealth
        player.dealDamage(this, hit2, Entity.hitType.STANDARD)
        callGfxMask(1196, 0)
    }
    return true
}

private fun Client.handleDragonScimitarSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    callGfxMask(347, 100)
    PlayerAnimationService.requestAttack(this, 1872)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        PrayerCombatService.disableProtectionPrayers(player.asSkillPlayer())
    }
    return true
}

private fun Client.handleDragonLongswordSpec(crit: Boolean): Boolean {
    hit = (hit * 1.15).toInt()
    callGfxMask(248, 100)
    PlayerAnimationService.requestAttack(this, 1058)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragonWarhammerSpec(crit: Boolean): Boolean {
    hit = (hit * 1.5).toInt()
    callGfxMask(1292, 100)
    PlayerAnimationService.requestAttack(this, 1378)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        val defLevel = player.getLevel(Skill.DEFENCE)
        player.setLevel((defLevel * 0.7).toInt().coerceAtLeast(1), Skill.DEFENCE)
        player.send(SendMessage("Your defence has been drained!"))
    }
    return true
}

private fun Client.handleDragonSpearSpec(@Suppress("UNUSED_PARAMETER") crit: Boolean): Boolean {
    callGfxMask(253, 100)
    PlayerAnimationService.requestAttack(this, 1064)
    hit = -1
    if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (player.stunTimer == 0) {
            player.stunTimer = 3
            player.send(SendMessage("You have been stunned!"))
            send(SendMessage("You push your opponent back!"))
        }
    }
    return true
}

private fun Client.handleAbyssalWhipSpec(crit: Boolean): Boolean {
    hit = (hit * 1.0).toInt()
    callGfxMask(341, 100)
    PlayerAnimationService.requestAttack(this, 1658)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragon2hSpec(crit: Boolean): Boolean {
    callGfxMask(559, 100)
    PlayerAnimationService.requestAttack(this, 3157)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragonHalberdSpec(crit: Boolean): Boolean {
    callGfxMask(1172, 100)
    PlayerAnimationService.requestAttack(this, 1203)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        hit = (hit * 1.1).toInt()
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        hit2 = (Utils.random(meleeMaxHit().toInt()) * 1.1).toInt()
        if (hit2 >= npc.currentHealth) hit2 = npc.currentHealth
        npc.dealDamage(this, hit2, Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        hit = (hit * 1.1).toInt()
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleStaffOfDeadSpec(crit: Boolean): Boolean {
    callGfxMask(1228, 100)
    PlayerAnimationService.requestAttack(this, 1720)
    setLevel(125, Skill.DEFENCE)
    send(SendMessage("Your defence has been greatly boosted!"))
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragonAxeSpec(crit: Boolean): Boolean {
    callGfxMask(479, 100)
    PlayerAnimationService.requestAttack(this, 2876)
    setLevel(WoodcuttingCombatService.dragonAxeBoost(getLevel(Skill.WOODCUTTING)), Skill.WOODCUTTING)
    send(SendMessage("Your woodcutting level has been boosted!"))
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDragonPickaxeSpec(crit: Boolean): Boolean {
    PlayerAnimationService.requestAttack(this, 7138)
    setLevel(MiningCombatService.dragonPickaxeBoost(getLevel(Skill.MINING)), Skill.MINING)
    send(SendMessage("Your mining level has been boosted!"))
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}

private fun Client.handleDinhsBulwarkSpec(crit: Boolean): Boolean {
    callGfxMask(1336, 100)
    PlayerAnimationService.requestAttack(this, 7511)
    hit = (hit * 1.2).toInt()
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    } else if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return false
        if (hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if (crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
    }
    return true
}
