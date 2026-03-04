package net.dodian.uber.game.runtime.interaction

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.WalkToTask
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.runtime.interaction.task.InteractionExecutionResult
import net.dodian.uber.game.runtime.task.GameTaskRuntime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InteractionProcessorTest {

    @AfterEach
    fun tearDown() {
        GameTaskRuntime.clear()
        PlayerHandler.playersOnline.clear()
    }

    @Test
    fun `mining click waits while player movement queue is not settled`() {
        val player = miningReadyPlayer(slot = 201, dbId = 9201)
        val intent = miningObjectClickIntent(player)
        player.walkToTask = intent.task
        player.pendingInteraction = intent
        player.interactionEarliestCycle = PlayerHandler.cycle.toLong()
        player.addToWalkingQueue(1, 1)

        val result = InteractionProcessor.process(player)

        assertEquals(InteractionExecutionResult.WAITING, result)
        assertNotNull(player.pendingInteraction)
    }

    @Test
    fun `mining click completes once movement is settled`() {
        val originalCycle = PlayerHandler.cycle
        val player = miningReadyPlayer(slot = 202, dbId = 9202)
        val intent = miningObjectClickIntent(player)
        player.walkToTask = intent.task
        player.pendingInteraction = intent
        player.interactionEarliestCycle = PlayerHandler.cycle.toLong()

        try {
            val initialSettledResult = InteractionProcessor.process(player)
            assertEquals(InteractionExecutionResult.WAITING, initialSettledResult)
            assertNotNull(player.pendingInteraction)

            PlayerHandler.cycle++
            val result = InteractionProcessor.process(player)

            assertEquals(InteractionExecutionResult.COMPLETE, result)
            assertNull(player.pendingInteraction)
        } finally {
            PlayerHandler.cycle = originalCycle
        }
    }

    private fun miningReadyPlayer(slot: Int, dbId: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.dbId = dbId
        client.validClient = true
        client.pLoaded = true
        client.isActive = true
        client.disconnected = false
        client.randomed = false
        client.UsingAgility = false
        client.getPosition().moveTo(2600, 3100, 0)
        client.setLevel(61, Skill.MINING)
        client.setExperience(1_000_000, Skill.MINING)
        client.getEquipment()[Equipment.Slot.WEAPON.id] = 1265
        PlayerHandler.playersOnline[slot.toLong()] = client
        return client
    }

    private fun miningObjectClickIntent(player: Client): ObjectClickIntent {
        val target = Position(player.position.x + 1, player.position.y, player.position.z)
        val task = WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, 7451, target)
        return ObjectClickIntent(
            opcode = 132,
            createdCycle = PlayerHandler.cycle.toLong(),
            option = 1,
            objectId = 7451,
            objectPosition = target,
            task = task,
            objectData = null,
            objectDef = null,
        )
    }
}
