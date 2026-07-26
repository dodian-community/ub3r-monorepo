package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.db.dbConnection
import net.dodian.uber.game.shop.ShopManager
import org.slf4j.LoggerFactory
import java.sql.SQLException

object ShopLog {
    private val logger = LoggerFactory.getLogger(ShopLog::class.java)

    @JvmStatic
    fun recordBuy(
        client: Client,
        shopId: Int,
        slot: Int,
        itemId: Int,
        amount: Int,
        currencyItemId: Int,
        totalPrice: Long,
    ) {
        ConsoleAuditLog.shopBuy(client, shopId, slot, itemId, amount, currencyItemId, totalPrice.toInt())
        val dbId = client.dbId
        val name = client.playerName ?: "Unknown"
        val pos = client.position ?: Position(0, 0, 0)
        val shopNameStr = shopName(shopId)
        val pricePerItem = if (amount > 0) (totalPrice / amount).toInt() else 0

        asyncRecordTransaction(
            dbId = dbId,
            playerName = name,
            shopId = shopId,
            shopName = shopNameStr,
            type = "BUY",
            itemId = itemId,
            itemAmount = amount,
            currencyId = currencyItemId,
            pricePerItem = pricePerItem,
            totalPrice = totalPrice,
            x = pos.x,
            y = pos.y,
            z = pos.z,
        )
    }

    @JvmStatic
    fun recordSell(
        client: Client,
        shopId: Int,
        slot: Int,
        itemId: Int,
        amount: Int,
        currencyItemId: Int,
        totalPrice: Long,
    ) {
        ConsoleAuditLog.shopSell(client, shopId, slot, itemId, amount, currencyItemId, totalPrice.toInt())
        val dbId = client.dbId
        val name = client.playerName ?: "Unknown"
        val pos = client.position ?: Position(0, 0, 0)
        val shopNameStr = shopName(shopId)
        val pricePerItem = if (amount > 0) (totalPrice / amount).toInt() else 0

        asyncRecordTransaction(
            dbId = dbId,
            playerName = name,
            shopId = shopId,
            shopName = shopNameStr,
            type = "SELL",
            itemId = itemId,
            itemAmount = amount,
            currencyId = currencyItemId,
            pricePerItem = pricePerItem,
            totalPrice = totalPrice,
            x = pos.x,
            y = pos.y,
            z = pos.z,
        )
    }

    private fun asyncRecordTransaction(
        dbId: Int,
        playerName: String,
        shopId: Int,
        shopName: String,
        type: String,
        itemId: Int,
        itemAmount: Int,
        currencyId: Int,
        pricePerItem: Int,
        totalPrice: Long,
        x: Int,
        y: Int,
        z: Int,
    ) {
        DbDispatchers.logExecutor.execute {
            try {
                dbConnection.use { conn ->
                    val sql = """
                        INSERT INTO ${DbTables.GAME_LOGS_SHOP_TRANSACTIONS}
                        (dbid, player_name, shop_id, shop_name, type, item_id, item_amount, currency_id, price_per_item, total_price, x, y, z)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setInt(1, dbId)
                        stmt.setString(2, playerName)
                        stmt.setInt(3, shopId)
                        stmt.setString(4, shopName)
                        stmt.setString(5, type)
                        stmt.setInt(6, itemId)
                        stmt.setInt(7, itemAmount)
                        stmt.setInt(8, currencyId)
                        stmt.setInt(9, pricePerItem)
                        stmt.setLong(10, totalPrice)
                        stmt.setInt(11, x)
                        stmt.setInt(12, y)
                        stmt.setInt(13, z)
                        stmt.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                logger.error("Failed to insert shop transaction log for player $playerName (dbId=$dbId): ${e.message}", e)
            }
        }
    }

    private fun shopName(shopId: Int): String {
        return try {
            ShopManager.ShopName.getOrElse(shopId) { "Shop $shopId" }
        } catch (_: Throwable) {
            "Shop $shopId"
        }
    }
}
