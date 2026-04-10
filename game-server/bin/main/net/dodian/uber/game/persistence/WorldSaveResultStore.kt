package net.dodian.uber.game.persistence

import java.util.concurrent.atomic.AtomicReference
import net.dodian.uber.game.persistence.world.WorldPollResult

/** Holds the latest result returned from the DB after a world save cycle. */
object WorldSaveResultStore {
    private val latest = AtomicReference(WorldPollResult.EMPTY)

    @JvmStatic
    fun latest(): WorldPollResult = latest.get()

    @JvmStatic
    fun publish(result: WorldPollResult?) {
        if (result != null) {
            latest.set(result)
        }
    }
}
