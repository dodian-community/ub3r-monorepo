package net.dodian.uber.game.model.item

import java.sql.ResultSet

class Item(
    private val id: Int,
    private val slot: Int,
    private val standAnim: Int,
    private val walkAnim: Int,
    private val runAnim: Int,
    private val attackAnim: Int,
    private val shopSellValue: Int,
    private val shopBuyValue: Int,
    private val bonuses: IntArray,
    private val stackable: Boolean,
    private val noteable: Boolean,
    private val tradeable: Boolean,
    private val twoHanded: Boolean,
    val full: Boolean,
    val mask: Boolean,
    private val premium: Boolean,
    private val name: String,
    private val description: String,
    private val alchemy: Int,
) {
    constructor(row: ResultSet) : this(
        id = row.getInt("id"),
        slot = row.getInt("slot"),
        standAnim = row.getInt("standAnim"),
        walkAnim = row.getInt("walkAnim"),
        runAnim = row.getInt("runAnim"),
        attackAnim = row.getInt("attackAnim"),
        shopSellValue = row.getInt("shopSellValue"),
        shopBuyValue = row.getInt("shopBuyValue"),
        bonuses =
            IntArray(12) { index ->
                row.getInt("bonus${index + 1}")
            },
        stackable = row.getBoolean("stackable"),
        noteable = row.getBoolean("noteable"),
        tradeable = row.getBoolean("tradeable"),
        twoHanded = row.getBoolean("twohanded"),
        full = row.getInt("full") == 1,
        mask = row.getInt("full") == 2,
        premium = row.getInt("premium") == 1,
        name = row.getString("name")?.replace("_", " ") ?: "Unnamed Item",
        description = row.getString("description") ?: "No Description",
        alchemy = row.getInt("Alchemy"),
    )

    fun getId(): Int = id

    fun getName(): String = name

    fun getDescription(): String = description

    fun getSlot(): Int = slot

    fun getStackable(): Boolean = stackable

    fun getTradeable(): Boolean = tradeable

    fun getNoteable(): Boolean = noteable

    fun getShopSellValue(): Int = shopSellValue

    fun getShopBuyValue(): Int = shopBuyValue

    fun getAlchemy(): Int = alchemy

    fun getStandAnim(): Int = standAnim

    fun getWalkAnim(): Int = walkAnim

    fun getRunAnim(): Int = runAnim

    fun getAttackAnim(): Int = attackAnim

    fun getPremium(): Boolean = premium

    fun getTwoHanded(): Boolean = twoHanded

    fun getBonuses(): IntArray = bonuses

    override fun toString(): String =
        "$name ($id); slot $slot; standAnim$standAnim; walkAnim $walkAnim; runAnim $runAnim; attackAnim $attackAnim"
}
