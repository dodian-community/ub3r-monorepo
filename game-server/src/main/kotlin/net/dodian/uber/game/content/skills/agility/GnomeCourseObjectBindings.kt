package net.dodian.uber.game.content.skills.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.content.skills.agility.AgilityCourseService
import net.dodian.uber.game.systems.interaction.FirstClickDslObjectContent
import net.dodian.uber.game.systems.interaction.firstClickObjectActions

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
    ): ContentObjectInteractionPolicy? {
        if (option != 1 || objectId !in objectIds) {
            return null
        }
        return ContentInteraction.legacyObjectDistancePolicy()
    }
}
