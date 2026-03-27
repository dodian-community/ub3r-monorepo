package net.dodian.uber.game.runtime.api.content

/**
 * Content-facing runtime API entrypoint.
 *
 * Runtime internals under `runtime.*` are implementation details and can change as
 * the engine evolves. Content modules should prefer this package for common operations
 * (actions, interaction throttles/policies, timing, and safety checks).
 *
 * This keeps content code focused on gameplay behavior while allowing internal runtime
 * subsystems to be reorganized with minimal churn.
 */
object ContentRuntimeApi
