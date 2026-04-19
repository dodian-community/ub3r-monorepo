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
