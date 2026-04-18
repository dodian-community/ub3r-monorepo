@file:Suppress("UNCHECKED_CAST")

package net.dodian.uber.game.engine.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.engine.event.bootstrap.CoreEventBusBootstrap
import net.dodian.uber.game.engine.metrics.EventDispatchTelemetry
import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.events.skilling.SkillActionCompleteEvent
import net.dodian.uber.game.events.skilling.SkillActionInterruptEvent
import net.dodian.uber.game.events.skilling.SkillActionStartEvent
import net.dodian.uber.game.events.skilling.SkillProgressAppliedEvent
import net.dodian.uber.game.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.events.skilling.SkillingActionSucceededEvent
import org.slf4j.LoggerFactory

object GameEventBus {
    private val logger = LoggerFactory.getLogger(GameEventBus::class.java)
    private val listeners = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventListener<out GameEvent>>>()
    private val returnableListeners =
        ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<ReturnableEventListener<out GameEvent, out Any>>>()
    private val filters = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventFilter<out GameEvent>>>()
    private val listenerFingerprints = ConcurrentHashMap.newKeySet<String>()
    private val returnableFingerprints = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var bootstrapped = false

    @JvmStatic
    fun bootstrap() {
        synchronized(this) {
            val alreadyBootstrapped = bootstrapped
            EventDispatchTelemetry.recordBootstrapInvocation(alreadyBootstrapped, CoreEventBusBootstrap.bootstrapCount())
            if (alreadyBootstrapped) {
                return
            }
            CoreEventBusBootstrap.bootstrap()
            bootstrapped = true
        }
    }

    @JvmStatic
    fun <E : GameEvent> post(event: E) {
        if (!hasSubscribers(event.javaClass)) {
            EventDispatchTelemetry.recordMissingSubscriber(event.javaClass.simpleName)
        }
        try {
            processListeners(event, listeners[event.javaClass])
            processReturnable(event, returnableListeners[event.javaClass])
        } catch (exception: RuntimeException) {
            EventDispatchTelemetry.recordDispatchException(event.javaClass.simpleName)
            logger.error(
                "Event bus dispatch failed for {} tags={}",
                event.javaClass.name,
                eventMetadataTags(event),
                exception,
            )
        }
    }

    @JvmStatic
    fun <E : GameEvent> postWithResult(event: E): Boolean {
        var handled = false
        if (!hasSubscribers(event.javaClass)) {
            EventDispatchTelemetry.recordMissingSubscriber(event.javaClass.simpleName)
        }
        try {
            handled = processListeners(event, listeners[event.javaClass]) || handled
            handled = processReturnable(event, returnableListeners[event.javaClass]) || handled
        } catch (exception: RuntimeException) {
            EventDispatchTelemetry.recordDispatchException(event.javaClass.simpleName)
            logger.error(
                "Event bus dispatch failed for {} tags={}",
                event.javaClass.name,
                eventMetadataTags(event),
                exception,
            )
        }
        return handled
    }

    @JvmStatic
    fun <E : GameEvent, T> postAndReturn(event: E): List<T> {
        val results = ArrayList<T>()
        if (!hasSubscribers(event.javaClass)) {
            EventDispatchTelemetry.recordMissingSubscriber(event.javaClass.simpleName)
        }
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
            EventDispatchTelemetry.recordDispatchException(event.javaClass.simpleName)
            logger.error(
                "Event bus return dispatch failed for {} tags={}",
                event.javaClass.name,
                eventMetadataTags(event),
                exception,
            )
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
        val fingerprint = "listener:${clazz.name}:${System.identityHashCode(listener)}"
        if (!listenerFingerprints.add(fingerprint)) {
            EventDispatchTelemetry.recordDuplicateListenerRegistration(clazz.simpleName)
            return
        }
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
        val fingerprint = "returnable:${clazz.name}:${System.identityHashCode(listener)}"
        if (!returnableFingerprints.add(fingerprint)) {
            EventDispatchTelemetry.recordDuplicateReturnableRegistration(clazz.simpleName)
            return
        }
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
        listenerFingerprints.clear()
        returnableFingerprints.clear()
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

    private fun hasSubscribers(clazz: Class<out GameEvent>): Boolean {
        val standard = listeners[clazz]
        if (standard != null && standard.isNotEmpty()) {
            return true
        }
        val returnable = returnableListeners[clazz]
        return returnable != null && returnable.isNotEmpty()
    }

    private fun eventMetadataTags(event: GameEvent): Map<String, String> =
        when (event) {
            is SkillActionStartEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName)
            is SkillActionInterruptEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName, "reason" to event.reason.name)
            is SkillActionCompleteEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName)
            is SkillProgressAppliedEvent -> mapOf("client" to event.client.playerName, "skill" to event.skill.name, "mode" to event.mode.name)
            is SkillingActionStartedEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName)
            is SkillingActionCycleEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName)
            is SkillingActionSucceededEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName)
            is SkillingActionStoppedEvent -> mapOf("client" to event.client.playerName, "actionName" to event.actionName, "reason" to event.reason.name)
            else -> emptyMap()
        }
}
