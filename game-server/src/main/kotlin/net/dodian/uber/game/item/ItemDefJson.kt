package net.dodian.uber.game.item

import com.google.gson.annotations.SerializedName

data class ItemDefJson(
    val id: Int,
    val name: String,
    val members: Boolean = false,
    val tradeable: Boolean = false,
    val stackable: Boolean = false,
    val noted: Boolean = false,
    val noteable: Boolean = false,
    val placeholder: Boolean = false,
    @SerializedName("unnoted-id", alternate = ["linked_id_item", "unnoted_id", "unnotedId"]) val unnotedId: Int? = null,
    @SerializedName("noted-id", alternate = ["linked_id_noted", "noted_id", "notedId"]) val notedId: Int? = null,
    @SerializedName("base-value", alternate = ["cost", "base_value", "baseValue"]) val cost: Int = 0,
    @SerializedName("low-alch", alternate = ["lowalch", "low_alch", "lowAlch"]) val lowAlch: Int = 0,
    @SerializedName("high-alch", alternate = ["highalch", "high_alch", "highAlch"]) val highAlch: Int = 0,
    @SerializedName("street-value", alternate = ["street_value", "streetValue"]) val streetValue: Int = 0,
    @SerializedName("two-handed", alternate = ["two_handed", "twoHanded"]) val twoHanded: Boolean = false,
    val weight: Double = 0.0,
    val examine: String? = null,
    val equipment: ItemEquipmentDef? = null,
    val weapon: ItemWeaponDef? = null,
    @SerializedName("linkedIdItem") val linkedIdItem: Int? = null,
    @SerializedName("linkedIdNoted") val linkedIdNoted: Int? = null,
) {
    val effectiveUnnotedId: Int? get() = unnotedId ?: linkedIdItem
    val effectiveNotedId: Int? get() = notedId ?: linkedIdNoted
}
