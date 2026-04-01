package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.skills.ActionSpec
import net.dodian.uber.game.systems.skills.CycleSignal
import net.dodian.uber.game.systems.skills.GatheringActionBuilder
import net.dodian.uber.game.systems.skills.ProductionActionBuilder
import net.dodian.uber.game.systems.skills.RunningGatheringAction
import net.dodian.uber.game.systems.skills.RunningProductionAction

typealias ActionSpec = net.dodian.uber.game.systems.skills.ActionSpec
typealias CycleSignal = net.dodian.uber.game.systems.skills.CycleSignal
typealias RunningGatheringAction = net.dodian.uber.game.systems.skills.RunningGatheringAction
typealias RunningProductionAction = net.dodian.uber.game.systems.skills.RunningProductionAction
typealias GatheringActionBuilder = net.dodian.uber.game.systems.skills.GatheringActionBuilder
typealias ProductionActionBuilder = net.dodian.uber.game.systems.skills.ProductionActionBuilder

fun gatheringAction(name: String, block: GatheringActionBuilder.() -> Unit): GatheringActionBuilder {
    return net.dodian.uber.game.systems.skills.gatheringAction(name, block)
}

fun productionAction(name: String, block: ProductionActionBuilder.() -> Unit): ProductionActionBuilder {
    return net.dodian.uber.game.systems.skills.productionAction(name, block)
}

fun action(name: String, block: GatheringActionBuilder.() -> Unit): ActionSpec {
    return net.dodian.uber.game.systems.skills.action(name, block)
}
