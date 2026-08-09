package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.TomlRemovedObjectLoader

/**
 * Authoritative overrides for static cache objects that this server intentionally
 * removes or replaces globally.
 *
 * These must be applied both to client visuals and to startup collision so the
 * server never blocks tiles for objects the client no longer sees.
 */
data class StaticObjectOverride(
    val position: Position,
    val replacementObjectId: Int,
    val replacementFace: Int,
    val replacementType: Int,
)

object StaticObjectOverrides {
    private val overrides: List<StaticObjectOverride> by lazy {
        val built = objectOverrides {
            plane(0) {
                taverleyDungeon()
                yanilleAndCustom()
                homeBoundary()
                slayerDungeons()
                obelisks()
                desert()
            }
        }
        built + tomlRemovedOverrides()
    }

    @JvmStatic
    fun all(): List<StaticObjectOverride> = overrides

    @JvmStatic
    fun tomlRemovedOverrides(): List<StaticObjectOverride> =
        TomlRemovedObjectLoader.load().flatMap { entry ->
            val position = Position(entry.x, entry.y, entry.z)
            val cachedObjects = net.dodian.uber.game.engine.systems.cache.CacheCollisionAuditStore.objectsForTile(entry.x, entry.y)
                .filter { !it.skipped && it.x == entry.x && it.y == entry.y && it.plane == entry.z && (entry.type == null || it.type == entry.type) }

            if (cachedObjects.isNotEmpty()) {
                cachedObjects.map { obj ->
                    StaticObjectOverride(
                        position = position,
                        replacementObjectId = -1,
                        replacementFace = -1,
                        replacementType = obj.type,
                    )
                }
            } else {
                listOf(
                    StaticObjectOverride(
                        position = position,
                        replacementObjectId = -1,
                        replacementFace = -1,
                        replacementType = entry.type ?: 0,
                    )
                )
            }
        }

    // Half-width of the client's loaded map region (104 tiles wide, centered on the viewer), so a
    // player can't walk into an already-loaded area and see the original, un-overridden cache
    // object before the override reaches them. Was previously 32, which left objects 33-52 tiles
    // out unreplayed on region entry.
    private const val LOADED_MAP_HALF_WIDTH = 52

    @JvmStatic
    fun replayTo(viewer: Client) {
        val pos = viewer.position ?: return
        for (override in overrides) {
            val oPos = override.position
            if (oPos.z != pos.z) continue
            if (kotlin.math.abs(oPos.x - pos.x) > LOADED_MAP_HALF_WIDTH || kotlin.math.abs(oPos.y - pos.y) > LOADED_MAP_HALF_WIDTH) continue
            viewer.ReplaceObject2(
                oPos,
                override.replacementObjectId,
                override.replacementFace,
                override.replacementType,
            )
        }
    }

}

private fun objectOverrides(build: StaticObjectOverrideRegistry.() -> Unit): List<StaticObjectOverride> =
    StaticObjectOverrideRegistry().apply(build).build()

private fun replacementObject(
    x: Int,
    y: Int,
    z: Int,
    newObjectId: Int,
    face: Int,
    objectType: Int,
): StaticObjectOverride =
    StaticObjectOverride(
        position = Position(x, y, z),
        replacementObjectId = newObjectId,
        replacementFace = face,
        replacementType = objectType,
    )

private class StaticObjectOverrideRegistry {
    private val overrides = mutableListOf<StaticObjectOverride>()

    fun plane(z: Int, build: PlaneStaticObjectOverrideBuilder.() -> Unit) {
        PlaneStaticObjectOverrideBuilder(z, overrides).apply(build)
    }

    fun build(): List<StaticObjectOverride> = overrides.toList()
}

private class PlaneStaticObjectOverrideBuilder(
    private val z: Int,
    private val sink: MutableList<StaticObjectOverride>,
) {
    fun replaceObject(x: Int, y: Int, newObjectId: Int, face: Int, objectType: Int) {
        sink += replacementObject(x, y, z, newObjectId, face, objectType)
    }

    fun taverleyDungeon() {
        // Taverley dungeon brick walls and shortcuts.
        replaceObject(2869, 9813, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2870, 9813, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2871, 9813, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2866, 9797, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2866, 9798, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2866, 9799, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2866, 9800, newObjectId = 2343, face = 0, objectType = 10)
        replaceObject(2885, 9794, newObjectId = 882, face = 0, objectType = 10)
        replaceObject(2899, 9728, newObjectId = 882, face = 0, objectType = 10)
    }

    fun yanilleAndCustom() {
        // Yanille and custom-world object replacements.
        replaceObject(2572, 3105, newObjectId = 14890, face = 0, objectType = 10)
        replaceObject(2595, 3409, newObjectId = 133, face = -1, objectType = 10)
        replaceObject(2613, 3084, newObjectId = 3994, face = -3, objectType = 11)
        replaceObject(2626, 3116, newObjectId = 2460, face = -1, objectType = 11)
        replaceObject(2628, 3151, newObjectId = 2104, face = -3, objectType = 11)
        replaceObject(2629, 3151, newObjectId = 2105, face = -3, objectType = 11)
        replaceObject(2688, 3481, newObjectId = 27978, face = 1, objectType = 11)
        replaceObject(2733, 3374, newObjectId = 375, face = -1, objectType = 11)
        replaceObject(2942, 4688, newObjectId = 12260, face = 3, objectType = 10)
        replaceObject(2443, 5169, newObjectId = 2352, face = 0, objectType = 10)
        replaceObject(2627, 3117, newObjectId = 34768, face = 0, objectType = 10)
    }

    fun homeBoundary() {
        // Boundary wall to keep players out of unfinished space.
        replaceObject(2770, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2771, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3141, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3142, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3143, newObjectId = 2050, face = 0, objectType = 10)
    }

    fun slayerDungeons() {
        // Slayer tower and dungeon object swaps.
        replaceObject(2492, 9916, newObjectId = 7491, face = 0, objectType = 10)
        replaceObject(2493, 9915, newObjectId = 7491, face = 0, objectType = 10)
        replaceObject(2661, 9815, newObjectId = 2391, face = 0, objectType = 0)
        replaceObject(2662, 9815, newObjectId = 2392, face = -2, objectType = 0)
        replaceObject(2904, 9678, newObjectId = 6951, face = 0, objectType = 10)
        replaceObject(2998, 3931, newObjectId = 6951, face = 0, objectType = 0)
    }

    fun obelisks() {
        // Elemental obelisks and altars.
        replaceObject(2743, 3174, newObjectId = 2152, face = 0, objectType = 10)
        replaceObject(2863, 3427, newObjectId = 2151, face = 0, objectType = 10)
        replaceObject(3059, 3564, newObjectId = 2153, face = 0, objectType = 10)
        replaceObject(3531, 3536, newObjectId = 2150, face = 0, objectType = 10)
    }

    fun desert() {
        // Desert region object corrections.
        replaceObject(3283, 2809, newObjectId = 20391, face = 4, objectType = 0)
        replaceObject(3284, 2809, newObjectId = 20391, face = 2, objectType = 0)
    }
}
