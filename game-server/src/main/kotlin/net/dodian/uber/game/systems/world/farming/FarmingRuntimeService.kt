package net.dodian.uber.game.systems.world.farming

@Deprecated(
    message = "Use net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService",
    replaceWith = ReplaceWith("net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService"),
)
class FarmingRuntimeService :
    net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService() {
    companion object {
        @JvmField
        val INSTANCE: net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService =
            net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService.INSTANCE
    }
}
