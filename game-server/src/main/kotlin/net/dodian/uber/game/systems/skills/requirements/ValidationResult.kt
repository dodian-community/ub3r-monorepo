package net.dodian.uber.game.systems.skills.requirements

typealias ValidationResult = net.dodian.uber.game.content.skills.core.requirements.ValidationResult

fun ValidationResult.failureMessageOrNull(): String? =
    when (this) {
        is net.dodian.uber.game.content.skills.core.requirements.ValidationResult.Failed -> this.message
        else -> null
    }
