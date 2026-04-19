package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

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
    private val overrides =
        objectOverrides {
            plane(0) {
                legacyDoorRemovals()
                nightmareZoneCleanup()
                taverleyDungeonEdits()
                yanilleAndTrainingEdits()
                homeAreaBoundaryEdits()
                slayerAndDungeonEdits()
                elementalObeliskEdits()
                desertEdits()
            }
        }

    @JvmStatic
    fun all(): List<StaticObjectOverride> = overrides

    @JvmStatic
    fun replayTo(viewer: Client) {
        for (override in overrides) {
            viewer.ReplaceObject2(
                override.position,
                override.replacementObjectId,
                override.replacementFace,
                override.replacementType,
            )
        }
    }

}

private fun objectOverrides(build: StaticObjectOverrideRegistry.() -> Unit): List<StaticObjectOverride> =
    StaticObjectOverrideRegistry().apply(build).build()

private fun removedDoor(x: Int, y: Int, z: Int, objectType: Int = 0): StaticObjectOverride =
    StaticObjectOverride(
        position = Position(x, y, z),
        replacementObjectId = -1,
        replacementFace = -1,
        replacementType = objectType,
    )

private fun removedObject(x: Int, y: Int, z: Int, face: Int, objectType: Int): StaticObjectOverride =
    StaticObjectOverride(
        position = Position(x, y, z),
        replacementObjectId = -1,
        replacementFace = face,
        replacementType = objectType,
    )

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

private fun removedRectangle(
    xRange: IntRange,
    yRange: IntRange,
    z: Int,
    face: Int = 0,
    objectType: Int,
): List<StaticObjectOverride> {
    val rows = ArrayList<StaticObjectOverride>(xRange.count() * yRange.count())
    for (x in xRange) {
        for (y in yRange) {
            rows += removedObject(x, y, z, face, objectType)
        }
    }
    return rows
}

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
    fun removeDoor(x: Int, y: Int, objectType: Int = 0) {
        sink += removedDoor(x, y, z, objectType)
    }

    fun removeObject(x: Int, y: Int, face: Int = 0, objectType: Int) {
        sink += removedObject(x, y, z, face, objectType)
    }

    fun replaceObject(x: Int, y: Int, newObjectId: Int, face: Int, objectType: Int) {
        sink += replacementObject(x, y, z, newObjectId, face, objectType)
    }

    fun removeRectangle(xRange: IntRange, yRange: IntRange, face: Int = 0, objectType: Int) {
        sink += removedRectangle(xRange, yRange, z, face, objectType)
    }

    fun legacyDoorRemovals() {
        // Legacy globally removed doors.
        removeDoor(2669, 2713, objectType = 11)
        removeDoor(2713, 3483)
        removeDoor(2716, 3472)
        removeDoor(2594, 3102)
        removeDoor(2816, 3438)
    }

    fun nightmareZoneCleanup() {
        // Nightmare Zone / boxed-off area cleanup.
        removeRectangle(2600..2609, 3111..3119, objectType = 10)
    }

    fun taverleyDungeonEdits() {
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

    fun yanilleAndTrainingEdits() {
        // Yanille and nearby training/custom-world edits.
        removeObject(2542, 3097, face = 0, objectType = 10)
        replaceObject(2572, 3105, newObjectId = 14890, face = 0, objectType = 10)
        replaceObject(2595, 3409, newObjectId = 133, face = -1, objectType = 10)
        replaceObject(2613, 3084, newObjectId = 3994, face = -3, objectType = 11)
        replaceObject(2626, 3116, newObjectId = 14905, face = -1, objectType = 11)
        replaceObject(2628, 3151, newObjectId = 2104, face = -3, objectType = 11)
        replaceObject(2629, 3151, newObjectId = 2105, face = -3, objectType = 11)
        replaceObject(2688, 3481, newObjectId = 27978, face = 1, objectType = 11)
        replaceObject(2733, 3374, newObjectId = 375, face = -1, objectType = 11)
        replaceObject(2942, 4688, newObjectId = 12260, face = 3, objectType = 10)
        replaceObject(2443, 5169, newObjectId = 2352, face = 0, objectType = 10)
    }

    fun homeAreaBoundaryEdits() {
        // Custom area boundary to keep new players out of unfinished space.
        replaceObject(2770, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2771, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3140, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3141, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3142, newObjectId = 2050, face = 0, objectType = 10)
        replaceObject(2772, 3143, newObjectId = 2050, face = 0, objectType = 10)
    }

    fun slayerAndDungeonEdits() {
        // Slayer / mining / dungeon object swaps.
        replaceObject(2492, 9916, newObjectId = 7491, face = 0, objectType = 10)
        replaceObject(2493, 9915, newObjectId = 7491, face = 0, objectType = 10)
        replaceObject(2661, 9815, newObjectId = 2391, face = 0, objectType = 0)
        replaceObject(2662, 9815, newObjectId = 2392, face = -2, objectType = 0)
        replaceObject(2904, 9678, newObjectId = 6951, face = 0, objectType = 10)
        replaceObject(2998, 3931, newObjectId = 6951, face = 0, objectType = 0)
    }

    fun elementalObeliskEdits() {
        // Altars and elemental obelisks.
        replaceObject(2743, 3174, newObjectId = 2152, face = 0, objectType = 10)
        replaceObject(2863, 3427, newObjectId = 2151, face = 0, objectType = 10)
        replaceObject(3059, 3564, newObjectId = 2153, face = 0, objectType = 10)
        replaceObject(3531, 3536, newObjectId = 2150, face = 0, objectType = 10)
    }

    fun desertEdits() {
        // Desert region object corrections.
        replaceObject(3283, 2809, newObjectId = 20391, face = 4, objectType = 0)
        replaceObject(3284, 2809, newObjectId = 20391, face = 2, objectType = 0)
    }
}

