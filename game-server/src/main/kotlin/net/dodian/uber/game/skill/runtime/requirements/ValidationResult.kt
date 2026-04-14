package net.dodian.uber.game.skill.runtime.requirements

sealed class ValidationResult {
    object Ok : ValidationResult()

    data class Failed(
        val message: String,
    ) : ValidationResult()

    companion object {
        @JvmStatic
        fun ok(): ValidationResult = Ok

        @JvmStatic
        fun failed(message: String): ValidationResult = Failed(message)
    }
}

fun ValidationResult.failureMessageOrNull(): String? =
    when (this) {
        is ValidationResult.Failed -> this.message
        else -> null
    }
