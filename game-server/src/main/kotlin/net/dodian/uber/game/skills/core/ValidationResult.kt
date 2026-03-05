package net.dodian.uber.game.skills.core

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
