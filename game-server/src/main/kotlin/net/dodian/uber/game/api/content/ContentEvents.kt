package net.dodian.uber.game.api.content

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.GameEvent

/**
 * Content-facing event facade.
 *
 * Keeps content modules on the `api.content` surface instead of importing engine internals directly.
 */
object ContentEvents {
	@JvmStatic
	fun <E : GameEvent> post(event: E) {
		GameEventBus.post(event)
	}

	@JvmStatic
	fun <E : GameEvent> postWithResult(event: E): Boolean = GameEventBus.postWithResult(event)

	@JvmStatic
	fun <E : GameEvent, T> postAndReturn(event: E): List<T> = GameEventBus.postAndReturn(event)

	@JvmStatic
	fun <E : GameEvent> on(
		clazz: Class<E>,
		condition: (E) -> Boolean = { true },
		otherwiseAction: (E) -> Unit = {},
		action: (E) -> Boolean,
	) {
		GameEventBus.on(clazz, condition, otherwiseAction, action)
	}

	@JvmStatic
	fun <E : GameEvent, T> onReturnable(
		clazz: Class<E>,
		condition: (E) -> Boolean = { true },
		otherwiseAction: (E) -> Unit = {},
		action: (E) -> T?,
	) {
		GameEventBus.onReturnable(clazz, condition, otherwiseAction, action)
	}

	@JvmStatic
	fun <E : GameEvent> addFilter(clazz: Class<E>, filter: (E) -> Boolean) {
		GameEventBus.addFilter(clazz, filter)
	}
}

