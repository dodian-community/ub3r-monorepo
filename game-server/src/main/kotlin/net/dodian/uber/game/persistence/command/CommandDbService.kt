package net.dodian.uber.game.persistence.command

import java.sql.Connection
import java.sql.PreparedStatement
import java.util.ArrayList
import java.util.function.Consumer
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.db.dbConnection

object CommandDbService {
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun <T> submit(
        taskName: String,
        work: ThrowingSupplier<T>?,
        onGameThread: Consumer<T>?,
        onGameThreadError: Consumer<Exception>?,
    ) {
        if (work == null) return
        DbDispatchers.commandExecutor.execute {
            try {
                val result = work.get()
                if (onGameThread != null) {
                    GameThreadTaskQueue.submit { onGameThread.accept(result) }
                }
            } catch (exception: Exception) {
                if (onGameThreadError != null) {
                    GameThreadTaskQueue.submit { onGameThreadError.accept(exception) }
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun loadOfflineContainerView(playerName: String, columnName: String): OfflineContainerViewResult {
        val containerColumn = validateContainerColumn(columnName)
        dbConnection.use { connection ->
            val userId = loadUserId(connection, playerName)
            if (userId < 0) return OfflineContainerViewResult.usernameNotFound(playerName)
            val query = "SELECT $containerColumn FROM ${DbTables.GAME_CHARACTERS} WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, userId)
                statement.executeQuery().use { results ->
                    return if (!results.next()) {
                        OfflineContainerViewResult.characterNotFound(playerName)
                    } else {
                        OfflineContainerViewResult.ready(playerName, toGameItems(parseContainerEntries(results.getString(containerColumn))))
                    }
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun removeOfflineExperience(playerName: String, skillName: String, xp: Int): OfflineSkillMutationResult {
        val skillColumn = validateSkillColumn(skillName)
        dbConnection.use { connection ->
            val userId = loadUserId(connection, playerName)
            if (userId < 0) return OfflineSkillMutationResult.notFound(playerName, skillColumn)
            val select = "SELECT $skillColumn, totalxp, total FROM ${DbTables.GAME_CHARACTERS_STATS} WHERE uid = ?"
            connection.prepareStatement(select).use { statement ->
                statement.setInt(1, userId)
                statement.executeQuery().use { results ->
                    if (!results.next()) return OfflineSkillMutationResult.notFound(playerName, skillColumn)
                    val computation = computeSkillMutation(results.getInt(skillColumn), results.getInt("totalxp"), results.getInt("total"), xp)
                    val update = "UPDATE ${DbTables.GAME_CHARACTERS_STATS} SET $skillColumn = ?, totalxp = ?, total = ? WHERE uid = ?"
                    connection.prepareStatement(update).use { updateStatement ->
                        updateStatement.setInt(1, computation.newXp)
                        updateStatement.setInt(2, computation.newTotalXp)
                        updateStatement.setInt(3, computation.newTotalLevel)
                        updateStatement.setInt(4, userId)
                        updateStatement.executeUpdate()
                    }
                    return OfflineSkillMutationResult.ready(playerName, skillColumn, computation.currentXp, computation.removedXp)
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun removeOfflineItems(playerName: String, itemId: Int, amount: Int): OfflineItemRemovalResult {
        dbConnection.use { connection ->
            val userId = loadUserId(connection, playerName)
            if (userId < 0) return OfflineItemRemovalResult.notFound(playerName, itemId)
            val query = "SELECT bank, inventory, equipment FROM ${DbTables.GAME_CHARACTERS} WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, userId)
                statement.executeQuery().use { results ->
                    if (!results.next()) return OfflineItemRemovalResult.notFound(playerName, itemId)
                    var remaining = amount
                    var totalRemoved = 0
                    val bankMutation = applyItemRemoval(results.getString("bank"), itemId, remaining)
                    remaining = bankMutation.remainingAmount
                    totalRemoved += bankMutation.removedAmount
                    val inventoryMutation = applyItemRemoval(results.getString("inventory"), itemId, remaining)
                    remaining = inventoryMutation.remainingAmount
                    totalRemoved += inventoryMutation.removedAmount
                    val equipmentMutation = applyItemRemoval(results.getString("equipment"), itemId, remaining)
                    totalRemoved += equipmentMutation.removedAmount
                    val update = "UPDATE ${DbTables.GAME_CHARACTERS} SET equipment = ?, inventory = ?, bank = ? WHERE id = ?"
                    connection.prepareStatement(update).use { updateStatement ->
                        updateStatement.setString(1, equipmentMutation.updatedText)
                        updateStatement.setString(2, inventoryMutation.updatedText)
                        updateStatement.setString(3, bankMutation.updatedText)
                        updateStatement.setInt(4, userId)
                        updateStatement.executeUpdate()
                    }
                    return OfflineItemRemovalResult.ready(playerName, itemId, totalRemoved)
                }
            }
        }
    }

    @JvmStatic @Throws(Exception::class)
    fun updateRank(playerName: String, rankId: Int): CommandWriteResult = executeWrite("UPDATE ${DbTables.WEB_USERS_TABLE} SET usergroupid = ? WHERE username = ?") {
        it.setInt(1, rankId)
        it.setString(2, playerName)
    }

    @JvmStatic @Throws(Exception::class)
    fun deleteNpcDrop(npcId: Int, itemId: Int, chance: Double): CommandWriteResult = executeWrite("DELETE FROM ${DbTables.GAME_NPC_DROPS} WHERE npcid = ? AND itemid = ? AND percent = ?") {
        it.setInt(1, npcId)
        it.setInt(2, itemId)
        it.setDouble(3, chance)
    }

    @JvmStatic @Throws(Exception::class)
    fun insertNpcDrop(npcId: Int, chance: Double, itemId: Int, minAmount: Int, maxAmount: Int, rareShout: String): CommandWriteResult = executeWrite(
        "INSERT INTO ${DbTables.GAME_NPC_DROPS} (npcid, percent, itemid, amt_min, amt_max, rareShout) VALUES (?, ?, ?, ?, ?, ?)",
    ) {
        it.setInt(1, npcId)
        it.setDouble(2, chance)
        it.setInt(3, itemId)
        it.setInt(4, minAmount)
        it.setInt(5, maxAmount)
        it.setString(6, rareShout)
    }

    @JvmStatic @Throws(Exception::class)
    fun insertObjectDefinition(id: Int, x: Int, y: Int, type: Int): CommandWriteResult = executeWrite(
        "INSERT INTO ${DbTables.GAME_OBJECT_DEFINITIONS} (id, x, y, type) VALUES (?, ?, ?, ?)",
    ) {
        it.setInt(1, id)
        it.setInt(2, x)
        it.setInt(3, y)
        it.setInt(4, type)
    }

    @JvmStatic
    fun parseContainerEntries(text: String?): ArrayList<ContainerEntry> {
        val items = ArrayList<ContainerEntry>()
        if (text == null || text.length <= 2) return items
        for (line in text.split(" ")) {
            if (line.isEmpty()) continue
            val parts = line.split("-")
            if (parts.size < 3) continue
            items += ContainerEntry(parts[1].toInt(), parts[2].toInt())
        }
        return items
    }

    @JvmStatic
    fun applyItemRemoval(text: String?, itemId: Int, amountToRemove: Int): ContainerMutation {
        if (text == null || text.length <= 2) return ContainerMutation("", 0, amountToRemove)
        val builder = StringBuilder()
        var remaining = amountToRemove
        var removed = 0
        for (line in text.split(" ")) {
            if (line.isEmpty()) continue
            val parts = line.split("-")
            if (parts.size < 3) continue
            val parsedItemId = parts[1].toInt()
            val parsedAmount = parts[2].toInt()
            if (parsedItemId == itemId && remaining > 0) {
                val canRemove = minOf(parsedAmount, remaining)
                val newAmount = parsedAmount - canRemove
                remaining -= canRemove
                removed += canRemove
                if (newAmount > 0) builder.append(parts[0]).append('-').append(parts[1]).append('-').append(newAmount).append(' ')
            } else {
                builder.append(parts[0]).append('-').append(parts[1]).append('-').append(parts[2]).append(' ')
            }
        }
        return ContainerMutation(builder.toString(), removed, remaining)
    }

    @JvmStatic
    fun computeSkillMutation(currentXp: Int, totalXp: Int, totalLevel: Int, requestedXp: Int): SkillMutationComputation {
        val removedXp = minOf(currentXp, requestedXp)
        val newXp = currentXp - removedXp
        val newTotalXp = totalXp - removedXp
        val newTotalLevel = totalLevel -
            net.dodian.uber.game.model.player.skills.Skills.getLevelForExperience(currentXp) +
            net.dodian.uber.game.model.player.skills.Skills.getLevelForExperience(newXp)
        return SkillMutationComputation(currentXp, removedXp, newXp, newTotalXp, newTotalLevel)
    }

    private fun toGameItems(entries: ArrayList<ContainerEntry>): ArrayList<GameItem> {
        val items = ArrayList<GameItem>(entries.size)
        for (entry in entries) items += GameItem(entry.itemId, entry.amount)
        return items
    }

    private fun loadUserId(connection: Connection, playerName: String): Int {
        val query = "SELECT userid FROM ${DbTables.WEB_USERS_TABLE} WHERE username = ?"
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, playerName)
            statement.executeQuery().use { results ->
                return if (results.next()) results.getInt("userid") else -1
            }
        }
    }

    private fun validateSkillColumn(skillName: String?): String {
        require(!(skillName == null || !skillName.matches(Regex("[A-Za-z_]+")))) { "Invalid skill column: $skillName" }
        return skillName
    }

    private fun validateContainerColumn(columnName: String): String {
        require(columnName == "inventory" || columnName == "bank") { "Invalid container column: $columnName" }
        return columnName
    }

    private fun executeWrite(query: String, bind: (PreparedStatement) -> Unit): CommandWriteResult {
        dbConnection.use { connection ->
            connection.prepareStatement(query).use { statement ->
                bind(statement)
                return CommandWriteResult(statement.executeUpdate())
            }
        }
    }

    fun interface ThrowingSupplier<T> {
        @Throws(Exception::class)
        fun get(): T
    }

    data class ContainerMutation(val updatedText: String, val removedAmount: Int, val remainingAmount: Int)
    data class ContainerEntry(val itemId: Int, val amount: Int)
    data class SkillMutationComputation(val currentXp: Int, val removedXp: Int, val newXp: Int, val newTotalXp: Int, val newTotalLevel: Int)
    data class CommandWriteResult(val updatedRows: Int)

    data class OfflineContainerViewResult private constructor(val status: Status, val playerName: String, val items: ArrayList<GameItem>) {
        enum class Status { READY, USERNAME_NOT_FOUND, CHARACTER_NOT_FOUND }
        companion object {
            @JvmStatic fun ready(playerName: String, items: ArrayList<GameItem>) = OfflineContainerViewResult(Status.READY, playerName, items)
            @JvmStatic fun usernameNotFound(playerName: String) = OfflineContainerViewResult(Status.USERNAME_NOT_FOUND, playerName, arrayListOf())
            @JvmStatic fun characterNotFound(playerName: String) = OfflineContainerViewResult(Status.CHARACTER_NOT_FOUND, playerName, arrayListOf())
        }
    }

    data class OfflineSkillMutationResult private constructor(val status: Status, val playerName: String, val skillName: String, val currentXp: Int, val removedXp: Int) {
        enum class Status { READY, NOT_FOUND }
        companion object {
            @JvmStatic fun ready(playerName: String, skillName: String, currentXp: Int, removedXp: Int) = OfflineSkillMutationResult(Status.READY, playerName, skillName, currentXp, removedXp)
            @JvmStatic fun notFound(playerName: String, skillName: String) = OfflineSkillMutationResult(Status.NOT_FOUND, playerName, skillName, 0, 0)
        }
    }

    data class OfflineItemRemovalResult private constructor(val status: Status, val playerName: String, val itemId: Int, val totalItemRemoved: Int) {
        enum class Status { READY, NOT_FOUND }
        companion object {
            @JvmStatic fun ready(playerName: String, itemId: Int, totalItemRemoved: Int) = OfflineItemRemovalResult(Status.READY, playerName, itemId, totalItemRemoved)
            @JvmStatic fun notFound(playerName: String, itemId: Int) = OfflineItemRemovalResult(Status.NOT_FOUND, playerName, itemId, 0)
        }
    }
}
