package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentDispatchContractBoundaryTest {
    @Test
    fun `item dispatcher uses event then skill plugin then domain registry order`() {
        val source = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/items/ItemDispatcher.kt"))
        val eventIndex = source.indexOf("GameEventBus.postWithResult")
        val skillIndex = source.indexOf("SkillInteractionDispatcher.tryHandleItemClick")
        val registryIndex = source.indexOf("ItemContentRegistry.get")

        assertTrue(eventIndex >= 0, "ItemDispatcher must emit an item click event before resolving routes")
        assertTrue(skillIndex >= 0, "ItemDispatcher must route through SkillInteractionDispatcher")
        assertTrue(registryIndex >= 0, "ItemDispatcher must fallback to ItemContentRegistry")
        assertTrue(eventIndex < skillIndex && skillIndex < registryIndex, "Expected Event -> SkillPlugin -> Domain fallback order")
    }

    @Test
    fun `button flow keeps event then skill plugin then interface registry order`() {
        val listenerSource = Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/ClickingButtonsListener.java"))
        val serviceSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/ui/buttons/InterfaceButtonService.kt"))

        assertTrue(listenerSource.contains("GameEventBus.postWithResult(new ButtonClickEvent(request))"))
        assertTrue(serviceSource.contains("SkillInteractionDispatcher.tryHandleButton"))
        assertTrue(serviceSource.contains("InterfaceButtonRegistry.tryHandle"))
    }

    @Test
    fun `core interaction flows use typed state adapters`() {
        val pickupSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketPickupService.kt"))
        val walkingSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketWalkingService.kt"))
        val deferredSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/lifecycle/PlayerDeferredLifecycleService.kt"))
        val interactionSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/InteractionProcessor.kt"))

        assertTrue(pickupSource.contains("GroundItemIntentStateAdapter"))
        assertTrue(walkingSource.contains("GroundItemIntentStateAdapter"))
        assertTrue(deferredSource.contains("GroundItemIntentStateAdapter"))
        assertTrue(interactionSource.contains("InteractionSessionStateAdapter"))
        assertTrue(interactionSource.contains("TeleportIntentStateAdapter"))
    }
}
