package net.dodian.uber.game.netty.listener.`in`

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class InteractionListenersBoundaryTest {
    @Test
    fun `attack player listener delegates interaction scheduling to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/AttackPlayerListener.java"),
        )

        assertTrue(
            source.contains("PacketInteractionService.handleAttackPlayer("),
            "Expected AttackPlayerListener to route through PacketInteractionService.handleAttackPlayer",
        )
        assertFalse(source.contains("new AttackPlayerIntent("))
        assertFalse(source.contains("new PlayerInteractionTask("))
        assertFalse(source.contains("InteractionTaskScheduler.schedule("))
        assertFalse(source.contains("PlayerRegistry.getClient("))
        assertFalse(source.contains("client.deathStage"))
        assertFalse(source.contains("client.randomed"))
        assertFalse(source.contains("client.UsingAgility"))
    }

    @Test
    fun `follow player listener delegates trade side effects to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/FollowPlayerListener.java"),
        )

        assertTrue(
            source.contains("PacketInteractionRequestService.handleTradeRequest(client, followId, other)"),
            "Expected FollowPlayerListener to route through PacketInteractionRequestService.handleTradeRequest",
        )
        assertFalse(source.contains("Player.openPage("))
        assertFalse(source.contains("modcp&action=search"))
    }

    @Test
    fun `trade listener delegates follow side effects to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/TradeListener.java"),
        )

        assertTrue(
            source.contains("PlayerClickListener.handleFollowPlayer(client, targetSlot)"),
            "Expected TradeListener to route through PlayerClickListener.handleFollowPlayer",
        )
        assertFalse(source.contains("PacketInteractionRequestService.handleTradeRequest("))
    }

    @Test
    fun `use item on npc listener delegates interaction scheduling to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnNpcListener.java"),
        )

        assertTrue(
            source.contains("PacketInteractionService.handleUseItemOnNpc("),
            "Expected UseItemOnNpcListener to route through PacketInteractionService.handleUseItemOnNpc",
        )
        assertFalse(source.contains("new ItemOnNpcIntent("))
        assertFalse(source.contains("new NpcInteractionTask("))
        assertFalse(source.contains("InteractionTaskScheduler.schedule("))
        assertFalse(source.contains("Server.npcManager.getNpc("))
        assertFalse(source.contains("client.randomed"))
        assertFalse(source.contains("client.UsingAgility"))
    }

    @Test
    fun `use item on player listener delegates gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnPlayerListener.java"),
        )

        assertTrue(
            source.contains("PacketItemActionService.handleUseItemOnPlayer("),
            "Expected UseItemOnPlayerListener to route through PacketItemActionService.handleUseItemOnPlayer",
        )
        assertFalse(source.contains("client.playerPotato"))
        assertFalse(source.contains("client.deleteItem("))
        assertFalse(source.contains("target.addItem("))
    }

    @Test
    fun `magic listeners delegate gameplay to kotlin service`() {
        val npcSource = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnNpcListener.java"),
        )
        val playerSource = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnPlayerListener.java"),
        )

        assertTrue(npcSource.contains("PacketMagicService.handleMagicOnNpc("))
        assertFalse(npcSource.contains("InteractionTaskScheduler.schedule("))
        assertFalse(npcSource.contains("client.magicId ="))

        assertTrue(playerSource.contains("PacketMagicService.handleMagicOnPlayer("))
        assertFalse(playerSource.contains("InteractionTaskScheduler.schedule("))
        assertFalse(playerSource.contains("client.magicId ="))
    }

    @Test
    fun `pickup listener delegates ground item gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/PickUpGroundItemListener.java"),
        )

        assertTrue(
            source.contains("PacketPickupService.handle(client, itemId, itemX, itemY)"),
            "Expected PickUpGroundItemListener to route through PacketPickupService.handle",
        )
        assertFalse(source.contains("client.pickUpItem("))
        assertFalse(source.contains("PlayerActionCancellationService.cancel("))
        assertFalse(source.contains("client.send("))
    }

    @Test
    fun `npc interaction listener delegates click scheduling to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java"),
        )

        assertTrue(
            source.contains("PacketInteractionService.handleNpcClick(client, packet.opcode(), 1, npcIndex);") &&
                source.contains("PacketInteractionService.handleNpcClick(client, packet.opcode(), 2, npcIndex);") &&
                source.contains("PacketInteractionService.handleNpcClick(client, packet.opcode(), 3, npcIndex);") &&
                source.contains("PacketInteractionService.handleNpcClick(client, packet.opcode(), 4, npcIndex);"),
            "Expected NpcInteractionListener click handlers to route through PacketInteractionService.handleNpcClick",
        )
        assertFalse(source.contains("new NpcInteractionIntent("))
        assertFalse(source.contains("new NpcInteractionTask("))
        assertFalse(source.contains("InteractionTaskScheduler.schedule("))
        assertFalse(source.contains("client.playerPotato"))
        assertFalse(source.contains("client.randomed"))
        assertFalse(source.contains("client.UsingAgility"))
    }

    @Test
    fun `npc interaction listener delegates npc attack orchestration to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java"),
        )

        assertTrue(
            source.contains("PacketInteractionService.handleNpcAttack(client, packet.opcode(), npcIndex);"),
            "Expected NpcInteractionListener attack handler to route through PacketInteractionService.handleNpcAttack",
        )
        assertFalse(source.contains("PacketMagicService.clearMagicIdIfSet(client)"))
        assertFalse(source.contains("client.deathStage"))
        assertFalse(source.contains("client.randomed"))
        assertFalse(source.contains("client.UsingAgility"))
    }

    @Test
    fun `object interaction listener delegates magic on object gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java"),
        )
        val methodSource = extractMethodBody(source, "private void handleMagicOnObject(Client client, GamePacket packet)")

        assertTrue(
            methodSource.contains("PacketMagicService.handleMagicOnObject(client, packet.opcode(), objectX, objectY, objectId, spellId);"),
            "Expected handleMagicOnObject to route through PacketMagicService.handleMagicOnObject",
        )
        assertFalse(methodSource.contains("new MagicOnObjectIntent("))
        assertFalse(methodSource.contains("new ObjectInteractionTask("))
        assertFalse(methodSource.contains("InteractionTaskScheduler.schedule("))
        assertFalse(methodSource.contains("client.randomed"))
        assertFalse(methodSource.contains("client.UsingAgility"))
        assertFalse(methodSource.contains("Misc.getObject("))
        assertFalse(methodSource.contains("GameObjectData.forId("))
    }

    @Test
    fun `object interaction listener delegates item on object gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java"),
        )
        val methodSource = extractMethodBody(source, "private void handleItemOnObject(Client client, GamePacket packet)")

        assertTrue(
            methodSource.contains("PacketObjectService.handleItemOnObject(client, packet.opcode(), interfaceId, objectId, objectX, objectY, itemSlot, itemId);"),
            "Expected handleItemOnObject to route through PacketObjectService.handleItemOnObject",
        )
        assertFalse(methodSource.contains("new ItemOnObjectIntent("))
        assertFalse(methodSource.contains("new ObjectInteractionTask("))
        assertFalse(methodSource.contains("InteractionTaskScheduler.schedule("))
        assertFalse(methodSource.contains("client.randomed"))
        assertFalse(methodSource.contains("Misc.getObject("))
        assertFalse(methodSource.contains("GameObjectData.forId("))
    }

    @Test
    fun `object interaction listener delegates click gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java"),
        )
        val methodSource = extractMethodBody(source, "private void handleClick(Client client, GamePacket packet, int option)")

        assertTrue(
            methodSource.contains("PacketObjectService.handleObjectClick(client, packet.opcode(), option, objectId, objectX, objectY);") ,
            "Expected handleClick to route through PacketObjectService.handleObjectClick",
        )
        assertFalse(methodSource.contains("new ObjectClickIntent("))
        assertFalse(methodSource.contains("new ObjectInteractionTask("))
        assertFalse(methodSource.contains("InteractionTaskScheduler.schedule("))
        assertFalse(methodSource.contains("client.randomed"))
        assertFalse(methodSource.contains("client.UsingAgility"))
        assertFalse(methodSource.contains("Misc.getObject("))
        assertFalse(methodSource.contains("GameObjectData.forId("))
    }

    private fun extractMethodBody(source: String, signature: String): String {
        val signatureIndex = source.indexOf(signature)
        require(signatureIndex >= 0) { "Method signature not found: $signature" }

        val bodyStart = source.indexOf('{', signatureIndex)
        require(bodyStart >= 0) { "Method body start not found for: $signature" }

        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(bodyStart + 1, index)
                    }
                }
            }
        }

        error("Method body end not found for: $signature")
    }
}

