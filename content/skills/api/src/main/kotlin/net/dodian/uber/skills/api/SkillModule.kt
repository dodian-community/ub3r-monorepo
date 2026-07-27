package net.dodian.uber.skills.api

/** Marks nested skill builders so an author cannot accidentally use an outer DSL scope. */
@DslMarker
annotation class SkillDsl

data class SkillModuleDescriptor(
    val id: String,
    val displayName: String,
    val version: String = "1.0.0",
) {
    init {
        require(ID.matches(id)) { "Skill module id must be lowercase dot-separated: $id" }
        require(displayName.isNotBlank()) { "Skill module display name cannot be blank" }
        require(version.isNotBlank()) { "Skill module version cannot be blank" }
    }

    companion object {
        private val ID = Regex("skill\\.[a-z][a-z0-9-]{1,63}")
    }
}

data class Material(val itemId: Int, val amount: Int = 1) {
    init {
        require(itemId >= 0) { "Material item id must be non-negative" }
        require(amount > 0) { "Material amount must be positive" }
    }
}

typealias SkillMaterial = Material

data class SkillRecipe(
    val key: String,
    val outputItemId: Int,
    val outputAmount: Int = 1,
    val byproducts: List<Material> = emptyList(),
    val materials: List<Material> = emptyList(),
    val requiredLevel: Int = 1,
    val experience: Int = 0,
    val animationId: Int = -1,
    val delayTicks: Int = 1,
    val successMessage: String? = null,
    val missingMaterialsMessage: String? = null,
    val toolItemIds: IntArray = IntArray(0),
    val requiresPremium: Boolean = false,
    val requiredFreeSlots: Int = 0,
) {
    init {
        require(KEY.matches(key)) { "Recipe key must be lowercase dot-separated: $key" }
        require(outputItemId >= 0) { "Output item id must be non-negative" }
        require(outputAmount > 0) { "Output amount must be positive" }
        require(requiredLevel > 0) { "Required level must be positive" }
        require(experience >= 0) { "Experience must be non-negative" }
        require(animationId >= -1) { "Animation id must be -1 or non-negative" }
        require(delayTicks > 0) { "Delay must be positive" }
        require(materials.groupBy { it.itemId }.all { (_, values) -> values.size == 1 }) {
            "A recipe must combine duplicate material ids before declaration"
        }
        require(byproducts.groupBy { it.itemId }.all { (_, values) -> values.size == 1 }) {
            "A recipe must combine duplicate byproduct ids before declaration"
        }
        require(requiredFreeSlots >= 0) { "Required free slots cannot be negative" }
    }

    companion object {
        private val KEY = Regex("[a-z][a-z0-9-]*(\\.[a-z0-9-]+)+")
    }
}

enum class SkillMultiAction { MAKE, COOK, CUT, SMELT, SMITH, BURN, STRING, USE }
enum class SkillMultiLayout { AUTO, SINGLE, TWO, THREE, SPECIALIZED }

data class SkillMultiEntry(
    val recipe: SkillRecipe,
    val label: String? = null,
)

data class SkillMultiConfig(
    val key: String,
    val verb: String = "make",
    val action: SkillMultiAction = SkillMultiAction.MAKE,
    val layout: SkillMultiLayout = SkillMultiLayout.AUTO,
    /** Required when using a server-registered non-generic 317 interface. */
    val presentationKey: String? = null,
    val entries: List<SkillMultiEntry>,
) {
    init {
        require(key.isNotBlank()) { "Production menu key cannot be blank" }
        require(verb.isNotBlank()) { "Production menu verb cannot be blank" }
        require(entries.isNotEmpty()) { "Production menu requires at least one entry" }
        require(entries.map { it.recipe.key }.distinct().size == entries.size) { "Production menu recipe keys must be unique" }
        if (layout != SkillMultiLayout.SPECIALIZED) {
            require(entries.size <= 3) { "The active 317 generic production interfaces support at most three entries" }
        }
        require(layout != SkillMultiLayout.SPECIALIZED || !presentationKey.isNullOrBlank()) {
            "Specialized production menus require a presentation key"
        }
    }

    val title: String
        get() = if (entries.size == 1) "How many would you like to $verb?" else "What would you like to $verb?"
}

data class SkillMultiSelection(
    val configKey: String,
    val recipeKey: String,
    val amount: Int,
) {
    init { require(amount > 0) { "Selected amount must be positive" } }
}

@SkillDsl
class SkillRecipeBuilder(private val key: String, private val outputItemId: Int) {
    private var outputAmount: Int = 1
    private val byproducts = linkedMapOf<Int, Int>()
    private val materials = linkedMapOf<Int, Int>()
    private var requiredLevel = 1
    private var experience = 0
    private var animationId = -1
    private var delayTicks = 1
    private var successMessage: String? = null
    private var missingMaterialsMessage: String? = null
    private var toolItemIds = IntArray(0)
    private var requiresPremium = false
    private var requiredFreeSlots = 0

    fun output(amount: Int) {
        require(amount > 0) { "Output amount must be positive" }
        outputAmount = amount
    }

    fun material(itemId: Int, amount: Int = 1) {
        require(itemId >= 0) { "Material item id must be non-negative" }
        require(amount > 0) { "Material amount must be positive" }
        materials[itemId] = (materials[itemId] ?: 0) + amount
    }

    fun byproduct(itemId: Int, amount: Int = 1) {
        require(itemId >= 0) { "Byproduct item id must be non-negative" }
        require(amount > 0) { "Byproduct amount must be positive" }
        byproducts[itemId] = (byproducts[itemId] ?: 0) + amount
    }

    fun requirement(level: Int) { requiredLevel = level }
    fun experience(amount: Int) { experience = amount }
    fun animation(id: Int) { animationId = id }
    fun delay(ticks: Int) { delayTicks = ticks }
    fun success(message: String) { successMessage = message }
    fun missingMaterials(message: String) { missingMaterialsMessage = message }
    fun tool(vararg itemIds: Int) { require(itemIds.isNotEmpty() && itemIds.all { it >= 0 }); toolItemIds = itemIds.distinct().toIntArray() }
    fun premiumOnly() { requiresPremium = true }
    fun freeSlots(amount: Int) { require(amount >= 0); requiredFreeSlots = amount }

    internal fun build(): SkillRecipe = SkillRecipe(
        key = key,
        outputItemId = outputItemId,
        outputAmount = outputAmount,
        byproducts = byproducts.map { (itemId, amount) -> Material(itemId, amount) },
        materials = materials.map { (itemId, amount) -> Material(itemId, amount) },
        requiredLevel = requiredLevel,
        experience = experience,
        animationId = animationId,
        delayTicks = delayTicks,
        successMessage = successMessage,
        missingMaterialsMessage = missingMaterialsMessage,
        toolItemIds = toolItemIds,
        requiresPremium = requiresPremium,
        requiredFreeSlots = requiredFreeSlots,
    )
}

fun skillRecipe(outputItemId: Int, block: SkillRecipeBuilder.() -> Unit = {}): SkillRecipe =
    SkillRecipeBuilder("item.$outputItemId", outputItemId).apply(block).build()

fun skillRecipe(key: String, outputItemId: Int, block: SkillRecipeBuilder.() -> Unit = {}): SkillRecipe =
    SkillRecipeBuilder(key, outputItemId).apply(block).build()
