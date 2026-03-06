package net.dodian.uber.game.runtime.sync.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerChunkActivityIndexTest {

    @Test
    fun `clear keeps chunk activity stamps monotonic across ticks`() {
        val viewer =
            Client(EmbeddedChannel(), 1).apply {
                loaded = true
                isActive = true
                moveTo(3200, 3200, 0)
            }
        val index = PlayerChunkActivityIndex()

        try {
            index.bump(viewer.position)
            val firstStamp = index.maxChunkActivityStampFor(viewer, 16)

            index.clear()
            index.bump(viewer.position)
            val secondStamp = index.maxChunkActivityStampFor(viewer, 16)

            assertTrue(firstStamp > 0L)
            assertTrue(secondStamp > firstStamp)
        } finally {
            (viewer.channel as EmbeddedChannel).finishAndReleaseAll()
        }
    }
}
