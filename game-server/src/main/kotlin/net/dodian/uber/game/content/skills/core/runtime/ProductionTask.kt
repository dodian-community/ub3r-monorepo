package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.core.requirements.ValidationResult

data class ProductionSpec(
    val actionName: String,
    val skillId: Int,
    val productId: Int,
    val amountPerCycle: Int,
    val primaryItemId: Int,
    val secondaryItemId: Int,
    val experiencePerUnit: Int,
    val animationId: Int,
    val tickDelay: Int,
)

abstract class ProductionTask(
    protected val client: Client,
    protected val spec: ProductionSpec,
) {
    open fun validateCycle(): ValidationResult = ValidationResult.ok()

    abstract fun performCycle(): Boolean

    fun runCycle(): Boolean {
        val validation = validateCycle()
        if (validation is ValidationResult.Failed) {
            client.sendFilterMessage(validation.message)
            return false
        }
        return performCycle()
    }
}
