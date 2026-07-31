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
            Skill.SLAYER, Skill.FARMING,
        ).forEach { this[it] = SkillMigrationState.BETA }
        // Prayer reached full parity: bury/altar/toggle fully own the plugin, and the combat
        // protect-prayer checks now route through PrayerCombatService instead of duplicating
        // logic against the old PrayerManager class directly.
        // Crafting reached full parity, then went fully plugin-native (no legacy owners left at
        // all, not just "off the old package"): gems/hides/spinning/tanning/glass-blowing/crystal
        // key/fishbowl-helmet/wool-amulet are all plugin-native now. The old game/skill/crafting
        // package (Crafting.kt/CraftingActions.kt/CraftingItemCombinations.kt) and
        // ui/CraftingInterface.kt are deleted entirely - a follow-up audit found the "regular
        // leather" (item 1741) 7-product menu and the gold-jewelry mould interface were both
        // confirmed dead/unreachable content (no live trigger existed for either), not migration
        // work, so they were deleted rather than ported (same "dead code, not missing content"
        // treatment already applied to the old orb-charging-spell/molten-glass gaps).
        // Herblore reached full parity: simple herb-cleaning/potion-mixing (19 recipes) are
        // plugin-native and shadow the old combo loops; dose-mixing, the two multi-ingredient
        // specials (super combat, overload), and the NPC-dialogue batch grind/pack flow are all
        // still-live Client-based code, relocated off the old game/skill/herblore package.
        // Agility reached full parity: obstacle-crossing was already plugin-native; this pass
        // fixed the UsingAgility movement-lock bypass (ClientSkillPlayerAdapter now routes through
        // setMovementLockState instead of writing the raw field), added DB persistence for
        // in-progress lap stage (previously in-memory only, lost on logout), removed the fully
        // vestigial agilitySessionStage/AgilityCourseStage plumbing (superseded by the retired
        // course bindings), and ported the remaining content gaps: item-key gating on the
        // orange-bar/yellow-ledge shortcuts, the werewolf ring-of-charos xp bonus, and the
        // werewolf juicy-stick turn-in (previously unreachable from any dispatch path at all).
        // Smithing reached full parity: bar-smelting/anvil logic was already fully plugin-native
        // (SmithingAnvilService/SmithingSmeltingService/SmeltingRegistry, with real legacy-protocol
        // bridges already routing everything there). The only things left in the old package were
        // two side-features that were never core smithing logic to begin with - the Superheat magic
        // spell (already reading its recipe data from the new SmeltingRegistry) and the standalone
        // Rock-shell armor minigame - both just needed relocating off the old game/skill/smithing
        // package, not porting.
        listOf(Skill.PRAYER, Skill.CRAFTING, Skill.HERBLORE, Skill.AGILITY, Skill.SMITHING).forEach { this[it] = SkillMigrationState.STABLE }
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
