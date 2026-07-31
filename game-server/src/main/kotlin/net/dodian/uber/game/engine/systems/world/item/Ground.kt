package net.dodian.uber.game.engine.systems.world.item

import java.util.concurrent.ConcurrentHashMap
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem

object Ground {
    @JvmField
    val ground_items: MutableSet<GroundItem> = ConcurrentHashMap.newKeySet()

    @JvmField
    val untradeable_items: MutableSet<GroundItem> = ConcurrentHashMap.newKeySet()

    @JvmField
    val tradeable_items: MutableSet<GroundItem> = ConcurrentHashMap.newKeySet()

    // Exact-tile index mirroring the three flat lists above, so findGroundItem - called on every
    // pickup attempt - only ever scans however many items are stacked on one tile, instead of
    // linear-scanning every ground item in the world. Kept in lockstep by addItem/deleteItem.
    private val byTile = HashMap<Long, MutableList<GroundItem>>()

    private fun tileKey(x: Int, y: Int, z: Int): Long =
        (x.toLong() shl 34) or (y.toLong() shl 4) or z.toLong()

    private fun indexAdd(item: GroundItem) {
        byTile.computeIfAbsent(tileKey(item.x, item.y, item.z)) { ArrayList(2) }.add(item)
    }

    private fun indexRemove(item: GroundItem) {
        val key = tileKey(item.x, item.y, item.z)
        val bucket = byTile[key] ?: return
        bucket.remove(item)
        if (bucket.isEmpty()) {
            byTile.remove(key)
        }
    }

    @JvmStatic
    fun deleteItem(item: GroundItem) {
        when (item.type) {
            0 -> {
                item.setTaken(true)
                item.visible = false
                item.removeItemDisplay()
                // Intentionally not removed from ground_items/byTile here - matches the existing
                // "hidden, not deleted" semantics for static/global ground items (see addItem's
                // replace-on-respawn path below).
            }

            1 -> {
                item.setTaken(true)
                item.removeItemDisplay()
                untradeable_items.remove(item)
                indexRemove(item)
            }

            else -> {
                item.setTaken(true)
                item.visible = false
                item.removeItemDisplay()
                tradeable_items.remove(item)
                indexRemove(item)
            }
        }
    }

    @JvmStatic
    fun addItem(item: GroundItem) {
        when (item.type) {
            0 -> {
                val existing = ground_items.find { it == item }
                if (existing == null) {
                    ground_items.add(item)
                    indexAdd(item)
                } else {
                    ground_items.remove(existing)
                    ground_items.add(item)
                    if (existing !== item) {
                        indexRemove(existing)
                        indexAdd(item)
                    }
                }
                item.itemDisplay()
            }

            1 -> {
                untradeable_items.add(item)
                indexAdd(item)
            }
            else -> {
                tradeable_items.add(item)
                indexAdd(item)
            }
        }
    }

    @JvmStatic
    fun isTracked(item: GroundItem?): Boolean {
        if (item == null) {
            return false
        }
        return when (item.type) {
            0 -> ground_items.contains(item)
            1 -> untradeable_items.contains(item)
            else -> tradeable_items.contains(item)
        }
    }

    @JvmStatic
    fun canPickup(client: Client?, item: GroundItem?): Boolean {
        if (client == null || item == null || item.isTaken() || client.position.z != item.z) {
            return false
        }
        return when (item.type) {
            0 -> item.isVisible()
            1 -> client.dbId == item.playerId
            else -> item.isVisible() || client.dbId == item.playerId
        }
    }

    @JvmStatic
    fun tryClaimPickup(client: Client?, item: GroundItem?): Boolean {
        if (client == null || item == null) {
            return false
        }
        synchronized(item) {
            if (!isTracked(item) || item.isTaken() || !canPickup(client, item)) {
                return false
            }
            item.setTaken(true)
            return true
        }
    }

    @JvmStatic
    fun releaseClaim(item: GroundItem?) {
        if (item == null) {
            return
        }
        synchronized(item) {
            item.setTaken(false)
        }
    }

    /**
     * Scans only the items stacked on tile ([x], [y], [z]) - typically zero to a handful - instead
     * of every ground item in the world. [matchesCategory] mirrors the exact 0/1/else type
     * branching addItem/deleteItem already use (static / untradeable / tradeable).
     */
    private fun findGroundItem(
        matchesCategory: (Int) -> Boolean,
        client: Client?,
        id: Int,
        x: Int,
        y: Int,
        z: Int,
    ): GroundItem? {
        val bucket = byTile[tileKey(x, y, z)] ?: return null
        for (item in bucket) {
            if (!matchesCategory(item.type) || item.id != id || item.isTaken()) {
                continue
            }
            if (client != null && !canPickup(client, item)) {
                continue
            }
            return item
        }
        return null
    }

    @JvmStatic
    fun findGroundItem(client: Client?, id: Int, x: Int, y: Int, z: Int): GroundItem? {
        val staticItem = findGroundItem({ it == 0 }, client, id, x, y, z)
        if (staticItem != null) {
            return staticItem
        }
        return if (!Server.itemManager.isTradable(id)) {
            findGroundItem({ it == 1 }, client, id, x, y, z)
        } else {
            findGroundItem({ it != 0 && it != 1 }, client, id, x, y, z)
        }
    }

    @JvmStatic
    fun findGroundItem(id: Int, x: Int, y: Int, z: Int): GroundItem? =
        findGroundItem(null, id, x, y, z)

    @JvmStatic
    fun addGroundItem(pos: Position, id: Int, amount: Int, time: Int) {
        addItem(GroundItem(pos, id, amount, time, true))
    }

    @JvmStatic
    fun addFloorItem(c: Client, id: Int, amount: Int) {
        addItem(GroundItem(c.position, intArrayOf(c.slot, id, amount, 500)))
    }

    @JvmStatic
    fun addFloorItem(c: Client, pos: Position, id: Int, amount: Int, time: Int) {
        addItem(GroundItem(pos, intArrayOf(c.slot, id, amount, time)))
    }

    @JvmStatic
    fun addNpcDropItem(c: Client, n: Npc, id: Int, amount: Int) {
        addItem(GroundItem(n.position, id, amount, c.slot, n.id))
    }
}