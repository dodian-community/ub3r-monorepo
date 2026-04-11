package net.dodian.uber.game.engine.systems.world

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

class OnlinePlayerIndex {
    private val byDbId = HashMap<Int, Client>()
    private val bySlot = HashMap<Int, Client>()
    private val activePlayers = ArrayList<Client>()
    private val activeSet = IdentityHashMap<Client, Boolean>()

    fun refresh() {
        byDbId.clear()
        bySlot.clear()
        activePlayers.clear()
        activeSet.clear()
        for (client in PlayerRegistry.snapshotActivePlayers()) {
            activePlayers += client
            activeSet[client] = true
            bySlot[client.slot] = client
            if (client.dbId > 0) {
                byDbId[client.dbId] = client
            }
        }
    }

    fun snapshot(): List<Client> = activePlayers

    fun playerCount(): Int = activePlayers.size

    fun dbIds(): List<Int> = ArrayList(byDbId.keys)

    fun dbIdsArray(): IntArray {
        val ids = IntArray(byDbId.size)
        var index = 0
        byDbId.keys.forEach { dbId ->
            ids[index++] = dbId
        }
        return ids
    }

    fun byDbId(dbId: Int): Client? = byDbId[dbId]

    fun activeSet(): IdentityHashMap<Client, Boolean> = activeSet
}
