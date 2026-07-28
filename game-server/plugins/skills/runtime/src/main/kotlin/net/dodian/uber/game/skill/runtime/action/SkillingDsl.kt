package net.dodian.uber.game.skill.runtime.action

import net.dodian.uber.game.api.plugin.skills.*
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.skill.runtime.requirements.Requirement
import net.dodian.uber.game.skill.runtime.requirements.RequirementBuilder

typealias ActionSpec = SkillActionSpec
typealias CycleSignal = SkillCycleSignal
data class RunningGatheringAction internal constructor(private val handle: SkillActionHandle) : SkillActionHandle { override fun cancel(reason: ActionStopReason) = handle.cancel(reason) }
data class RunningProductionAction internal constructor(private val action: RunningGatheringAction) : SkillActionHandle { override fun cancel(reason: ActionStopReason) = action.cancel(reason) }
class GatheringActionBuilder internal constructor(private val name: String) {
    private var delay: SkillPlayer.()->Int = { 1 }; private val requirements = ArrayList<Requirement>(); private var start:(SkillPlayer.()->Unit)?=null; private var cycle:(SkillPlayer.()->CycleSignal)?=null; private var success:(SkillPlayer.()->Unit)?=null; private var stop:(SkillPlayer.(ActionStopReason)->Unit)?=null; private var priority = TaskPriority.STANDARD
    fun delay(ticks:Int){delay={ticks.coerceAtLeast(1)}}; fun delay(calculator:SkillPlayer.()->Int){delay={calculator().coerceAtLeast(1)}}; fun priority(value:TaskPriority){priority=value}; fun requirements(block:RequirementBuilder.()->Unit){requirements+=RequirementBuilder().apply(block).build()}; fun onStart(block:SkillPlayer.()->Unit){start=block}; fun onCycle(block:SkillPlayer.()->Unit){cycle={block();CycleSignal.success()}}; fun onCycleWhile(block:SkillPlayer.()->Boolean){cycle={if(block()) CycleSignal.success() else CycleSignal.stop()}}; fun onCycleSignal(block:SkillPlayer.()->CycleSignal){cycle=block}; fun onSuccess(block:SkillPlayer.()->Unit){success=block}; fun onStop(block:SkillPlayer.(ActionStopReason)->Unit){stop=block}
    fun build()=ActionSpec(name,delay,requirements.toList(),priority,start,cycle,success,stop)
    fun start(player:SkillPlayer,beforeStart:()->Unit={}):RunningGatheringAction?=player.actions.queue(build(),beforeStart)?.let(::RunningGatheringAction)
}
fun gatheringAction(name:String,block:GatheringActionBuilder.()->Unit)=GatheringActionBuilder(name).apply(block)
class ProductionActionBuilder internal constructor(name:String){private val gathering=GatheringActionBuilder(name); fun delay(ticks:Int)=gathering.delay(ticks);fun delay(calculator:SkillPlayer.()->Int)=gathering.delay(calculator);fun priority(value:TaskPriority)=gathering.priority(value);fun requirements(block:RequirementBuilder.()->Unit)=gathering.requirements(block);fun onStart(block:SkillPlayer.()->Unit)=gathering.onStart(block);fun onCycle(block:SkillPlayer.()->Unit)=gathering.onCycle(block);fun onCycleWhile(block:SkillPlayer.()->Boolean)=gathering.onCycleWhile(block);fun onCycleSignal(block:SkillPlayer.()->CycleSignal)=gathering.onCycleSignal(block);fun onSuccess(block:SkillPlayer.()->Unit)=gathering.onSuccess(block);fun onStop(block:SkillPlayer.(ActionStopReason)->Unit)=gathering.onStop(block);fun start(player:SkillPlayer,beforeStart:()->Unit={}):RunningProductionAction?=gathering.start(player,beforeStart)?.let(::RunningProductionAction)}
fun productionAction(name:String,block:ProductionActionBuilder.()->Unit)=ProductionActionBuilder(name).apply(block)
fun action(name:String,block:GatheringActionBuilder.()->Unit):ActionSpec=GatheringActionBuilder(name).apply(block).build()
