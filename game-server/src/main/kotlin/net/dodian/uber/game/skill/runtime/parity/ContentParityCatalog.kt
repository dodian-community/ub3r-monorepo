package net.dodian.uber.game.skill.runtime.parity

import net.dodian.uber.game.model.player.skills.Skill

data class NpcClickRouteKey(
    val option: Int,
    val npcId: Int,
)

data class ObjectClickRouteKey(
    val option: Int,
    val objectId: Int,
)

data class ItemOnItemRouteKey(
    val leftItemId: Int,
    val rightItemId: Int,
)

enum class SkillRouteType {
    OBJECT,
    NPC,
    ITEM_ON_ITEM,
    BUTTON,
}

enum class SkillMigrationState { LEGACY, BETA, STABLE }

object SkillMigrationCatalog {
    private val states = Skill.VALUES.associateWith { SkillMigrationState.LEGACY }.toMutableMap().apply {
        // A module is promoted to STABLE only after its immutable baseline parity fixture and
        // ownership sweep pass. The current migration still has active legacy owners.
        listOf(
            Skill.FLETCHING, Skill.COOKING, Skill.FISHING, Skill.WOODCUTTING, Skill.MINING,
            Skill.FIREMAKING, Skill.RUNECRAFTING, Skill.THIEVING,
            Skill.SMITHING, Skill.AGILITY, Skill.SLAYER, Skill.FARMING,
        ).forEach { this[it] = SkillMigrationState.BETA }
        // Prayer reached full parity: bury/altar/toggle fully own the plugin, and the combat
        // protect-prayer checks now route through PrayerCombatService instead of duplicating
        // logic against the old PrayerManager class directly.
        // Crafting reached full parity: gems/hides/spinning are plugin-native; the regular-leather
        // menu, tanning, and gold jewelry (still Client-based, driven by CraftingInterface.kt's
        // button bindings) and the crystal-key/orb/fishbowl/wool combos (ItemCombinationService.kt)
        // no longer depend on the old game/skill/crafting package - only the confirmed-dead
        // ResourceFilling/shaft-crafting/old-spinning paths were dropped, all still-live routes
        // are unchanged.
        // Herblore reached full parity: simple herb-cleaning/potion-mixing (19 recipes) are
        // plugin-native and shadow the old combo loops; dose-mixing, the two multi-ingredient
        // specials (super combat, overload), and the NPC-dialogue batch grind/pack flow are all
        // still-live Client-based code, relocated off the old game/skill/herblore package.
        listOf(Skill.PRAYER, Skill.CRAFTING, Skill.HERBLORE).forEach { this[it] = SkillMigrationState.STABLE }
    }
    fun state(skill: Skill): SkillMigrationState = states.getValue(skill)
    fun requiredCoverage(): Set<Skill> = states.filterValues { it != SkillMigrationState.LEGACY }.keys
}

data class ContentParityCatalog(
    val requiredNpcClicks: Set<NpcClickRouteKey>,
    val bannedNpcClicks: Set<NpcClickRouteKey>,
    val requiredObjectClicks: Set<ObjectClickRouteKey>,
    val bannedObjectClicks: Set<ObjectClickRouteKey>,
    val requiredItemOnItem: Set<ItemOnItemRouteKey>,
    val bannedItemOnItem: Set<ItemOnItemRouteKey>,
    val requiredSkillCoverage: Set<Skill>,
    val requiredSkillRouteTypes: Map<Skill, Set<SkillRouteType>>,
)

object LegacyContentParityCatalog {
    // These are the cache IDs owned by the mining plugin.  Keep this independent
    // from RSCM: cache-name mappings are diagnostic data, not gameplay routing.
    private val miningObjectIds = setOf(
        34773, 10943, 11161, 11360, 11361, 11364, 11365, 11366, 11367,
        11370, 11371, 36206, 11372, 11373, 11374, 11375, 36208, 11376, 11377,
    )
    private val retiredMiningObjectIds = setOf(7471, 7451, 7484, 7452, 7485, 7455, 7488, 7456, 7489, 7458, 7491, 7459, 7492, 7460, 7493, 7461, 7494)

    val default: ContentParityCatalog =
        ContentParityCatalog(
            requiredNpcClicks =
                setOf(
                    NpcClickRouteKey(option = 1, npcId = 555),
                    NpcClickRouteKey(option = 1, npcId = 557),
                ),
            bannedNpcClicks = emptySet(),
            requiredObjectClicks =
                miningObjectIds.map { ObjectClickRouteKey(option = 1, objectId = it) }.toSet(),
            bannedObjectClicks =
                retiredMiningObjectIds.map { ObjectClickRouteKey(option = 1, objectId = it) }.toSet(),
            requiredItemOnItem = emptySet(),
            bannedItemOnItem = emptySet(),
            requiredSkillCoverage =
                setOf(
                    Skill.MINING,
                    Skill.WOODCUTTING,
                    Skill.FISHING,
                    Skill.AGILITY,
                    Skill.COOKING,
                    Skill.CRAFTING,
                    Skill.FARMING,
                    Skill.FIREMAKING,
                    Skill.FLETCHING,
                    Skill.HERBLORE,
                    Skill.PRAYER,
                    Skill.RUNECRAFTING,
                    Skill.SLAYER,
                    Skill.SMITHING,
                    Skill.THIEVING,
                ),
            requiredSkillRouteTypes =
                mapOf(
                    Skill.MINING to setOf(SkillRouteType.OBJECT),
                    Skill.WOODCUTTING to setOf(SkillRouteType.OBJECT),
                    Skill.FISHING to setOf(SkillRouteType.NPC),
                    Skill.AGILITY to setOf(SkillRouteType.OBJECT),
                    Skill.CRAFTING to setOf(SkillRouteType.OBJECT),
                    Skill.FARMING to setOf(SkillRouteType.OBJECT),
                    Skill.FIREMAKING to setOf(SkillRouteType.ITEM_ON_ITEM),
                    Skill.FLETCHING to setOf(SkillRouteType.ITEM_ON_ITEM),
                    Skill.PRAYER to setOf(SkillRouteType.OBJECT),
                    Skill.RUNECRAFTING to setOf(SkillRouteType.OBJECT),
                    Skill.SMITHING to setOf(SkillRouteType.OBJECT),
                    Skill.THIEVING to setOf(SkillRouteType.OBJECT),
                ),
        )
}
