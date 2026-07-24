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
        this[Skill.FLETCHING] = SkillMigrationState.STABLE
        this[Skill.COOKING] = SkillMigrationState.STABLE
        this[Skill.FISHING] = SkillMigrationState.STABLE
        this[Skill.WOODCUTTING] = SkillMigrationState.STABLE
        this[Skill.MINING] = SkillMigrationState.STABLE
        this[Skill.FIREMAKING] = SkillMigrationState.STABLE
        this[Skill.RUNECRAFTING] = SkillMigrationState.STABLE
        this[Skill.HERBLORE] = SkillMigrationState.STABLE
        this[Skill.PRAYER] = SkillMigrationState.STABLE
        this[Skill.THIEVING] = SkillMigrationState.STABLE
        this[Skill.CRAFTING] = SkillMigrationState.STABLE
        this[Skill.SMITHING] = SkillMigrationState.STABLE
        this[Skill.AGILITY] = SkillMigrationState.STABLE
        this[Skill.SLAYER] = SkillMigrationState.STABLE
        this[Skill.FARMING] = SkillMigrationState.STABLE
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
    val default: ContentParityCatalog =
        ContentParityCatalog(
            requiredNpcClicks =
                setOf(
                    NpcClickRouteKey(option = 1, npcId = 555),
                    NpcClickRouteKey(option = 1, npcId = 557),
                ),
            bannedNpcClicks = emptySet(),
            requiredObjectClicks = emptySet(),
            bannedObjectClicks = emptySet(),
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
