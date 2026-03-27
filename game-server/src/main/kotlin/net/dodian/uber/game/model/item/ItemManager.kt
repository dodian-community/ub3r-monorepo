package net.dodian.uber.game.model.item

import java.sql.Statement
import java.util.HashMap
import java.util.LinkedHashMap
import net.dodian.uber.game.content.items.spawn.GlobalGroundSpawnContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import org.slf4j.LoggerFactory

open class ItemManager @JvmOverloads constructor(
    private val definitionLoader: (() -> Map<Int, Item>)? = null,
    private val globalSpawnBootstrap: (() -> Unit)? = null,
) {
    @JvmField
    val items: MutableMap<Int, Item> = HashMap()

    private val logger = LoggerFactory.getLogger(ItemManager::class.java)
    private val defaultStandAnim = 808
    private val defaultWalkAnim = 819
    private val defaultRunAnim = 824
    private val defaultAttackAnim = 806

    init {
        loadGlobalItems()
        loadItems()
    }

    open fun loadGlobalItems() {
        val bootstrap = globalSpawnBootstrap
        if (bootstrap != null) {
            bootstrap()
            return
        }
        GlobalGroundSpawnContent.spawnAll()
    }

    open fun loadItems() {
        items.clear()
        items.putAll(definitionLoader?.invoke() ?: loadItemsFromDatabase())
        logger.info("Loaded {} item definitions.", items.size)
    }

    fun isNote(id: Int): Boolean {
        val item = items[id]
        return item != null && id >= 0 && item.getNoteable()
    }

    fun isStackable(id: Int): Boolean {
        val item = items[id]
        return item != null && id >= 0 && item.getStackable()
    }

    fun isTwoHanded(id: Int): Boolean {
        val item = items[id]
        return id >= 0 && item != null && item.getTwoHanded()
    }

    fun getSlot(id: Int): Int {
        if (id < 0) {
            return 3
        }
        val item = items[id] ?: return 3
        return item.getSlot()
    }

    fun getStandAnim(id: Int): Int {
        if (id < 1) {
            return defaultStandAnim
        }
        val item = items[id] ?: return defaultStandAnim
        return item.getStandAnim()
    }

    fun getWalkAnim(id: Int): Int {
        if (id < 1) {
            return defaultWalkAnim
        }
        val item = items[id] ?: return defaultWalkAnim
        return item.getWalkAnim()
    }

    fun getRunAnim(id: Int): Int {
        if (id < 1) {
            return defaultRunAnim
        }
        val item = items[id] ?: return defaultRunAnim
        return item.getRunAnim()
    }

    fun getAttackAnim(id: Int): Int {
        if (id < 1) {
            return defaultAttackAnim
        }
        val item = items[id] ?: return defaultAttackAnim
        return item.getAttackAnim()
    }

    fun isPremium(id: Int): Boolean {
        if (id < 0) {
            return false
        }
        val item = items[id] ?: return false
        return item.getPremium()
    }

    fun isTradable(id: Int): Boolean {
        if (id < 0 || id == 4084) {
            return false
        }
        val item = items[id] ?: return false
        return item.getTradeable()
    }

    fun getBonus(id: Int, bonus: Int): Int {
        if (id < 0) {
            return 0
        }
        val item = items[id] ?: return 0
        return item.getBonuses()[bonus]
    }

    fun isFullBody(id: Int): Boolean {
        if (id < 0) {
            return false
        }
        val item = items[id]
        return item != null && item.getSlot() == 4 && item.full
    }

    fun isFullHelm(id: Int): Boolean {
        if (id < 0) {
            return false
        }
        val item = items[id]
        return item != null && item.getSlot() == 0 && item.full
    }

    fun isMask(id: Int): Boolean {
        if (id < 0) {
            return false
        }
        val item = items[id]
        return item != null && item.getSlot() == 0 && item.mask
    }

    fun getShopSellValue(id: Int): Int = items[id]?.getShopSellValue() ?: 1

    fun getShopBuyValue(id: Int): Int = items[id]?.getShopBuyValue() ?: 0

    fun getAlchemy(id: Int): Int = items[id]?.getAlchemy() ?: 0

    fun getName(id: Int): String =
        items[id]?.getName()?.replace("_", " ")
            ?: "Database Error. Please contact admins with this error code: ITEM_NAME_$id"

    fun getExamine(id: Int): String = items[id]?.getDescription()?.replace("_", " ") ?: ""

    fun getItemName(c: Client, name: String) {
        var send = false
        val normalizedName = name.replace("_", " ")
        for (item in items.values) {
            if (normalizedName.equals(item.getName().replace("_", " "), ignoreCase = true) &&
                !item.getDescription().equals("null", ignoreCase = true)
            ) {
                var prefix = ""
                if (isNote(item.getId())) {
                    prefix = " (NOTED)"
                }
                c.send(
                    SendMessage(
                        normalizedName.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase() else char.toString()
                        } + prefix +
                            ": Sell Price: ${item.getShopBuyValue()}. Alchemy Price: ${item.getAlchemy()}",
                    ),
                )
                send = true
            }
        }
        if (!send) {
            c.send(SendMessage("Could not find the item $normalizedName in the database!"))
        }
    }

    open fun reloadItems() {
        loadItems()
    }

    private fun loadItemsFromDatabase(): Map<Int, Item> {
        val loaded = LinkedHashMap<Int, Item>()
        val query = "SELECT * FROM ${DbTables.GAME_ITEM_DEFINITIONS} ORDER BY id ASC"
        try {
            DbAsyncRepository.withConnection { connection ->
                connection.createStatement().use { statement: Statement ->
                    statement.executeQuery(query).use { rows ->
                        while (rows.next()) {
                            loaded[rows.getInt("id")] = Item(rows)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to load item definitions from the database.", exception)
        }
        return loaded
    }
}
