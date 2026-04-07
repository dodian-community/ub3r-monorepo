# Netty Packet Gameplay To Kotlin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move all gameplay decisions and state mutations out of Java Netty inbound packet listeners into Kotlin systems-layer services, leaving listeners as decode/validate/dispatch adapters only.

**Architecture:** Keep packet decode and protocol-level bounds checks in `netty/listener/in` (Java), but route every gameplay action through Kotlin request models and services under `src/main/kotlin/net/dodian/uber/game/systems`. This preserves RS protocol safety while preventing gameplay logic from living in transport classes. Each migration step is protected by focused tests and boundary rules to prevent regressions.

**Tech Stack:** Java 17, Kotlin, Netty `ByteBuf`, JUnit 5, Gradle (`:game-server:test`)

---

### Task 1: Add A Boundary Test That Enforces Thin Netty Listeners

**Files:**
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ArchitectureBoundaryTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class NettyListenerBoundaryTest {
    @Test
    fun `netty inbound listeners only decode validate and delegate`() {
        val root = Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in")
        val violations = Files.walk(root)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".java") }
            .flatMap { path ->
                Files.readAllLines(path).stream().mapIndexed { index, line -> path to (index + 1) to line.trim() }
            }
            .filter { (_, line) ->
                line.contains("client.bankItem(") ||
                    line.contains("client.fromBank(") ||
                    line.contains("client.tradeItem(") ||
                    line.contains("client.stakeItem(") ||
                    line.contains("client.dropItem(") ||
                    line.contains("client.wear(")
            }
            .toList()

        assertTrue(violations.isEmpty(), "Gameplay mutations still exist in netty listeners: $violations")
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: FAIL with multiple violations in bank/item/walking listeners.

**Step 3: Adjust `ArchitectureBoundaryTest` to include this migration goal**

```kotlin
@Test
fun `netty inbound gameplay migration test class is present`() {
    val path = Paths.get("src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt")
    assertTrue(Files.exists(path), "Expected Netty listener boundary test file")
}
```

**Step 4: Run test to verify it still fails for the right reason**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest" --tests "net.dodian.uber.game.architecture.ArchitectureBoundaryTest"`
Expected: `ArchitectureBoundaryTest` PASS, new boundary test FAIL.

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/ArchitectureBoundaryTest.kt
git commit -m "test: add boundary guard for netty gameplay leakage"
```

### Task 2: Create Kotlin Packet Request Models And Facade

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketRequest.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketGameplayFacade.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketGameplayFacadeTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.net

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PacketGameplayFacadeTest {
    @Test
    fun `facade singleton is accessible`() {
        assertNotNull(PacketGameplayFacade)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.net.PacketGameplayFacadeTest"`
Expected: FAIL because classes do not exist yet.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

sealed interface PacketRequest

data class WalkRequest(
    val opcode: Int,
    val firstStepXAbs: Int,
    val firstStepYAbs: Int,
    val running: Boolean,
    val deltasX: IntArray,
    val deltasY: IntArray,
) : PacketRequest

object PacketGameplayFacade {
    @JvmStatic
    fun handleWalk(player: Client, request: WalkRequest) {
        PacketWalkingService.handle(player, request)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.net.PacketGameplayFacadeTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketRequest.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketGameplayFacade.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketGameplayFacadeTest.kt
git commit -m "feat: add kotlin packet request facade"
```

### Task 3: Move Walking Gameplay Side Effects To Kotlin

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketWalkingService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/WalkingListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/WalkingListenerRefactorTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.netty.listener.in

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class WalkingListenerRefactorTest {
    @Test
    fun `walking listener no longer mutates gameplay directly`() {
        val source = Files.readString(Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/WalkingListener.java"))
        assertFalse(source.contains("client.declineTrade("))
        assertFalse(source.contains("client.farming.updateCompost("))
        assertFalse(source.contains("PlayerActionCancellationService.cancel("))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.WalkingListenerRefactorTest"`
Expected: FAIL because those calls exist in listener.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.ui.dialogue.DialogueService

object PacketWalkingService {
    @JvmStatic
    fun handle(player: Client, request: WalkRequest) {
        if (player.inTrade && (request.opcode == 164 || request.opcode == 248)) player.declineTrade()
        if (player.inDuel && !player.duelFight && (request.opcode == 164 || request.opcode == 248)) player.declineDuel()

        if (request.deltasX.isNotEmpty()) {
            DialogueService.closeBlockingDialogue(player, false)
            player.send(RemoveInterfaces())
            PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.MOVEMENT, true, false, false, true)
            player.faceTarget(65535)
        }
    }
}
```

In `WalkingListener.java`, keep only:
- decode and bounds checks
- `WalkRequest` creation
- `PacketGameplayFacade.handleWalk(client, request)` call
- walking queue assignment (`newWalkCmdX/Y`), no business branches

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.WalkingListenerRefactorTest" --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: PASS for walking-specific assertions; boundary test may still fail on other listeners.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketWalkingService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/WalkingListener.java game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/WalkingListenerRefactorTest.kt
git commit -m "refactor: move walking gameplay logic into kotlin service"
```

### Task 4: Migrate Bank/Container Gameplay Routing To Kotlin

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketBankingService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankAllListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/Bank5Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/Bank10Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankX1Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankX2Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveItemListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MoveItemsListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/BankListenersBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.netty.listener.in

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class BankListenersBoundaryTest {
    @Test
    fun `bank listeners do not call inventory mutations directly`() {
        val files = listOf(
            "BankAllListener.java", "Bank5Listener.java", "Bank10Listener.java",
            "BankX1Listener.java", "BankX2Listener.java", "RemoveItemListener.java", "MoveItemsListener.java"
        )
        files.forEach { name ->
            val source = Files.readString(Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/$name"))
            assertFalse(source.contains("client.bankItem("), "$name still calls bankItem")
            assertFalse(source.contains("client.fromBank("), "$name still calls fromBank")
            assertFalse(source.contains("client.tradeItem("), "$name still calls tradeItem")
            assertFalse(source.contains("client.stakeItem("), "$name still calls stakeItem")
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.BankListenersBoundaryTest"`
Expected: FAIL in multiple listeners.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

object PacketBankingService {
    @JvmStatic
    fun handleFive(player: Client, interfaceId: Int, itemId: Int, slot: Int) {
        // copy exact existing branch behavior from Bank5Listener into here
    }

    @JvmStatic
    fun handleTen(player: Client, interfaceId: Int, itemId: Int, slot: Int) {
        // copy exact existing branch behavior from Bank10Listener into here
    }

    @JvmStatic
    fun handleAll(player: Client, interfaceId: Int, itemId: Int, slot: Int) {
        // copy exact existing branch behavior from BankAllListener into here
    }

    @JvmStatic
    fun handleX(player: Client, amount: Int) {
        // copy exact existing branch behavior from BankX2Listener into here
    }
}
```

In each Java listener, keep only decode/validation and call the service.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.BankListenersBoundaryTest" --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: bank boundary test PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketBankingService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankAllListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/Bank5Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/Bank10Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankX1Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/BankX2Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveItemListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MoveItemsListener.java game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/BankListenersBoundaryTest.kt
git commit -m "refactor: route bank packet gameplay to kotlin"
```

### Task 5: Migrate Item/Wear/Drop Gameplay Routing To Kotlin

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketItemActionService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItemListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItem2Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItem3Listener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/DropItemListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/WearItemListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ItemOnItemListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ItemOnGroundItemListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/ItemListenersBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.netty.listener.in

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class ItemListenersBoundaryTest {
    @Test
    fun `item listeners delegate to kotlin service`() {
        val source = Files.readString(Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ClickItemListener.java"))
        assertFalse(source.contains("if (item =="))
        assertFalse(source.contains("client.checkItemUpdate("))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.ItemListenersBoundaryTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

object PacketItemActionService {
    @JvmStatic
    fun handleClick1(player: Client, interfaceId: Int, slot: Int, itemId: Int) {
        // move existing ClickItemListener gameplay branches here
    }

    @JvmStatic
    fun handleWear(player: Client, interfaceId: Int, slot: Int, itemId: Int) {
        // move WearItemListener behavior here
    }

    @JvmStatic
    fun handleDrop(player: Client, slot: Int, itemId: Int) {
        // move DropItemListener behavior here
    }
}
```

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.ItemListenersBoundaryTest" --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: item boundary PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketItemActionService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItemListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItem2Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ClickItem3Listener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/DropItemListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/WearItemListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ItemOnItemListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ItemOnGroundItemListener.java game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/ItemListenersBoundaryTest.kt
git commit -m "refactor: move item packet gameplay to kotlin services"
```

### Task 6: Migrate Social/Chat/Command Gameplay Routing To Kotlin

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketSocialService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ChatListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UpdateChatListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/CommandsListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SyntaxInputListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AddFriendListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveFriendListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AddIgnoreListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveIgnoreListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SendPrivateMessageListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/SocialListenersBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.netty.listener.in

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SocialListenersBoundaryTest {
    @Test
    fun `commands listener only delegates`() {
        val source = Files.readString(Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/CommandsListener.java"))
        assertFalse(source.contains("CommandDispatcher.dispatch("))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.SocialListenersBoundaryTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.content.commands.CommandDispatcher

object PacketSocialService {
    @JvmStatic
    fun handleCommand(player: Client, command: String): Boolean =
        CommandDispatcher.dispatch(player, command)

    @JvmStatic
    fun handleAddFriend(player: Client, encodedName: Long) {
        player.addFriend(encodedName)
    }
}
```

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.SocialListenersBoundaryTest" --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: social boundary PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketSocialService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ChatListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UpdateChatListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/CommandsListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SyntaxInputListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AddFriendListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveFriendListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AddIgnoreListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/RemoveIgnoreListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SendPrivateMessageListener.java game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/SocialListenersBoundaryTest.kt
git commit -m "refactor: move social and command packet gameplay to kotlin"
```

### Task 7: Migrate Remaining Player Interaction Branches To Kotlin

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketInteractionService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AttackPlayerListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/FollowPlayerListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnPlayerListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnNpcListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnNpcListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnPlayerListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/PickUpGroundItemListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/InteractionListenersBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.netty.listener.in

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class InteractionListenersBoundaryTest {
    @Test
    fun `attack player listener does not construct gameplay task directly`() {
        val source = Files.readString(Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/AttackPlayerListener.java"))
        assertFalse(source.contains("new PlayerInteractionTask("))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.InteractionListenersBoundaryTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.systems.interaction.scheduler.PlayerInteractionTask
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object PacketInteractionService {
    @JvmStatic
    fun scheduleAttackPlayer(player: Client, victimSlot: Int, opcode: Int) {
        val intent = AttackPlayerIntent(opcode, PlayerRegistry.cycle, victimSlot)
        InteractionTaskScheduler.schedule(player, intent, PlayerInteractionTask(player, intent))
    }
}
```

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.InteractionListenersBoundaryTest" --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: interaction boundary PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketInteractionService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/AttackPlayerListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/FollowPlayerListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnPlayerListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/UseItemOnNpcListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnNpcListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/MagicOnPlayerListener.java game-server/src/main/java/net/dodian/uber/game/netty/listener/in/PickUpGroundItemListener.java game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/InteractionListenersBoundaryTest.kt
git commit -m "refactor: delegate player interaction packet gameplay to kotlin"
```

### Task 8: Final Verification And Boundary Lock

**Files:**
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketListenerManager.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt`

**Step 1: Expand boundary test to include all migrated listeners**

```kotlin
// add all listener filenames and forbidden gameplay call patterns
val forbiddenPatterns = listOf(
    "client.bankItem(", "client.fromBank(", "client.tradeItem(",
    "client.stakeItem(", "client.dropItem(", "CommandDispatcher.dispatch(",
    "InteractionTaskScheduler.schedule("
)
```

**Step 2: Run test to verify it fails before final cleanup**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: FAIL until last direct gameplay calls are removed.

**Step 3: Clean remaining direct calls and ensure delegation-only listeners**

Code target in listeners:

```java
PacketGameplayFacade.handleX(client, request);
```

No gameplay branch beyond:
- payload length checks
- slot/id bounds checks
- request creation
- delegation call

**Step 4: Run full targeted verification**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.*" --tests "net.dodian.uber.game.netty.listener.in.*BoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketListenerManager.java game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/*.java game-server/src/main/kotlin/net/dodian/uber/game/systems/net/*.kt game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/*BoundaryTest.kt
git commit -m "refactor: complete netty gameplay extraction into kotlin systems"
```

## Verification Checklist

Run in order:

1. `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
2. `./gradlew :game-server:test --tests "net.dodian.uber.game.netty.listener.in.*BoundaryTest"`
3. `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.*"`
4. `./gradlew :game-server:test`

Expected:
- No boundary violations
- No listener-level gameplay branches
- Existing interaction/object/npc flows remain functional

## Notes For Implementer

- Keep Java listeners as protocol adapters only; never add new business rules there.
- Preserve 600ms tick guarantees: no blocking APIs, no sleeps, no sync DB calls in listener/service paths.
- Keep ByteBuf parsing in Java listener layer, and pass primitive request values into Kotlin services.
- If a listener currently calls into already-modular Kotlin code (`InterfaceButtonService`, `InteractionTaskScheduler`), move only orchestration and state mutation into Kotlin service wrappers; do not rewrite stable domain modules.
- For high-risk handlers (`WalkingListener`, `BankX2Listener`), migrate incrementally with parity assertions and frequent commits.
