package net.dodian.uber.event.impl

import net.dodian.uber.event.TypeGameEvent

sealed class GameProcessEvent : TypeGameEvent {
    object BootUp : GameProcessEvent()
    object StartCycle : GameProcessEvent()
    object EndCycle : GameProcessEvent()
}
