package net.dodian.uber.game.testutil

import org.junit.jupiter.api.Assumptions
import java.io.File

object RequiresCache {
    fun assume() {
        Assumptions.assumeTrue(
            File("data/cache").isDirectory,
            "Requires the real OSRS binary cache under game-server/data/cache - skipped without it (e.g. in CI)",
        )
    }
}
