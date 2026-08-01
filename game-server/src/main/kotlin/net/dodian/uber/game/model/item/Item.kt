package net.dodian.uber.game.model.item

import net.dodian.uber.game.item.ItemDefBase
import net.dodian.uber.game.item.ItemDefJson
import net.dodian.uber.game.item.ItemEquipmentDef
import net.dodian.uber.game.item.ItemRequirement
import net.dodian.uber.game.item.ItemWeaponDef
import net.dodian.uber.game.item.ItemWeaponStance
import net.dodian.uber.game.item.EquipmentAppearanceType
import net.dodian.uber.game.item.toRequiredArray
import net.dodian.uber.game.model.entity.player.PlayerCore as Player
import net.dodian.uber.game.engine.systems.animation.AttackAnimationService
import net.dodian.uber.game.engine.systems.animation.toAttackStyleKey

class Item(
    val id: Int,
    private val name: String,
    val slot: Int,
    private val standAnim: Int,
    private val walkAnim: Int,
    private val runAnim: Int,
    private val attackAnim: Int,
    private val shopSellValue: Int,
    private val shopBuyValue: Int,
    private val bonuses: IntArray,
    stackable: Boolean,
    noted: Boolean = false,
    placeholder: Boolean = false,
    noteable: Boolean = false,
    tradeable: Boolean = true,
    twoHanded: Boolean = false,
    full: Boolean = false,
    mask: Boolean = false,
    premium: Boolean = false,
    val examine: String,
    private val alchemy: Int,
    val weight: Double = 0.0,
    val lowAlch: Int = 0,
    val linkedItemId: Int = 0,
    val linkedNotedId: Int = 0,
    val attackSpeed: Int = 4,
    val weaponType: String = "",
    val stances: Array<ItemWeaponStance> = emptyArray(),
    val requirements: IntArray = IntArray(23),
    val attackAnimations: IntArray? = null,
    val blockAnimation: Int = 0,
    val appearanceType: EquipmentAppearanceType = EquipmentAppearanceType.DEFAULT,
) {
    private val flags: Int = run {
        var f = 0
        if (stackable) f = f or FLAG_STACKABLE
        if (noted) f = f or FLAG_NOTED
        if (placeholder) f = f or FLAG_PLACEHOLDER
        if (noteable) f = f or FLAG_NOTEABLE
        if (tradeable) f = f or FLAG_TRADEABLE
        if (twoHanded) f = f or FLAG_TWO_HANDED
        if (full) f = f or FLAG_FULL
        if (mask) f = f or FLAG_MASK
        if (premium) f = f or FLAG_PREMIUM
        f
    }

    fun getName(): String = name

    fun getStandAnim(): Int = standAnim

    fun getWalkAnim(): Int = walkAnim

    fun getRunAnim(): Int = runAnim

    fun getAttackAnim(): Int = attackAnim

    fun getShopSellValue(): Int = shopSellValue

    fun getShopBuyValue(): Int = shopBuyValue

    fun getAlchemy(): Int = alchemy

    fun getStackable(): Boolean = (flags and FLAG_STACKABLE) != 0

    fun getTradeable(): Boolean = (flags and FLAG_TRADEABLE) != 0

    fun getNoteable(): Boolean = (flags and FLAG_NOTEABLE) != 0

    fun isNoted(): Boolean = (flags and FLAG_NOTED) != 0

    fun isPlaceholder(): Boolean = (flags and FLAG_PLACEHOLDER) != 0

    fun getPremium(): Boolean = (flags and FLAG_PREMIUM) != 0

    fun getTwoHanded(): Boolean = (flags and FLAG_TWO_HANDED) != 0

    fun getBonuses(): IntArray = bonuses

    fun getDescription(): String = examine

    val full: Boolean get() = (flags and FLAG_FULL) != 0
    val mask: Boolean get() = (flags and FLAG_MASK) != 0

    override fun toString(): String =
        "$name ($id); slot $slot; standAnim $standAnim; walkAnim $walkAnim; runAnim $runAnim; attackAnim $attackAnim"
    companion object {
        const val FLAG_STACKABLE = 1 shl 0
        const val FLAG_NOTED = 1 shl 1
        const val FLAG_PLACEHOLDER = 1 shl 2
        const val FLAG_NOTEABLE = 1 shl 3
        const val FLAG_TRADEABLE = 1 shl 4
        const val FLAG_TWO_HANDED = 1 shl 5
        const val FLAG_FULL = 1 shl 6
        const val FLAG_MASK = 1 shl 7
        const val FLAG_PREMIUM = 1 shl 8

        const val DEFAULT_ATTACK_ANIM = 806

        private val defaultStandAnim = 808
        private val defaultWalkAnim = 819
        private val defaultRunAnim = 824

        private val fullBodyNames = setOf(
            "platebody", "chainbody", "plate", "body", "chestplate",
            "hauberk", "robetop", "torso", "brassard", "cuirass", "garb", "jacket",
            "leathertop", "leatherbody", "d'hide body", "hardleather body",
            "top", "tunic", "traitor"
        )
        private val fullHelmNames =
            setOf("full helm", "med helm", "helm", "coif", "cowl", "hood")
        private val maskNames = setOf("mask", "nosepeg", "spectacles", "glasses")

        private val slotMapping = mapOf(
            "head" to 0,
            "cape" to 1,
            "neck" to 2,
            "weapon" to 3,
            "2h" to 3,
            "body" to 4,
            "shield" to 5,
            "legs" to 7,
            "hands" to 9,
            "feet" to 10,
            "ring" to 12,
            "ammo" to 13
        )

        fun slotFromName(slotName: String, isTwoHanded: Boolean): Int =
            slotMapping[slotName] ?: if (isTwoHanded) 3 else 3

        fun isTwoHandedFromSlot(slotName: String): Boolean = slotName == "2h"

        fun deriveFull(nameLower: String, slot: Int): Boolean =
            when {
                slot == 4 -> fullBodyNames.any { nameLower.contains(it) }
                slot == 0 -> fullHelmNames.any { nameLower.contains(it) }
                else -> false
            }

        fun deriveMask(nameLower: String, slot: Int): Boolean =
            slot == 0 && maskNames.any { nameLower.contains(it) }

        private fun weaponInterfaceToType(wi: String): String = when (wi.uppercase()) {
            "UNARMED" -> "unarmed"
            "DART", "KNIFE", "THROWNAXE", "BLOWPIPE" -> "thrown"
            "STAFF", "MAGIC_STAFF", "NIGHTMARE_STAFF", "TRIDENT", "SHADOW" -> "staff"
            "WARHAMMER", "GRANITE_MAUL", "ELDER_MAUL" -> "blunt"
            "SCYTHE", "VITUR" -> "scythe"
            "BATTLEAXE", "GREATAXE", "WHIP" -> "weapon"
            "CROSSBOW", "ZARYTE_CROSSBOW", "KARIL_CROSSBOW", "BALLISTA" -> "crossbow"
            "SHORTBOW", "LONGBOW", "DARK_BOW", "COMPOSITE_BOW" -> "bow"
            "DAGGER", "DRAGON_DAGGER", "SWORD", "GHRAZI_SWORD", "OSMUM_SWORD" -> "stab_sword"
            "SCIMITAR", "LONGSWORD", "SAELDOR_SWORD" -> "slash_sword"
            "MACE", "VIGGORA_MACE", "INQUISITOR_MACE", "FLAIL" -> "spiked"
            "SPEAR", "HUNTER_LANCE" -> "spear"
            "TWO_HANDED_SWORD", "SARADOMIN_SWORD" -> "2h_sword"
            "GODSWORD_SWORD" -> "godsword"
            "PICKAXE" -> "pickaxe"
            "CLAWS" -> "claw"
            "HALBERD" -> "polearm"
            "CHINCHOMPA" -> "chinchompas"
            "SALAMANDER" -> "salamander"
            "BULWARK" -> "bulwark"
            "TWO_HANDED" -> "bludgeon"
            "BLUDGEN" -> "bludgeon"
            else -> ""
        }

        fun fromDefs(
            base: ItemDefBase,
            json: ItemDefJson?,
            appearanceType: EquipmentAppearanceType = EquipmentAppearanceType.DEFAULT,
        ): Item {
            val itemName = json?.name ?: base.name
            val nameLower = itemName.lowercase()

            val isTwoHanded =
                base.twoHanded || json?.let { isTwoHandedFromSlot(it.equipment?.slot ?: "") } == true

            val effectiveSlot = when {
                json?.equipment != null -> slotFromName(json.equipment.slot, isTwoHanded)
                base.equipmentSlot != null -> slotFromName(base.equipmentSlot.lowercase(), isTwoHanded)
                else -> 0
            }

            val isPlaceholder = json?.placeholder == true
            val linkedItemId = if (isPlaceholder) 0 else (json?.effectiveUnnotedId ?: base.unnotedId ?: 0)
            val linkedNotedId = json?.effectiveNotedId ?: base.notedId ?: 0

            val isNoted = !isPlaceholder && (json?.noted == true || (linkedItemId > 0 && linkedItemId != base.id))
            val isNoteable = json?.noteable == true || (linkedNotedId > 0 && linkedNotedId != base.id)
            val stackable = (json?.stackable ?: base.stackable) || isNoted
            val tradeable = json?.tradeable ?: base.tradeable
            val premium = json?.members ?: false

            val highAlch = json?.highAlch ?: base.highAlch
            val lowAlchValue = json?.lowAlch ?: base.lowAlch
            val baseValue = json?.cost ?: base.baseValue
            val weight = json?.weight ?: base.weight
            val examine = json?.examine ?: "It's a $itemName."

            val standAnim = base.standAnimation ?: defaultStandAnim
            val walkAnim = base.walkAnimation ?: defaultWalkAnim
            val runAnim = base.runAnimation ?: defaultRunAnim

            val jsonAttackAnimations: Map<String, Int>? = base.attackAnimations
            val attackAnimationsArray = if (jsonAttackAnimations != null) {
                val styles = Player.fightStyle.VALUES
                val array = IntArray(styles.size)
                for (style in styles) {
                    val key = toAttackStyleKey(style)
                    val anim = jsonAttackAnimations[key] ?: 0
                    array[style.ordinal] = anim
                }
                array
            } else null

            val jsonBlockAnimation: Int = base.blockAnimation ?: 0

            val bonuses = when {
                json?.equipment != null -> json.equipment.toBonusArray()
                base.hasAnyBonus() -> base.toBonusArray()
                else -> IntArray(14)
            }

            val requirements = when {
                json?.equipment?.requirements != null -> json.equipment.requirements.toRequiredArray()
                else -> base.toRequirements().toRequiredArray()
            }

            val weaponType = json?.weapon?.weaponType ?: base.weaponInterface?.let { weaponInterfaceToType(it) } ?: ""
            val attackSpeed = json?.weapon?.attackSpeed ?: 4
            val stances = json?.weapon?.stances ?: emptyArray()

            val full = deriveFull(nameLower, effectiveSlot)
            val mask = deriveMask(nameLower, effectiveSlot)

            val derivedAppearanceType = if (effectiveSlot == 0) {
                val name = itemName.lowercase()
                when {
                    name.startsWith("hood") || name.endsWith("hood") -> EquipmentAppearanceType.FACE
                    name.contains("full helm") || name == "masori mask" || name == "masori mask (f)" || name == "gas mask" -> EquipmentAppearanceType.HELM
                    name.contains("bloodbark helm") -> EquipmentAppearanceType.HAT
                    name.contains("med helm") -> EquipmentAppearanceType.FACE
                    name.contains("helm") -> {
                        if (name.startsWith("dharok")) EquipmentAppearanceType.FACE else EquipmentAppearanceType.HELM
                    }
                    name.contains("mask") -> EquipmentAppearanceType.MASK
                    else -> appearanceType
                }
            } else {
                appearanceType
            }

            return Item(
                id = base.id,
                name = itemName,
                slot = effectiveSlot,
                standAnim = standAnim,
                walkAnim = walkAnim,
                runAnim = runAnim,
                attackAnim = DEFAULT_ATTACK_ANIM,
                shopSellValue = baseValue,
                shopBuyValue = baseValue,
                bonuses = bonuses,
                stackable = stackable,
                noted = isNoted,
                placeholder = json?.placeholder ?: false,
                noteable = isNoteable,
                tradeable = tradeable,
                twoHanded = isTwoHanded,
                full = full,
                mask = mask,
                premium = premium,
                examine = examine,
                alchemy = highAlch,
                weight = weight,
                lowAlch = lowAlchValue,
                linkedItemId = linkedItemId,
                linkedNotedId = linkedNotedId,
                attackSpeed = attackSpeed,
                weaponType = weaponType,
                stances = stances,
                requirements = requirements,
                attackAnimations = attackAnimationsArray,
                blockAnimation = jsonBlockAnimation,
                appearanceType = derivedAppearanceType,
            )
        }
    }
}
