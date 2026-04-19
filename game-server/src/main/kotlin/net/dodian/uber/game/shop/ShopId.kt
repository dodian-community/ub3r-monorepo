package net.dodian.uber.game.shop

enum class ShopId(
    val id: Int,
    val displayName: String,
) {
    LEGACY_SHOP_2(2, "Legacy Shop 2"),
    GENERAL_STORE(3, "General Store"),
    AUBURYS_MAGIC_STORE(9, "Aubury's Magic Store"),
    WEAPON_AND_ARMOR(10, "Weapon and Armor"),
    RANGE_STORE(11, "Range store"),
    SLAYER_SHOP(15, "Slayer Shop"),
    FISHING_STORE(18, "Fishing Store"),
    LEGACY_SHOP_19(19, "Legacy Shop 19"),
    PREMIUM_MEMBER_SHOP(20, "Premium Member Shop"),
    LEGACY_SHOP_22(22, "Legacy Shop 22"),
    CRAFTING_STORE(25, "Crafting store"),
    CAPE_SHOP(34, "Cape Shop"),
    JATIX_HERBLORE_STORE(39, "Jatix herblore Store"),
    FISHING_SUPPLIES(40, "Fishing supplies"),
    CHRISTMAS_EVENT_STORE(55, "Christmas Event Store"),
    ;

    companion object {
        private val byId = values().associateBy(ShopId::id)

        @JvmStatic
        fun fromId(id: Int): ShopId? = byId[id]
    }
}



