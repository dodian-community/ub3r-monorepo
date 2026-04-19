package net.dodian.uber.game.item.admin

import net.dodian.uber.game.Server
import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object StaffToolItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(5733)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        if (client.playerRights < 2) {
            client.sendMessage("You need to be an administrator to use this tool.")
            return true
        }
        if (client.playerNpc < 0) {
            client.sendMessage("Please select an NPC first using ::pnpc id")
            return true
        }
        if (Server.npcManager.getData(client.playerNpc) == null) {
            Server.npcManager.reloadNpcConfig(client, client.playerNpc, "New Npc", "-1")
            client.sendMessage("NPC data not found, creating a new entry. Please try again.")
            return true
        }

        client.sendMessage("Staff spawn SQL tooling is disabled after NPC hard cutover.")
        client.sendMessage("Define spawns in Kotlin NPC content files for permanent changes.")
        // TODO(npc-hard-cutover): legacy SQL tool flow kept for rollback.
//        return try {
//            val connection = dbConnection
//            val statement = connection.createStatement()
//            try {
//                val position = client.position
//                val npcId = client.playerNpc
//                val existsQuery = "SELECT 1 FROM ${DbTables.GAME_NPC_SPAWNS} WHERE id='$npcId' AND x='${position.x}' AND y='${position.y}' AND height='${position.z}'"
//                val resultSet = statement.executeQuery(existsQuery)
//                try {
//                    if (resultSet.next()) {
//                        client.sendMessage("An NPC spawn already exists at this position.")
//                        return true
//                    }
//                } finally {
//                    resultSet.close()
//                }
//
//                val health = Server.npcManager.getData(npcId).getHP()
//                val insertSql = "INSERT INTO ${DbTables.GAME_NPC_SPAWNS} SET id = $npcId, x=${position.x}, y=${position.y}, height=${position.z}, hitpoints=$health, live=1, face=0, rx=0,ry=0,rx2=0,ry2=0,movechance=0"
//                statement.executeUpdate(insertSql)
//
//                Server.npcManager.createNpc(npcId, Position(position.x, position.y, position.z), 0)
//                client.sendMessage("Npc added = $npcId, at x = ${position.x} y = ${position.y}.")
//            } finally {
//                statement.close()
//                connection.close()
//            }
//            true
//        } catch (exception: Exception) {
//            logger.error("Staff tool SQL error for {}: {}", client.playerName, exception.message, exception)
//            client.sendMessage("An error occurred while spawning the NPC.")
//            true
//        }
        return true
    }
}

