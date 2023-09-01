package net.dodian.uber.event

class EventList<T> {

    private val _unbound: MutableList<Event<T>> = mutableListOf()
    private val _keyed: MutableList<CachedKeyedEvent<T>> = mutableListOf()

    val unbound: List<Event<T>> get() = _unbound
    val keyed: List<CachedKeyedEvent<T>> get() = _keyed

    fun publishAll(parameter: T, eventBus: EventBus) {
        _unbound.forEach { event -> eventBus.publish(parameter, event) }
        _keyed.forEach { (id, event) -> eventBus.publish(id, parameter, event) }
    }

    fun clear() {
        _unbound.clear()
        _keyed.clear()
    }

    fun add(id: Long, event: KeyedEvent<T>) {
        _keyed += CachedKeyedEvent(id, event)
    }

    operator fun plusAssign(event: Event<T>) {
        _unbound += event
    }

    data class CachedKeyedEvent<T>(
        val id: Long,
        val event: KeyedEvent<T>
    )
}