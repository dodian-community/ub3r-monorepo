package net.dodian.uber.game.api.plugin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentPlatformCatalogTest {
    @AfterEach
    fun resetFeatureState() = ContentModuleFeatureState.setDisabledForTests(emptySet())

    @Test
    fun `catalog fingerprint is deterministic independent of publication order`() {
        val first = manifest("content.alpha")
        val second = manifest("content.beta")
        ContentPlatformCatalog.publish(listOf(second, first))
        val fingerprint = ContentPlatformCatalog.snapshot().fingerprint

        ContentPlatformCatalog.publish(listOf(first, second))
        assertEquals(fingerprint, ContentPlatformCatalog.snapshot().fingerprint)
    }

    @Test
    fun `disabled module is present in catalog but omitted from active count`() {
        ContentModuleFeatureState.setDisabledForTests(setOf("content.beta"))
        ContentPlatformCatalog.publish(listOf(manifest("content.alpha"), manifest("content.beta")))

        val snapshot = ContentPlatformCatalog.snapshot()
        assertEquals(1, snapshot.enabledCount)
        assertEquals(1, snapshot.disabledCount)
        assertFalse("content.beta" in snapshot.enabledModuleIds)
    }

    @Test
    fun `manifest rejects unstable identifiers and duplicate module ids`() {
        assertThrows(IllegalArgumentException::class.java) { manifest("Bad Id") }
        assertThrows(IllegalArgumentException::class.java) {
            ContentPlatformCatalog.publish(listOf(manifest("content.same"), manifest("content.same")))
        }
    }

    @Test
    fun `module feature state leaves always enabled modules active`() {
        ContentModuleFeatureState.setDisabledForTests(setOf("content.alpha"))
        assertTrue(ContentModuleFeatureState.isEnabled(manifest("content.always", ContentModuleManifest.ALWAYS_ENABLED)))
    }

    private fun manifest(id: String, featureFlag: String = id) = ContentModuleManifest(
        id = id,
        owner = "test",
        version = "1.0.0",
        featureFlag = featureFlag,
    )
}
