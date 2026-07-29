package net.dodian.uber.game.api.plugin.social

import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.ContentModuleManifestProvider
import net.dodian.uber.game.api.plugin.PluginModuleMetadata
import net.dodian.uber.game.api.plugin.PluginModuleMetadataProvider

enum class ExchangeKind { TRADE, DUEL }
enum class ExchangeStage { REQUESTED, OFFER, CONFIRM, ACTIVE, SETTLING, COMPLETE, CANCELLED }

data class ExchangeParticipantId(
    val databaseId: Int,
    val playerSlot: Int,
    val sessionEpoch: Long,
)

data class ExchangeItem(val id: Int, val amount: Int) {
    init {
        require(id >= 0)
        require(amount > 0)
    }
}

data class ExchangeReservation(
    val inventorySlot: Int,
    val itemId: Int,
    val amount: Int,
) {
    init {
        require(inventorySlot >= 0)
        require(itemId >= 0)
        require(amount > 0)
    }
}

enum class ExchangeRouteType { PLAYER_OPTION, CHAT_REQUEST, ITEM_OFFER, ITEM_WITHDRAW, BUTTON, LIFECYCLE }

data class ExchangeRoute(
    val type: ExchangeRouteType,
    val key: String,
)

data class SocialPluginDefinition(
    val id: String,
    val name: String,
    val kind: ExchangeKind,
    val routes: Set<ExchangeRoute>,
)

fun SocialPluginDefinition.routeKeys(): Set<String> = routes.mapTo(linkedSetOf()) { "social:${it.type.name.lowercase()}:${it.key}" }

interface SocialPlugin : PluginModuleMetadataProvider, ContentModuleManifestProvider {
    val definition: SocialPluginDefinition
    override val pluginMetadata: PluginModuleMetadata
        get() = PluginModuleMetadata(definition.name, "${definition.name} player exchange content", owner = "gameplay")
    override val contentManifest: ContentModuleManifest
        get() = ContentModuleManifest(
            id = definition.id,
            owner = "gameplay",
            version = "1.0.0",
            declaredRouteKeys = definition.routeKeys(),
        )
}

sealed interface ExchangeCommandResult {
    data object Applied : ExchangeCommandResult
    data object AlreadyApplied : ExchangeCommandResult
    data class Rejected(val reason: ExchangeRejectReason) : ExchangeCommandResult
}

enum class ExchangeRejectReason {
    NOT_IN_SESSION,
    WRONG_STAGE,
    INVALID_PARTICIPANT,
    INVALID_SLOT,
    ITEM_MISMATCH,
    INVALID_AMOUNT,
    NOT_TRADABLE,
    NO_SPACE,
    STALE_REVISION,
    ALREADY_SETTLED,
}
