package net.dodian.uber.game.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.content.objects.travel.VerticalTravel
import net.dodian.uber.game.content.objects.travel.VerticalTravelStyle
import net.dodian.uber.game.content.objects.travel.VerticalTravelStyles
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy

data class VerticalTravelAction(
    val option: Int,
    val objectIds: IntArray,
    val policy: ContentObjectInteractionPolicy,
    val handler: (Client, Int, Position, GameObjectData?) -> Boolean,
)

class VerticalTravelActionBuilder internal constructor() {
    private val actions = ArrayList<VerticalTravelAction>()

    fun firstClick(
        vararg objectIds: Int,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        handler: (Client, Int, Position, GameObjectData?) -> Boolean,
    ) = action(1, objectIds, policy, handler)

    fun secondClick(
        vararg objectIds: Int,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        handler: (Client, Int, Position, GameObjectData?) -> Boolean,
    ) = action(2, objectIds, policy, handler)

    fun thirdClick(
        vararg objectIds: Int,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        handler: (Client, Int, Position, GameObjectData?) -> Boolean,
    ) = action(3, objectIds, policy, handler)

    fun verticalLink(
        vararg objectIds: Int,
        option: Int = 1,
        style: VerticalTravelStyle,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = action(option, objectIds, policy) { client, objectId, position, obj ->
        val resolved = destination(client, objectId, position, obj) ?: return@action false
        VerticalTravel.start(client, resolved, style)
    }

    fun verticalLink(
        vararg objectIds: Int,
        option: Int = 1,
        style: VerticalTravelStyle,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = style, policy = policy) { _, _, _, _ -> destination }

    fun ladderUp(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.LADDER, policy = policy, destination = destination)

    fun ladderUp(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.LADDER, policy = policy, destination = destination)

    fun ladderDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.LADDER, policy = policy, destination = destination)

    fun ladderDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.LADDER, policy = policy, destination = destination)

    fun stairsUp(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.STAIRS, policy = policy, destination = destination)

    fun stairsUp(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.STAIRS, policy = policy, destination = destination)

    fun stairsDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.STAIRS, policy = policy, destination = destination)

    fun stairsDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.STAIRS, policy = policy, destination = destination)

    fun trapdoorDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: (Client, Int, Position, GameObjectData?) -> Position?,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.TRAPDOOR, policy = policy, destination = destination)

    fun trapdoorDown(
        vararg objectIds: Int,
        option: Int = 1,
        policy: ContentObjectInteractionPolicy = DEFAULT_VERTICAL_POLICY,
        destination: Position,
    ) = verticalLink(*objectIds, option = option, style = VerticalTravelStyles.TRAPDOOR, policy = policy, destination = destination)

    private fun action(
        option: Int,
        objectIds: IntArray,
        policy: ContentObjectInteractionPolicy,
        handler: (Client, Int, Position, GameObjectData?) -> Boolean,
    ) {
        require(objectIds.isNotEmpty()) { "Vertical travel action requires at least one object id" }
        actions += VerticalTravelAction(option = option, objectIds = objectIds.copyOf(), policy = policy, handler = handler)
    }

    internal fun build(): List<VerticalTravelAction> = actions.toList()
}

fun verticalTravelActions(block: VerticalTravelActionBuilder.() -> Unit): List<VerticalTravelAction> {
    val builder = VerticalTravelActionBuilder()
    builder.block()
    return builder.build()
}

abstract class VerticalTravelDslObjectContent(
    actions: List<VerticalTravelAction>,
) : ObjectContent {
    private val handlers = HashMap<Int, HashMap<Int, MutableList<(Client, Int, Position, GameObjectData?) -> Boolean>>>()
    private val policies = HashMap<Int, HashMap<Int, ContentObjectInteractionPolicy>>()

    final override val objectIds: IntArray

    init {
        val ids = LinkedHashSet<Int>()
        for (action in actions) {
            val optionHandlers = handlers.getOrPut(action.option) { HashMap() }
            val optionPolicies = policies.getOrPut(action.option) { HashMap() }
            for (objectId in action.objectIds) {
                ids += objectId
                optionHandlers.getOrPut(objectId) { ArrayList() } += action.handler
                optionPolicies.putIfAbsent(objectId, action.policy)
            }
        }
        objectIds = ids.sorted().toIntArray()
    }

    final override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
        dispatch(option = 1, client = client, objectId = objectId, position = position, obj = obj)

    final override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
        dispatch(option = 2, client = client, objectId = objectId, position = position, obj = obj)

    final override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
        dispatch(option = 3, client = client, objectId = objectId, position = position, obj = obj)

    final override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? = policies[option]?.get(objectId)

    private fun dispatch(
        option: Int,
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val optionHandlers = handlers[option] ?: return false
        val objectHandlers = optionHandlers[objectId] ?: return false
        for (handler in objectHandlers) {
            if (handler(client, objectId, position, obj)) {
                return true
            }
        }
        return false
    }
}

val DEFAULT_VERTICAL_POLICY =
    ContentInteraction.nearestBoundaryCardinalPolicy()
