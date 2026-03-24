package net.dodian.uber.game.content.objects.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.skills.core.FirstClickDslObjectContent
import net.dodian.uber.game.skills.core.firstClickObjectActions
import net.dodian.uber.game.skills.agility.AgilityCourseService

object GnomeCourseObjects : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(23145) { client, _, _, _ ->
            AgilityCourseService(client).GnomeLog()
            true
        }
        objectAction(23134) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomeNet1()
            true
        }
        objectAction(23559) { client, _, _, _ ->
            AgilityCourseService(client).GnomeTree1()
            true
        }
        objectAction(23557) { client, _, _, _ ->
            AgilityCourseService(client).GnomeRope()
            true
        }
        objectAction(23560, 23561) { client, _, _, _ ->
            AgilityCourseService(client).GnomeTreebranch2()
            true
        }
        objectAction(23135) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 3) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomeNet2()
            true
        }
        objectAction(23138) { client, _, position, _ ->
            if (client.position.x != 2484 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            AgilityCourseService(client).GnomePipe()
            true
        }
        objectAction(23139) { client, _, position, _ ->
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
