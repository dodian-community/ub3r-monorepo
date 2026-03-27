package net.dodian.uber.game.content.skills.core.requirements

import kotlin.math.abs
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill

fun interface Requirement {
    fun validate(client: Client): ValidationResult
}

class RequirementBuilder {
    private val requirements = ArrayList<Requirement>()

    fun requirement(requirement: Requirement) {
        requirements += requirement
    }

    fun level(skill: Skill, minimumLevel: Int, message: String? = null) {
        requirements += HasLevelRequirement(skill, minimumLevel, message)
    }

    fun item(itemId: Int, amount: Int = 1, message: String? = null) {
        requirements += HasItemRequirement(itemId, amount, message)
    }

    fun inventorySpace(slots: Int = 1, message: String? = null) {
        requirements += HasInventorySpaceRequirement(slots, message)
    }

    fun nearPosition(position: Position, maxDistance: Int, message: String? = null) {
        requirements += NearPositionRequirement(position, maxDistance, message)
    }

    fun tool(
        skill: Skill,
        toolIdsByTier: List<Int>,
        requiredLevelByTool: Map<Int, Int>,
        message: String,
    ) {
        requirements += ToolRequirement(skill, toolIdsByTier, requiredLevelByTool, message)
    }

    fun build(): List<Requirement> = requirements
}

class HasLevelRequirement(
    private val skill: Skill,
    private val minimumLevel: Int,
    private val overrideMessage: String? = null,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        return if (client.getLevel(skill) >= minimumLevel) {
            ValidationResult.ok()
        } else {
            ValidationResult.failed(overrideMessage ?: "You need a ${skill.name.lowercase()} level of $minimumLevel.")
        }
    }
}

class HasItemRequirement(
    private val itemId: Int,
    private val amount: Int,
    private val overrideMessage: String? = null,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        return if (client.playerHasItem(itemId, amount)) {
            ValidationResult.ok()
        } else {
            val defaultMessage = "You need ${client.GetItemName(itemId).lowercase()} x$amount."
            ValidationResult.failed(overrideMessage ?: defaultMessage)
        }
    }
}

class HasInventorySpaceRequirement(
    private val slots: Int,
    private val overrideMessage: String? = null,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        return if (client.freeSlots() >= slots) {
            ValidationResult.ok()
        } else {
            ValidationResult.failed(overrideMessage ?: "Your inventory is full!")
        }
    }
}

class NearPositionRequirement(
    private val anchor: Position,
    private val maxDistance: Int,
    private val overrideMessage: String? = null,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        if (client.position.z != anchor.z) {
            return ValidationResult.failed(overrideMessage ?: "You moved too far away.")
        }
        val dx = abs(client.position.x - anchor.x)
        val dy = abs(client.position.y - anchor.y)
        return if (dx + dy <= maxDistance) {
            ValidationResult.ok()
        } else {
            ValidationResult.failed(overrideMessage ?: "You moved too far away.")
        }
    }
}

class NearObjectRequirement(
    private val objectPosition: Position,
    private val maxDistance: Int,
    private val overrideMessage: String? = null,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        return NearPositionRequirement(objectPosition, maxDistance, overrideMessage).validate(client)
    }
}

class ToolRequirement(
    private val skill: Skill,
    private val toolIdsByTier: List<Int>,
    private val requiredLevelByTool: Map<Int, Int>,
    private val missingToolMessage: String,
) : Requirement {
    override fun validate(client: Client): ValidationResult {
        val skillLevel = client.getLevel(skill)
        val equippedWeapon = client.equipment[Equipment.Slot.WEAPON.id]
        val hasTool =
            toolIdsByTier.any { toolId ->
                val required = requiredLevelByTool[toolId] ?: 1
                skillLevel >= required && (equippedWeapon == toolId || client.playerHasItem(toolId))
            }
        return if (hasTool) {
            ValidationResult.ok()
        } else {
            ValidationResult.failed(missingToolMessage)
        }
    }
}
