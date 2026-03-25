package net.dodian.uber.game.content.objects.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.skills.agility.AgilityCourseService
import net.dodian.uber.game.content.objects.dsl.FirstClickDslObjectContent
import net.dodian.uber.game.content.objects.dsl.firstClickObjectActions

object GnomeCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(GnomeCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            AgilityCourseService(client).GnomeLog()
            true
        }
        objectAction(GnomeCourseObjectComponents.NET_ONE) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomeNet1()
            true
        }
        objectAction(GnomeCourseObjectComponents.TREE_BRANCH_UP) { client, _, _, _ ->
            AgilityCourseService(client).GnomeTree1()
            true
        }
        objectAction(GnomeCourseObjectComponents.ROPE_SWING) { client, _, _, _ ->
            AgilityCourseService(client).GnomeRope()
            true
        }
        objectAction(*GnomeCourseObjectComponents.TREE_BRANCH_DOWN) { client, _, _, _ ->
            AgilityCourseService(client).GnomeTreebranch2()
            true
        }
        objectAction(GnomeCourseObjectComponents.NET_TWO) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 3) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomeNet2()
            true
        }
        objectAction(GnomeCourseObjectComponents.PIPE_ENTRY_ONE) { client, _, position, _ ->
            if (client.position.x != 2484 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomePipe()
            true
        }
        objectAction(GnomeCourseObjectComponents.PIPE_ENTRY_TWO) { client, _, position, _ ->
            if (client.position.x != 2487 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomePipe()
            true
        }
    },
) {
    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ObjectInteractionPolicy? {
        if (option != 1 || objectId !in objectIds) {
            return null
        }
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.LEGACY_OBJECT_DISTANCE,
            requireMovementSettled = true,
            settleTicks = 1,
        )
    }
}
