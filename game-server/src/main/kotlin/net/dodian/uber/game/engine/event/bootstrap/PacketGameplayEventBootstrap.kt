package net.dodian.uber.game.engine.event.bootstrap

/**
 * Superseded — all event wiring has been split into focused per-event bootstrap objects
 * (ItemDropBootstrap, ItemExamineBootstrap, NpcExamineBootstrap, ObjectExamineBootstrap,
 * DialogueContinueBootstrap) that KSP auto-discovers.
 *
 * This object is kept as an empty stub so any existing callers compile without change.
 */
@Suppress("unused")
object PacketGameplayEventWiring {
    @JvmStatic
    fun bootstrap() {
        // no-op: individual Bootstrap objects are auto-discovered by KSP
    }
}
