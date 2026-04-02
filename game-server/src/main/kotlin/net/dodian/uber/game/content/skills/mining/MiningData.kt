package net.dodian.uber.game.content.skills.mining

import net.dodian.uber.game.event.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.skills.ResourceSkillContent
import net.dodian.uber.game.systems.skills.resourceSkillContent

data class MiningState(
    val rockObjectId: Int,
    val rockPosition: Position,
    val startedCycle: Long,
    val resourcesGathered: Int,
)

enum class RockCategory {
    STANDARD,
    GEM,
    SPECIAL,
}

data class MiningRockDef(
    val name: String,
    val objectIds: IntArray,
    val requiredLevel: Int,
    val baseDelayMs: Long,
    val oreItemId: Int,
    val experience: Int,
    val randomGemEligible: Boolean = true,
    val restThreshold: Int,
    val category: RockCategory = RockCategory.STANDARD,
)

data class PickaxeDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean,
)

enum class MiningStopReason {
    USER_INTERRUPT,
    NO_PICKAXE,
    FULL_INVENTORY,
    MOVED_AWAY,
    BUSY,
    RESTED,
    DISCONNECTED,
    INVALID_ROCK,
}

data class MiningStartedEvent(
    val client: Client,
    val rock: MiningRockDef,
    val position: Position,
    val pickaxe: PickaxeDef,
) : GameEvent

data class MiningSuccessEvent(
    val client: Client,
    val rock: MiningRockDef,
    val oreItemId: Int,
    val experience: Int,
    val position: Position,
) : GameEvent

data class MiningStoppedEvent(
    val client: Client,
    val rock: MiningRockDef?,
    val position: Position?,
    val reason: MiningStopReason,
) : GameEvent

object GemRocksObjectComponents {
    // Current Dodian mining parity does not include gem-rock object behavior.
    val objectIds: IntArray = intArrayOf()
}

object SpecialMiningObjectComponents {
    // Current Dodian mining parity does not include special mining object behavior.
    val objectIds: IntArray = intArrayOf()
}

object MiningData {
    private val content: ResourceSkillContent = buildMiningContent()

    val rocks: List<MiningRockDef> =
        content.nodes.map { node ->
            MiningRockDef(
                name = node.name,
                objectIds = node.objectIds,
                requiredLevel = node.requiredLevel,
                baseDelayMs = node.baseDelayMs,
                oreItemId = node.resourceItemId,
                experience = node.experience,
                randomGemEligible = node.randomBonusEligible,
                restThreshold = node.restThreshold,
                category =
                    when (node.family) {
                        "Gem Rocks" -> RockCategory.GEM
                        "Special" -> RockCategory.SPECIAL
                        else -> RockCategory.STANDARD
                    },
            )
        }

    val rockByObjectId: Map<Int, MiningRockDef> =
        buildMap {
            rocks.forEach { rock ->
                rock.objectIds.forEach { objectId ->
                    put(objectId, rock)
                }
            }
        }

    val allRockObjectIds: IntArray = rockByObjectId.keys.sorted().toIntArray()

    val pickaxesDescending: List<PickaxeDef> =
        content.tools.map { tool ->
            PickaxeDef(
                name = tool.name,
                itemId = tool.itemId,
                requiredLevel = tool.requiredLevel,
                speedBonus = tool.speedBonus,
                animationId = tool.animationId,
                dragonTierBoostEligible = tool.tierBoostEligible,
            )
        }

    val pickaxeByItemId: Map<Int, PickaxeDef> = pickaxesDescending.associateBy { it.itemId }

    val randomGemDropTable: IntArray = intArrayOf(1623, 1623, 1623, 1621, 1621, 1619, 1617)

    private fun buildMiningContent(): ResourceSkillContent =
        resourceSkillContent {
            family("Essence") {
                node("Rune essence") {
                    objectIds(7471)
                    requiredLevel(1)
                    baseDelayMs(1000L)
                    resourceItemId(1436)
                    experience(50)
                    restThreshold(14)
                    randomBonusEligible(false)
                }
            }
            family("Common Ores") {
                node("Copper") {
                    objectIds(7451, 7484)
                    requiredLevel(1)
                    baseDelayMs(2000L)
                    resourceItemId(436)
                    experience(110)
                    restThreshold(4)
                }
                node("Tin") {
                    objectIds(7452, 7485)
                    requiredLevel(1)
                    baseDelayMs(2000L)
                    resourceItemId(438)
                    experience(110)
                    restThreshold(4)
                }
                node("Iron") {
                    objectIds(7455, 7488)
                    requiredLevel(15)
                    baseDelayMs(3000L)
                    resourceItemId(440)
                    experience(280)
                    restThreshold(4)
                }
                node("Coal") {
                    objectIds(7456, 7489)
                    requiredLevel(30)
                    baseDelayMs(5000L)
                    resourceItemId(453)
                    experience(420)
                    restThreshold(4)
                }
                node("Gold") {
                    objectIds(7458, 7491)
                    requiredLevel(40)
                    baseDelayMs(6000L)
                    resourceItemId(444)
                    experience(510)
                    restThreshold(4)
                }
            }
            family("High Tier Ores") {
                node("Mithril") {
                    objectIds(7459, 7492)
                    requiredLevel(55)
                    baseDelayMs(7000L)
                    resourceItemId(447)
                    experience(620)
                    restThreshold(4)
                }
                node("Adamantite") {
                    objectIds(7460, 7493)
                    requiredLevel(70)
                    baseDelayMs(9000L)
                    resourceItemId(449)
                    experience(780)
                    restThreshold(4)
                }
                node("Runite") {
                    objectIds(7461, 7494)
                    requiredLevel(85)
                    baseDelayMs(35000L)
                    resourceItemId(451)
                    experience(3100)
                    restThreshold(4)
                }
            }
            family("Pickaxes") {
                tool("3rd age") {
                    itemId(20014)
                    requiredLevel(61)
                    speedBonus(0.8)
                    animationId(7139)
                    tierBoostEligible(true)
                }
                tool("Dragon") {
                    itemId(11920)
                    requiredLevel(61)
                    speedBonus(0.8)
                    animationId(7139)
                    tierBoostEligible(true)
                }
                tool("Rune") {
                    itemId(1275)
                    requiredLevel(41)
                    speedBonus(0.42)
                    animationId(624)
                }
                tool("Iron") {
                    itemId(1271)
                    requiredLevel(31)
                    speedBonus(0.33)
                    animationId(628)
                }
                tool("Steel") {
                    itemId(1273)
                    requiredLevel(21)
                    speedBonus(0.24)
                    animationId(629)
                }
                tool("Black") {
                    itemId(12297)
                    requiredLevel(11)
                    speedBonus(0.15)
                    animationId(629)
                }
                tool("Mithril") {
                    itemId(1269)
                    requiredLevel(6)
                    speedBonus(0.1)
                    animationId(627)
                }
                tool("Adamant") {
                    itemId(1267)
                    requiredLevel(1)
                    speedBonus(0.065)
                    animationId(626)
                }
                tool("Bronze") {
                    itemId(1265)
                    requiredLevel(1)
                    speedBonus(0.04)
                    animationId(625)
                }
            }
        }
}
