package net.dodian.uber.game.skills.core.resource

data class HarvestNodeDef(
    val family: String,
    val name: String,
    val objectIds: IntArray,
    val requiredLevel: Int,
    val baseDelayMs: Long,
    val resourceItemId: Int,
    val experience: Int,
    val restThreshold: Int,
    val randomBonusEligible: Boolean = true,
)

data class ToolDef(
    val family: String,
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val tierBoostEligible: Boolean = false,
)

data class ResourceSkillContent(
    val nodes: List<HarvestNodeDef>,
    val tools: List<ToolDef>,
)

class ResourceSkillContentBuilder {
    private val nodes = ArrayList<HarvestNodeDef>()
    private val tools = ArrayList<ToolDef>()

    fun family(name: String, block: ResourceFamilyBuilder.() -> Unit) {
        ResourceFamilyBuilder(name, nodes, tools).apply(block)
    }

    fun build(): ResourceSkillContent =
        ResourceSkillContent(
            nodes = nodes.toList(),
            tools = tools.toList(),
        )
}

class ResourceFamilyBuilder internal constructor(
    private val family: String,
    private val nodes: MutableList<HarvestNodeDef>,
    private val tools: MutableList<ToolDef>,
) {
    fun node(name: String, block: HarvestNodeBuilder.() -> Unit) {
        val builder = HarvestNodeBuilder(family, name)
        builder.block()
        nodes += builder.build()
    }

    fun tool(name: String, block: ToolBuilder.() -> Unit) {
        val builder = ToolBuilder(family, name)
        builder.block()
        tools += builder.build()
    }
}

class HarvestNodeBuilder internal constructor(
    private val family: String,
    private val name: String,
) {
    private var objectIds: IntArray = IntArray(0)
    private var requiredLevel: Int = 1
    private var baseDelayMs: Long = 1_000L
    private var resourceItemId: Int = -1
    private var experience: Int = 0
    private var restThreshold: Int = 1
    private var randomBonusEligible: Boolean = true

    fun objectIds(vararg ids: Int) {
        objectIds = ids
    }

    fun requiredLevel(level: Int) {
        requiredLevel = level
    }

    fun baseDelayMs(delayMs: Long) {
        baseDelayMs = delayMs
    }

    fun resourceItemId(itemId: Int) {
        resourceItemId = itemId
    }

    fun experience(xp: Int) {
        experience = xp
    }

    fun restThreshold(threshold: Int) {
        restThreshold = threshold
    }

    fun randomBonusEligible(eligible: Boolean) {
        randomBonusEligible = eligible
    }

    internal fun build(): HarvestNodeDef {
        require(objectIds.isNotEmpty()) { "Resource node '$name' must declare at least one object id." }
        require(resourceItemId >= 0) { "Resource node '$name' must declare a resource item id." }
        require(restThreshold > 0) { "Resource node '$name' must declare a positive rest threshold." }
        return HarvestNodeDef(
            family = family,
            name = name,
            objectIds = objectIds,
            requiredLevel = requiredLevel,
            baseDelayMs = baseDelayMs,
            resourceItemId = resourceItemId,
            experience = experience,
            restThreshold = restThreshold,
            randomBonusEligible = randomBonusEligible,
        )
    }
}

class ToolBuilder internal constructor(
    private val family: String,
    private val name: String,
) {
    private var itemId: Int = -1
    private var requiredLevel: Int = 1
    private var speedBonus: Double = 0.0
    private var animationId: Int = -1
    private var tierBoostEligible: Boolean = false

    fun itemId(id: Int) {
        itemId = id
    }

    fun requiredLevel(level: Int) {
        requiredLevel = level
    }

    fun speedBonus(bonus: Double) {
        speedBonus = bonus
    }

    fun animationId(id: Int) {
        animationId = id
    }

    fun tierBoostEligible(eligible: Boolean) {
        tierBoostEligible = eligible
    }

    internal fun build(): ToolDef {
        require(itemId >= 0) { "Tool '$name' must declare an item id." }
        require(animationId >= 0) { "Tool '$name' must declare an animation id." }
        return ToolDef(
            family = family,
            name = name,
            itemId = itemId,
            requiredLevel = requiredLevel,
            speedBonus = speedBonus,
            animationId = animationId,
            tierBoostEligible = tierBoostEligible,
        )
    }
}

fun resourceSkillContent(block: ResourceSkillContentBuilder.() -> Unit): ResourceSkillContent {
    val builder = ResourceSkillContentBuilder()
    builder.block()
    return builder.build()
}
