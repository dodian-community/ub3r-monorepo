package net.dodian.uber.game.testutil

import org.junit.jupiter.api.Assumptions
import java.io.File

object RequiresReferenceMappings {
    fun assume() {
        Assumptions.assumeTrue(
            File("reference/cache-rev218").isDirectory,
            "Requires generated developer-reference mappings under game-server/reference/cache-rev218 " +
                "(run `./gradlew :game-server:generateRscm -PosrsDump=/path/to/osrs-dump`) - skipped without it (e.g. in CI)",
        )
    }
}
