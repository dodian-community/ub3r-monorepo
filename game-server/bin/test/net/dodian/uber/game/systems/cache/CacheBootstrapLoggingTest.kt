package net.dodian.uber.game.systems.cache

import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class CacheBootstrapLoggingTest {
    @Test
    fun `bootstrap logs cache decode summary and rebuilds runtime collision`() {
        val cacheDir = Files.createTempDirectory("cache-bootstrap-logging")
        CollisionManager.global().clear()
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 1000,
                    name = "Test blocking object",
                    description = "fixture",
                    sizeX = 1,
                    sizeY = 1,
                    solid = true,
                    interactive = true,
                ),
            ),
        )
        CacheStoreTestFixtures.writeRegionArchives(
            root = cacheDir,
            regionId = 42,
            landscapeArchiveId = 43,
            objectArchiveId = 44,
            landscapeArchive =
                CacheStoreTestFixtures.createLandscapeArchive(
                    mapOf(CacheStoreTestFixtures.TileCoordinate(x = 1, y = 2, plane = 0) to byteArrayOf(50, 82.toByte(), 0)),
                ),
            objectArchive =
                CacheStoreTestFixtures.createObjectArchive(
                    listOf(
                        CacheStoreTestFixtures.FixtureMapObject(
                            id = 1000,
                            x = 1,
                            y = 2,
                            plane = 0,
                            type = 10,
                            rotation = 3,
                        ),
                    ),
                ),
        )

        val logger = LogManager.getLogger(CacheBootstrapService::class.java) as Logger
        val previousLevel = logger.level
        val previousAdditive = logger.isAdditive
        val appender = CapturingAppender()

        appender.start()
        logger.addAppender(appender)
        logger.level = Level.INFO
        logger.isAdditive = false

        try {
            CacheBootstrapService(cacheDir).bootstrap()
        } finally {
            logger.removeAppender(appender)
            logger.level = previousLevel
            logger.isAdditive = previousAdditive
            appender.stop()
        }

        val summaryMessage = appender.events
            .map { it.message.formattedMessage }
            .firstOrNull {
                it.contains("Cache decode complete: regions=1, tiles=16384, objects=1, blockingObjects=1, walkableObjects=0")
            }

        val collisionReadyMessage = appender.events
            .map { it.message.formattedMessage }
            .firstOrNull {
                it.contains("World collision ready from decoded cache: regions=1, tiles=16384, objects=1")
            }

        assertTrue(summaryMessage != null, "Expected cache decode summary log line, got: ${appender.events.map { it.message.formattedMessage }}")
        assertTrue(collisionReadyMessage != null, "Expected collision-ready log line, got: ${appender.events.map { it.message.formattedMessage }}")
                assertFalse(CollisionManager.global().canMove(1, 2689, 1, 2690, 0, 1, 1))
                CollisionManager.global().clear()
    }

    private class CapturingAppender : AbstractAppender(
        "cache-bootstrap-capture",
        null,
        PatternLayout.createDefaultLayout(),
        true,
        emptyArray<Property>(),
    ) {
        val events = mutableListOf<LogEvent>()

        override fun append(event: LogEvent) {
            events += event.toImmutable()
        }
    }
}
