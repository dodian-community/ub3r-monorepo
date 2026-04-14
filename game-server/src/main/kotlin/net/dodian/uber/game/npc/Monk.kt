package net.dodian.uber.game.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Monk : NpcModule {
    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 555, x = 2604, y = 3092, z = 0, face = 0),
    )
    val npcIds: IntArray = npcIdsFromEntries(entries)


    override val definition = legacyNpcDefinition(
        name = "Monk",
        entries = entries,
        onFirstClick = ::onFirstClick,
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.quests[0]++
        client.sendMessage(
            if (client.playerRights > 1) {
                "Set your quest to: ${client.quests[0]}"
            } else {
                "Suddenly the monk had an urge to dissapear!"
            },
        )
        return true
    }
}
