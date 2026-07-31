package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.engine.systems.cache.CacheCollisionAuditStore
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import org.junit.jupiter.api.Test

/** Temporary diagnostic - not part of the suite, reverted after use. */
class TempFarmingPart3ScanTest : RealWorldSkillTest() {
    @Test
    fun scan() {
        val targets = mapOf(
            "tree" to (8388..8391).toList() + listOf(19147),
            "fruit_tree" to (7962..7965).toList(),
            "bush" to (7577..7580).toList(),
        )
        val targetIds = targets.values.flatten().toSet()
        val found = mutableMapOf<Int, MutableList<Triple<Int, Int, Int>>>() // objectId -> (x,y,plane)
        for (regionId in CacheCollisionAuditStore.regionIdsForTests()) {
            for (obj in CacheCollisionAuditStore.objectsForRegion(regionId)) {
                if (obj.objectId in targetIds && obj.skippedReason == null) {
                    found.getOrPut(obj.objectId) { mutableListOf() }.add(Triple(obj.x, obj.y, obj.effectivePlane))
                }
            }
        }
        for ((category, ids) in targets) {
            println("=== $category ===")
            for (id in ids) {
                val positions = found[id].orEmpty()
                println("  $id: ${positions.size} hits -> ${positions.take(20)}")
            }
        }
    }
}
