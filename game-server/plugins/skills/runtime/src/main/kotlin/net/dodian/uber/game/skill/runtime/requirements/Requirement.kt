package net.dodian.uber.game.skill.runtime.requirements

import kotlin.math.abs
import net.dodian.uber.game.api.plugin.skills.*
import net.dodian.uber.game.model.player.skills.Skill

typealias Requirement = SkillRequirement
typealias ValidationResult = SkillValidationResult
fun ValidationResult.failureMessageOrNull(): String? = (this as? SkillValidationResult.Failed)?.message
enum class ItemPossession { INVENTORY, EQUIPPED, ANY }

class RequirementBuilder {
    private val requirements = ArrayList<Requirement>()
    fun requirement(requirement: Requirement) { requirements += requirement }
    fun level(skill: Skill, minimumLevel: Int, message: String? = null) { requirements += HasLevelRequirement(skill, minimumLevel, message) }
    fun baseLevel(skill: Skill, minimumLevel: Int, message: String? = null) { requirements += HasBaseLevelRequirement(skill, minimumLevel, message) }
    fun boostedLevel(skill: Skill, minimumLevel: Int, message: String? = null) { requirements += HasBoostedLevelRequirement(skill, minimumLevel, message) }
    fun item(itemId: Int, amount: Int = 1, message: String? = null) { requirements += HasItemRequirement(itemId, amount, message) }
    fun item(itemId: Int, amount: Int = 1, message: String? = null, possession: ItemPossession = ItemPossession.INVENTORY, consume: Boolean = false) { requirements += AdvancedItemRequirement(itemId, amount, message, possession, consume) }
    fun inventorySpace(slots: Int = 1, message: String? = null) { requirements += HasInventorySpaceRequirement(slots, message) }
    fun freeSlots(slots: Int = 1, message: String? = null) = inventorySpace(slots, message)
    fun nearPosition(position: SkillPosition, maxDistance: Int, message: String? = null) { requirements += NearPositionRequirement(position, maxDistance, message) }
    fun tool(skill: Skill, toolIdsByTier: List<Int>, requiredLevelByTool: Map<Int, Int>, message: String) { requirements += ToolRequirement(skill, toolIdsByTier, requiredLevelByTool, message) }
    fun build(): List<Requirement> = requirements.toList()
}
private fun level(current: Int, skill: Skill, min: Int, message: String?) = if (current >= min) ValidationResult.ok() else ValidationResult.failed(message ?: "You need a ${skill.name.lowercase()} level of $min.")
class HasLevelRequirement(private val skill: Skill, private val min: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer) = level(player.skills.current(skill), skill, min, message) }
class HasBaseLevelRequirement(private val skill: Skill, private val min: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer) = level(player.skills.base(skill), skill, min, message) }
class HasBoostedLevelRequirement(private val skill: Skill, private val min: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer) = level(player.skills.current(skill), skill, min, message) }
class HasItemRequirement(private val id: Int, private val amount: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer) = if (player.inventory.contains(id, amount)) ValidationResult.ok() else ValidationResult.failed(message ?: "You need ${player.inventory.itemName(id).lowercase()} x$amount.") }
class HasInventorySpaceRequirement(private val slots: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer) = if (player.inventory.freeSlots() >= slots) ValidationResult.ok() else ValidationResult.failed(message ?: "Your inventory is full!") }
class NearPositionRequirement(private val anchor: SkillPosition, private val distance: Int, private val message: String? = null) : Requirement { override fun validate(player: SkillPlayer): ValidationResult { val p = player.world.position; return if (p.z == anchor.z && abs(p.x-anchor.x)+abs(p.y-anchor.y) <= distance) ValidationResult.ok() else ValidationResult.failed(message ?: "You moved too far away.") } }
class NearObjectRequirement(position: SkillPosition, distance: Int, message: String? = null) : Requirement by NearPositionRequirement(position, distance, message)
class ToolRequirement(private val skill: Skill, private val tools: List<Int>, private val levels: Map<Int, Int>, private val message: String) : Requirement { override fun validate(player: SkillPlayer): ValidationResult { val level = player.skills.current(skill); return if (tools.any { level >= (levels[it] ?: 1) && (player.equipment.item(SkillEquipmentSlot.WEAPON) == it || player.inventory.contains(it)) }) ValidationResult.ok() else ValidationResult.failed(message) } }
class AdvancedItemRequirement(private val id: Int, private val amount: Int, private val message: String? = null, private val possession: ItemPossession = ItemPossession.INVENTORY, private val consume: Boolean = false) : Requirement {
    private fun slot(player: SkillPlayer) = (0..13).firstOrNull { player.equipment.item(it) == id } ?: -1
    override fun validate(player: SkillPlayer): ValidationResult { val inventory = player.inventory.contains(id, amount); val equipped = slot(player) >= 0 && amount == 1; if (when(possession){ItemPossession.INVENTORY->inventory;ItemPossession.EQUIPPED->equipped;ItemPossession.ANY->inventory||equipped}) return ValidationResult.ok(); return ValidationResult.failed(message ?: "You need ${player.inventory.itemName(id).lowercase()}.") }
    override fun execute(player: SkillPlayer) { if (!consume) return; val slot = slot(player); when { possession == ItemPossession.EQUIPPED && slot >= 0 -> player.equipment.remove(slot,id,amount); player.inventory.contains(id,amount) -> player.inventory.transaction { remove(id,amount) }; possession == ItemPossession.ANY && slot >= 0 -> player.equipment.remove(slot,id,amount) } }
}
