package net.dodian.uber.game.events.skilling

import kotlin.reflect.KClass
import net.dodian.uber.game.events.GameEvent

object SkillEventCatalog {
    @JvmField
    val events: Set<KClass<out GameEvent>> =
        setOf(
            SkillActionStartEvent::class,
            SkillActionInterruptEvent::class,
            SkillActionCompleteEvent::class,
            SkillProgressAppliedEvent::class,
            SkillingActionStartedEvent::class,
            SkillingActionCycleEvent::class,
            SkillingActionSucceededEvent::class,
            SkillingActionStoppedEvent::class,
        )
}
