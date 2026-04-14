@file:Suppress("UNCHECKED_CAST")

package net.dodian.uber.game.engine.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.engine.event.bootstrap.CoreEventBusBootstrap
import net.dodian.uber.game.events.GameEvent
import org.slf4j.LoggerFactory

object GameEventBus {
    private val logger = LoggerFactory.getLogger(GameEventBus::class.java)
    private val listeners = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventListener<out GameEvent>>>()
    private val returnableListeners =
        ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<ReturnableEventListener<out GameEvent, out Any>>>()
    private val filters = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventFilter<out GameEvent>>>()

    @Volatile
    private var bootstrapped = false

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped) {
            return
        }
        CoreEventBusBootstrap.bootstrap()
        bootstrapped = true
    }

    @JvmStatic
    fun <E : GameEvent> post(event: E) {
        try {
            processListeners(event, listeners[event.javaClass])
            processReturnable(event, returnableListeners[event.javaClass])
        } catch (exception: RuntimeException) {
            logger.error("Event bus dispatch failed for {}", event.javaClass.name, exception)
        }
    }

    @JvmStatic
    fun <E : GameEvent> postWithResult(event: E): Boolean {
        var handled = false
        try {
            handled = processListeners(event, listeners[event.javaClass]) || handled
            handled = processReturnable(event, returnableListeners[event.javaClass]) || handled
        } catch (exception: RuntimeException) {
            logger.error("Event bus dispatch failed for {}", event.javaClass.name, exception)
        }
        return handled
    }

    @JvmStatic
    fun <E : GameEvent, T> postAndReturn(event: E): List<T> {
        val results = ArrayList<T>()
        try {
            if (!passesFilters(event)) {
                return emptyList()
            }
            returnableListeners[event.javaClass]?.forEach { raw ->
                val listener = raw as ReturnableEventListener<E, T>
                if (listener.condition(event)) {
                    listener.action(event)?.let { results += it }
                } else {
                    listener.otherwiseAction(event)
                }
            }
        } catch (exception: RuntimeException) {
            logger.error("Event bus return dispatch failed for {}", event.javaClass.name, exception)
        }
        return results
    }

    inline fun <reified E : GameEvent> on(
        noinline condition: (E) -> Boolean = { true },
        noinline otherwiseAction: (E) -> Unit = {},
        noinline action: (E) -> Boolean,
    ) {
        on(E::class.java, EventListener(condition, action, otherwiseAction))
    }

    @JvmStatic
    fun <E : GameEvent> on(clazz: Class<E>, listener: EventListener<E>) {
        listeners.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(listener)
    }

    @JvmStatic
    fun <E : GameEvent> on(
        clazz: Class<E>,
        condition: (E) -> Boolean = { true },
        otherwiseAction: (E) -> Unit = {},
        action: (E) -> Boolean,
    ) {
        on(clazz, EventListener(condition, action, otherwiseAction))
    }

    inline fun <reified E : GameEvent, T> onReturnable(
        noinline condition: (E) -> Boolean = { true },
        noinline otherwiseAction: (E) -> Unit = {},
        noinline action: (E) -> T?,
    ) {
        onReturnable(E::class.java, ReturnableEventListener(condition, action, otherwiseAction))
    }

    @JvmStatic
    fun <E : GameEvent, T> onReturnable(clazz: Class<E>, listener: ReturnableEventListener<E, T>) {
        returnableListeners.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(listener as ReturnableEventListener<out GameEvent, out Any>)
    }

    @JvmStatic
    fun <E : GameEvent, T> onReturnable(
        clazz: Class<E>,
        condition: (E) -> Boolean = { true },
        otherwiseAction: (E) -> Unit = {},
        action: (E) -> T?,
    ) {
        onReturnable(clazz, ReturnableEventListener(condition, action, otherwiseAction))
    }

    inline fun <reified E : GameEvent> addFilter(noinline filter: (E) -> Boolean) {
        addFilter(E::class.java, EventFilter(filter))
    }

    @JvmStatic
    fun <E : GameEvent> addFilter(clazz: Class<E>, filter: EventFilter<E>) {
        filters.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(filter)
    }

    @JvmStatic
    fun <E : GameEvent> addFilter(clazz: Class<E>, filter: (E) -> Boolean) {
        addFilter(clazz, EventFilter(filter))
    }

    @JvmStatic
    fun clear() {
        listeners.clear()
        returnableListeners.clear()
        filters.clear()
        bootstrapped = false
    }

    private fun <E : GameEvent> processListeners(
        event: E,
        eventListeners: List<EventListener<out GameEvent>>?,
    ): Boolean {
        if (!passesFilters(event)) {
            return false
        }
        var handled = false
        eventListeners?.forEach { raw ->
            val listener = raw as EventListener<E>
            if (listener.condition(event)) {
                handled = listener.action(event) || handled
            } else {
                listener.otherwiseAction(event)
            }
        }
        return handled
    }

    private fun <E : GameEvent> processReturnable(
        event: E,
        eventListeners: List<ReturnableEventListener<out GameEvent, out Any>>?,
    ): Boolean {
        if (!passesFilters(event)) {
            return false
        }
        var handled = false
        eventListeners?.forEach { raw ->
            val listener = raw as ReturnableEventListener<E, Any>
            if (listener.condition(event)) {
                if (listener.action(event) != null) {
                    handled = true
                }
            } else {
                listener.otherwiseAction(event)
            }
        }
        return handled
    }

    private fun <E : GameEvent> passesFilters(event: E): Boolean {
        return filters[event.javaClass]?.all { (it as EventFilter<E>).test(event) } ?: true
    }
}
