package net.dodian.uber.event.impl

import net.dodian.uber.event.TypePlayerEvent

sealed class PlayerSessionEvent : TypePlayerEvent {
    object Initialize : PlayerSessionEvent()
    object Login : PlayerSessionEvent()
    object Logout : PlayerSessionEvent()
    object Finalize : PlayerSessionEvent()
}