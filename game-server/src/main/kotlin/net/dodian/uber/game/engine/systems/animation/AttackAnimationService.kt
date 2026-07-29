package net.dodian.uber.game.engine.systems.animation

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.CombatStyle
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerCore as Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.Item

object AttackAnimationService {
    private const val PUNCH_ANIMATION = 422
    private const val KICK_ANIMATION = 423

    private const val WHIP_ATTACK = 1658

    private const val SCIMITAR_CHOP = 390
    private const val SCIMITAR_SLASH = 390
    private const val SCIMITAR_LUNGE = 386
    private const val SCIMITAR_BLOCK = 390

    private const val DAGGER_STAB = 412
    private const val DAGGER_LUNGE = 412
    private const val DAGGER_SLASH = 395
    private const val DAGGER_BLOCK = 412

    private const val SWORD_STAB = 412
    private const val SWORD_LUNGE = 412
    private const val SWORD_SLASH = 395
    private const val SWORD_BLOCK = 412

    private const val BATTLEAXE_CHOP = 401
    private const val BATTLEAXE_HACK = 401
    private const val BATTLEAXE_SMASH = 401
    private const val BATTLEAXE_BLOCK = 401

    private const val TWO_H_CHOP = 407
    private const val TWO_H_SLASH = 407
    private const val TWO_H_SMASH = 406
    private const val TWO_H_BLOCK = 407

    private const val GODSWORD_CHOP = 7046
    private const val GODSWORD_SLASH = 7045
    private const val GODSWORD_SMASH = 7054
    private const val GODSWORD_BLOCK = 7055

    private const val WARHAMMER_ATTACK = 401

    private const val SPEAR_LUNGE = 2080
    private const val SPEAR_SWIPE = 2081
    private const val SPEAR_POUND = 2080
    private const val SPEAR_BLOCK = 2082

    private const val HALBERD_ATTACK = 440

    private const val STAFF_ATTACK = 401
    private const val STAFF_POUND = 406
    private const val STAFF_BLOCK = 406

    private const val MACE_POUND = 401
    private const val MACE_PUMMEL = 401
    private const val MACE_SPIKE = 400
    private const val MACE_BLOCK = 401

    private const val PICKAXE_ATTACK = 400
    private const val PICKAXE_SMASH = 401
    private const val PICKAXE_BLOCK = 400

    private const val AXE_ATTACK = 395

    private const val CLAWS_ATTACK = 393

    private const val SCYTHE_REAP = 414
    private const val SCYTHE_CHOP = 382
    private const val SCYTHE_JAB = 2066
    private const val SCYTHE_BLOCK = 382

    private const val BOW_ATTACK = 426
    private const val CROSSBOW_ATTACK = 4230
    private const val THROWN_ATTACK = 929

    @JvmStatic
    fun resolve(player: Client): Int {
        if (player.playerNpc >= 0) {
            val npcData = Server.npcManager.getData(player.playerNpc)
            if (npcData != null && npcData.attackEmote > 0) {
                return npcData.attackEmote
            }
        }
        val weaponId = player.equipment[Equipment.Slot.WEAPON.id]
        return resolveWeapon(weaponId, player.weaponStyle, player.combatStyle)
    }

    fun resolveWeapon(weaponId: Int, weaponStyle: Player.fightStyle, combatStyle: CombatStyle): Int {
        if (weaponId <= 0) {
            return when (combatStyle) {
                CombatStyle.AGGRESSIVE_MELEE -> KICK_ANIMATION
                else -> PUNCH_ANIMATION
            }
        }

        val item = Server.itemManager.items[weaponId] ?: return when (combatStyle) {
            CombatStyle.AGGRESSIVE_MELEE -> KICK_ANIMATION
            else -> PUNCH_ANIMATION
        }

        val jsonAnim = resolveJsonAttackAnim(item, weaponStyle)
        if (jsonAnim > 0) return jsonAnim

        val weaponType = item.weaponType
        val nameLower = item.getName().lowercase()

        return resolveDefault(weaponStyle, weaponType, nameLower)
            ?: resolveFromName(nameLower, weaponStyle)
            ?: when (combatStyle) {
                CombatStyle.AGGRESSIVE_MELEE -> KICK_ANIMATION
                else -> PUNCH_ANIMATION
            }
    }

    private fun resolveJsonAttackAnim(item: Item, weaponStyle: Player.fightStyle): Int {
        val anims = item.attackAnimations ?: return 0
        return anims.getOrNull(weaponStyle.ordinal) ?: 0
    }

    private fun resolveDefault(
        weaponStyle: Player.fightStyle,
        weaponType: String,
        nameLower: String,
    ): Int? {
        return when (weaponType) {
            "whip" -> resolveWhip(weaponStyle)
            "slash_sword" -> resolveSlashSword(weaponStyle, nameLower)
            "stab_sword" -> resolveStabSword(weaponStyle, nameLower)
            "axe", "weapon" -> resolveBattleaxe(weaponStyle)
            "2h_sword" -> resolve2h(weaponStyle)
            "godsword" -> resolveGodsword(weaponStyle)
            "blunt" -> WARHAMMER_ATTACK
            "spear" -> resolveSpear(weaponStyle)
            "polearm" -> HALBERD_ATTACK
            "staff", "powered_staff" -> resolveStaff(weaponStyle)
            "spiked" -> resolveMace(weaponStyle)
            "pickaxe" -> resolvePickaxe(weaponStyle)
            "claw" -> CLAWS_ATTACK
            "scythe" -> resolveScythe(weaponStyle)
            "bludgeon" -> resolve2h(weaponStyle)
            "bow" -> BOW_ATTACK
            "crossbow" -> CROSSBOW_ATTACK
            "thrown" -> THROWN_ATTACK
            else -> null
        }
    }

    private fun resolveFromName(nameLower: String, weaponStyle: Player.fightStyle): Int? {
        return when {
            nameLower.contains("whip") -> resolveWhip(weaponStyle)
            nameLower.contains("scimitar") || nameLower.contains("longsword") -> resolveSlashSword(weaponStyle, nameLower)
            nameLower.contains("dagger") || nameLower.contains("sword") -> resolveStabSword(weaponStyle, nameLower)
            nameLower.contains("battleaxe") || (nameLower.contains("axe") && !nameLower.contains("pick")) -> resolveBattleaxe(weaponStyle)
            nameLower.contains("godsword") -> resolveGodsword(weaponStyle)
            nameLower.contains("2h") -> resolve2h(weaponStyle)
            nameLower.contains("maul") || nameLower.contains("warhammer") || nameLower.contains("hammer") -> WARHAMMER_ATTACK
            nameLower.contains("spear") || nameLower.contains("halberd") -> resolveSpear(weaponStyle)
            nameLower.contains("halberd") -> HALBERD_ATTACK
            nameLower.contains("staff") || nameLower.contains("wand") -> resolveStaff(weaponStyle)
            nameLower.contains("mace") || nameLower.contains("flail") -> resolveMace(weaponStyle)
            nameLower.contains("pickaxe") -> resolvePickaxe(weaponStyle)
            nameLower.contains("claws") || nameLower.contains("claw") -> CLAWS_ATTACK
            nameLower.contains("scythe") -> resolveScythe(weaponStyle)
            nameLower.contains("axe") -> AXE_ATTACK
            nameLower.contains("bow") -> BOW_ATTACK
            nameLower.contains("crossbow") -> CROSSBOW_ATTACK
            nameLower.contains("dart") || nameLower.contains("knife") || nameLower.contains("javelin") || nameLower.contains("thrownaxe") -> THROWN_ATTACK
            else -> null
        }
    }

    private fun resolveWhip(@Suppress("UNUSED_PARAMETER") style: Player.fightStyle): Int = WHIP_ATTACK

    private fun resolveSlashSword(style: Player.fightStyle, @Suppress("UNUSED_PARAMETER") nameLower: String): Int = when (style) {
        Player.fightStyle.CHOP -> SCIMITAR_CHOP
        Player.fightStyle.SLASH -> SCIMITAR_SLASH
        Player.fightStyle.CONTROLLED, Player.fightStyle.LUNGE -> SCIMITAR_LUNGE
        Player.fightStyle.BLOCK -> SCIMITAR_BLOCK
        else -> SCIMITAR_CHOP
    }

    private fun resolveStabSword(style: Player.fightStyle, nameLower: String): Int = when (style) {
        Player.fightStyle.STAB -> if (nameLower.contains("dagger")) DAGGER_STAB else SWORD_STAB
        Player.fightStyle.LUNGE_STR -> if (nameLower.contains("dagger")) DAGGER_LUNGE else SWORD_LUNGE
        Player.fightStyle.SLASH -> if (nameLower.contains("dagger")) DAGGER_SLASH else SWORD_SLASH
        Player.fightStyle.BLOCK -> if (nameLower.contains("dagger")) DAGGER_BLOCK else SWORD_BLOCK
        else -> SWORD_STAB
    }

    private fun resolveBattleaxe(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.CHOP -> BATTLEAXE_CHOP
        Player.fightStyle.HACK -> BATTLEAXE_HACK
        Player.fightStyle.SMASH -> BATTLEAXE_SMASH
        Player.fightStyle.BLOCK -> BATTLEAXE_BLOCK
        else -> BATTLEAXE_CHOP
    }

    private fun resolve2h(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.CHOP -> TWO_H_CHOP
        Player.fightStyle.SLASH -> TWO_H_SLASH
        Player.fightStyle.SMASH -> TWO_H_SMASH
        Player.fightStyle.BLOCK -> TWO_H_BLOCK
        else -> TWO_H_CHOP
    }

    private fun resolveGodsword(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.CHOP -> GODSWORD_CHOP
        Player.fightStyle.SLASH -> GODSWORD_SLASH
        Player.fightStyle.SMASH -> GODSWORD_SMASH
        Player.fightStyle.BLOCK -> GODSWORD_BLOCK
        else -> GODSWORD_CHOP
    }

    private fun resolveSpear(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.LUNGE, Player.fightStyle.CONTROLLED -> SPEAR_LUNGE
        Player.fightStyle.SWIPE_CON -> SPEAR_SWIPE
        Player.fightStyle.POUND_CON -> SPEAR_POUND
        Player.fightStyle.BLOCK -> SPEAR_BLOCK
        else -> SPEAR_LUNGE
    }

    private fun resolveStaff(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.POUND -> STAFF_ATTACK
        Player.fightStyle.PUMMEL -> STAFF_POUND
        Player.fightStyle.BLOCK -> STAFF_BLOCK
        else -> STAFF_ATTACK
    }

    private fun resolveMace(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.POUND -> MACE_POUND
        Player.fightStyle.PUMMEL -> MACE_PUMMEL
        Player.fightStyle.SPIKE -> MACE_SPIKE
        Player.fightStyle.BLOCK -> MACE_BLOCK
        else -> MACE_POUND
    }

    private fun resolvePickaxe(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.SPIKE -> PICKAXE_ATTACK
        Player.fightStyle.IMPALE -> PICKAXE_ATTACK
        Player.fightStyle.SMASH -> PICKAXE_SMASH
        Player.fightStyle.BLOCK -> PICKAXE_BLOCK
        else -> PICKAXE_ATTACK
    }

    private fun resolveScythe(style: Player.fightStyle): Int = when (style) {
        Player.fightStyle.CHOP -> SCYTHE_CHOP
        Player.fightStyle.SLASH -> SCYTHE_CHOP
        Player.fightStyle.JAB -> SCYTHE_JAB
        Player.fightStyle.BLOCK -> SCYTHE_BLOCK
        else -> SCYTHE_REAP
    }
}

internal fun toAttackStyleKey(style: Player.fightStyle): String = when (style) {
    Player.fightStyle.PUNCH -> "UNARMED_PUNCH"
    Player.fightStyle.KICK -> "UNARMED_KICK"
    Player.fightStyle.BLOCK -> "UNARMED_BLOCK"
    Player.fightStyle.STAB -> "SWORD_STAB"
    Player.fightStyle.LUNGE_STR -> "SWORD_LUNGE"
    Player.fightStyle.SLASH -> "SWORD_SLASH"
    Player.fightStyle.CONTROLLED -> "SCIMITAR_LUNGE"
    Player.fightStyle.CHOP -> "SCIMITAR_CHOP"
    Player.fightStyle.LUNGE -> "SCIMITAR_LUNGE"
    Player.fightStyle.HACK -> "BATTLEAXE_HACK"
    Player.fightStyle.SMASH -> "BATTLEAXE_SMASH"
    Player.fightStyle.POUND -> "WARHAMMER_POUND"
    Player.fightStyle.PUMMEL -> "WARHAMMER_PUMMEL"
    Player.fightStyle.SPIKE -> "MACE_SPIKE"
    Player.fightStyle.JAB -> "SCYTHE_JAB"
    Player.fightStyle.SWIPE -> "HALBERD_SWIPE"
    Player.fightStyle.FEND -> "HALBERD_FEND"
    Player.fightStyle.IMPALE -> "PICKAXE_IMPALE"
    Player.fightStyle.FLICK -> "WHIP_FLICK"
    Player.fightStyle.LASH -> "WHIP_LASH"
    Player.fightStyle.DEFLECT -> "WHIP_DEFLECT"
    Player.fightStyle.SWIPE_CON -> "SPEAR_SWIPE"
    Player.fightStyle.POUND_CON -> "SPEAR_POUND"
    else -> ""
}
