package net.dodian.uber.game.systems.skills

typealias GatheringActionBuilder = net.dodian.uber.game.content.skills.core.runtime.GatheringActionBuilder
typealias ProductionActionBuilder = net.dodian.uber.game.content.skills.core.runtime.ProductionActionBuilder

fun gatheringAction(name: String, block: GatheringActionBuilder.() -> Unit): GatheringActionBuilder =
    net.dodian.uber.game.content.skills.core.runtime.gatheringAction(name, block)

fun productionAction(name: String, block: ProductionActionBuilder.() -> Unit): ProductionActionBuilder =
    net.dodian.uber.game.content.skills.core.runtime.productionAction(name, block)
