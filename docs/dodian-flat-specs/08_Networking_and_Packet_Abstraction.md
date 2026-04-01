# Spec 08: Networking and Packet Abstraction
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> `PacketOpcodes.kt` and the `OutboundPackets.kt` extension functions are **wrappers over existing Java packet classes** — they do not add new protocol features.
> All Kotlin extension functions created in this spec must produce the exact same client behavior as the Java calls they replace.
> **Do not modify the `UpstreamHandler.java` opcode table** beyond the specific lines required to wire new `InteractionIntent` types as described in Spec 09.

---

## 1. Executive Summary

The networking layer in `game-server old` is mature and functional. The Netty pipeline (`io.netty:netty-all:4.1.108.Final`) handles all TCP communication. The Java-side code in `src/main/java/net/dodian/uber/game/netty/` is complex and load-bearing. This spec defines what can and cannot be changed, and how to add a Kotlin abstraction layer on top without disturbing the existing Netty machinery.

**The explicit rule for this phase:** The Netty bootstrap, codec, pipeline, and all inbound packet handlers in Java are **frozen**. No changes to Java files in `netty/` are permitted as part of this restructuring. New code can be written in Kotlin that wraps the Java packet classes.

---

## 2. The Actual Networking Structure

### 2.1 Java Files (Frozen — Do Not Touch)

```
src/main/java/net/dodian/uber/game/netty/
    ├── UpstreamHandler.java          ← Incoming packet router — all client opcodes dispatched here
    ├── LoginProcessorHandler.java    ← Login flow: auth, world entry
    ├── GamePacketEncoder.java        ← Outgoing packets encoded to ByteBuf
    ├── SessionInteractor.java        ← Session-level utilities
    ├── listener/
    │   └── out/                      ← 60+ outbound packet classes (Java)
    │       ├── SendMessage.java      ← Chat message
    │       ├── ShowInterface.java    ← Open interface
    │       ├── RefreshSkill.java     ← Update skill display
    │       ├── SetSidebarInterface.java
    │       ├── RemoveInterfaces.java
    │       ├── SendInventory.java
    │       ├── SendBankItems.java
    │       ├── SendString.java
    │       ├── Sound.java
    │       ├── SetVarbit.java
    │       ├── SendFrame246.java     ← Item model on interface
    │       ├── SendFrame200.java     ← Animation on interface
    │       └── ... (40+ more)
    └── decode/
        └── ... (inbound packet decoders)
```

### 2.2 Outbound Packet Java Classes — Relationship to `Client.send()`

The `Client.send(OutboundPacketListener listener)` method exists on the Java `Client` class. All outbound packets call `client.send(XxxPacket(...))`. Kotlin code already does this:

```kotlin
// Example from existing Kotlin code:
client.send(SendMessage("Hello!"))         // SendMessage is a Java class
client.send(RemoveInterfaces())             // RemoveInterfaces is a Java class
client.send(RefreshSkill(skillId, xp, lvl))
```

This pattern is correct. Kotlin wraps the Java packet calls via extension functions (see Spec 04 and Spec 06 for the extension wrappers).

### 2.3 The Login Flow (How It Works Today)

1. Player connects via TCP → Netty's `LoginProcessorHandler.java`
2. Username + password received → credential check via MySQL (`PlayerSaveRepository.loadPlayer()`)
3. If login OK → `PlayerSaveRepository` loads the player data into a `Client` object
4. `Client` is added to the `PlayerRegistry`
5. Bootstrap packets are sent (sidebar interfaces, skill levels, inventory, bank)
6. `PlayerLoginEvent` should fire here (currently does not — must be added in Spec 02)

### 2.4 Logout Flow (How It Works Today)

1. Player disconnects (TCP close) or `/logout` button
2. `SessionInteractor` detects connection close
3. `PlayerSaveService.queueSave(client)` is called
4. `PlayerSaveService` builds a `PlayerSaveEnvelope` from `Client` fields
5. `PlayerSaveSqlRepository` encodes the envelope to SQL and executes via `PlayerSaveRepository`
6. `PlayerLogoutEvent` should fire here before the save (currently does not — must be added in Spec 02)

---

## 3. What Content Developers Must Never Touch

The following are strictly off-limits for content developers:

| Forbidden Access | Why |
|:---|:---|
| `UpstreamHandler.java` directly | Any change here risks dropping or misrouting packets |
| `GamePacketEncoder.java` | Changing encoding without updating the client binary causes log-out storms |
| `ByteBuf` / buffer manipulation | Raw Netty buffer operations belong only in `engine.net` |
| Raw opcode integers in content | Opcodes belong in `engine.net.PacketOpcodes` |
| `io.netty.*` imports in `content.*` | Absolute prohibition |

---

## 4. The Kotlin Net Bridge Layer

The Kotlin net bridge lives at `net.dodian.uber.game.engine.net`. Its role is to translate inbound `InboundPacket` objects (decoded by Java) into game events, and to provide Kotlin helpers for outbound packet creation.

### 4.1 Current Kotlin Net Bridge Files (What Exists)

```
engine/net/
    GameMessage.kt             ← Sealed interface for typed inbound messages
    GameSignal.kt              ← Sealed interface for typed outbound signals
    PacketOpcodes.kt           ← All opcode constants (do not use raw ints in content)
    PlayerChannel.kt           ← Kotlin wrapper around the player's Netty channel
    NettyPipelineBootstrap.kt  ← Kotlin side of Netty setup (if present)
```

> **Note:** Verify the actual files present in `engine/net/` before assuming. The above reflects expected files based on the 3-tier architecture goal. Add any additional files found during migration.

### 4.2 `PacketOpcodes.kt` — Opcode Constant Registry

All inbound and outbound opcodes must be defined here. Content developers reference opcode names, never raw integers.

```kotlin
// net.dodian.uber.game.engine.net.PacketOpcodes
package net.dodian.uber.game.engine.net

object PacketOpcodes {
    // ─── Inbound (Client → Server) ───────────────────────────────────────
    // Walk
    const val WALK_FIRST_STEP = 164
    const val WALK_NEXT_STEP  = 98
    const val WALK_MINI_MAP   = 248

    // NPC interactions
    const val NPC_CLICK_OPT1  = 72
    const val NPC_CLICK_OPT2  = 17
    const val NPC_CLICK_OPT3  = 21
    const val NPC_CLICK_OPT4  = 18
    const val NPC_CLICK_OPT1B = 155 // alt NPC optClick

    // Object interactions
    const val OBJECT_OPT1     = 132
    const val OBJECT_OPT2     = 252
    const val OBJECT_OPT3     = 70
    const val OBJECT_OPT4     = 234
    const val OBJECT_OPT5     = 228

    // Item interactions
    const val ITEM_OPT1       = 122
    const val ITEM_OPT2       = 41
    const val ITEM_OPT3       = 16
    const val ITEM_OPT4       = 75
    const val ITEM_ON_ITEM    = 53
    const val ITEM_ON_OBJECT  = 192
    const val ITEM_ON_NPC     = 57

    // Player interaction
    const val PLAYER_OPT1     = 73
    const val PLAYER_OPT2     = 43  // attack player
    const val PLAYER_OPT5     = 57  // trade player (verify — shares with item_on_npc?)

    // Magic
    const val MAGIC_ON_NPC    = 131
    const val MAGIC_ON_PLAYER = 249
    const val MAGIC_ON_OBJECT = 35
    const val MAGIC_ON_ITEM   = 3

    // Interface buttons
    const val BUTTON_CLICK    = 185

    // Dialogue
    const val DIALOGUE_CONTINUE = 40

    // Command / input
    const val COMMAND         = 103
    const val ENTER_AMOUNT    = 208
    const val ENTER_NAME      = 60

    // Chat
    const val CHAT_PUBLIC     = 4
    const val CHAT_CLAN       = 11

    // ─── Outbound (Server → Client) ──────────────────────────────────────
    const val OUT_MESSAGE           = 253
    const val OUT_SEND_STRING       = 126
    const val OUT_SHOW_INTERFACE    = 97
    const val OUT_REMOVE_INTERFACES = 219
    const val OUT_REFRESH_SKILL     = 134
    const val OUT_SET_SIDEBAR       = 71
    const val OUT_SEND_INVENTORY    = 53
    const val OUT_SEND_VARBIT       = 200
    const val OUT_SEND_SOUND        = 174
    const val OUT_ANIMATION         = 238
    const val OUT_GFX               = 36
}
```

---

## 5. The Outbound Packet Extension API

Rather than a new packet class hierarchy, Kotlin extension functions on `Client` provide the developer-facing API. These are what Spec 04 and Spec 06 reference:

```kotlin
// net.dodian.uber.game.systems.api.packet.OutboundPackets.kt
// (These may already exist — collect them all here for reference)

// ─── Messaging ───────────────────────────────────────────────
fun Client.sendMessage(text: String) {
    send(SendMessage(text))
}

fun Client.sendRedMessage(text: String) {
    sendMessage("<col=FF0000>$text</col>")
}

fun Client.sendGameMessage(text: String) {
    sendMessage(":: $text")
}

// ─── Interface Control ───────────────────────────────────────
fun Client.openInterface(interfaceId: Int) {
    send(ShowInterface(interfaceId))
}

fun Client.closeInterfaces() {
    send(RemoveInterfaces())
}

fun Client.setInterfaceText(componentId: Int, text: String) {
    send(SendString(text, componentId))
}

fun Client.setWalkableInterface(interfaceId: Int) {
    send(SetWalkableInterface(interfaceId))
}

// ─── Sound ───────────────────────────────────────────────────
fun Client.playSound(soundId: Int, delay: Int = 0, volume: Int = 1) {
    send(Sound(soundId, delay, volume))
}

// ─── Skills ──────────────────────────────────────────────────
fun Client.refreshSkill(skillId: Int) {
    send(RefreshSkill(skillId, playerXP[skillId], levelForXp[skillId]))
}

fun Client.refreshAllSkills() {
    for (i in 0 until 21) refreshSkill(i)
}

// ─── Items ───────────────────────────────────────────────────
fun Client.refreshInventory() {
    send(SendInventory(playerItems, playerItemsN))
}

fun Client.refreshBank() {
    send(SendBankItems(bankItems, bankItemsN))
}

fun Client.refreshEquipment() {
    send(SendInventory(playerEquipment, playerEquipmentN /*, interfaceId */))
}

// ─── Animation / Visual ──────────────────────────────────────
fun Client.animate(animation: Int) {
    send(AnimationPacket(slot, animation))
}

fun Client.gfx(gfxId: Int, height: Int = 0) {
    send(GfxPacket(slot, gfxId, height))
}

// ─── Varbit ──────────────────────────────────────────────────
fun Client.setVarbit(id: Int, value: Int) {
    send(SetVarbit(id, value))
}
```

---

## 6. The Inbound Packet → Game Event Bridge

Inbound packets arrive from Netty on worker threads. They must not call `GameEventBus.post()` directly. Instead, they are queued and processed on the game thread during the input phase of each tick.

### 6.1 The `InboundPacketQueue` Pattern

```kotlin
// net.dodian.uber.game.engine.net.InboundPacketQueue (may already exist)
// This is a thread-safe bounded queue attached to each Client
class InboundPacketQueue {
    private val queue: ConcurrentLinkedQueue<InboundPacket> = ConcurrentLinkedQueue()

    // Called by Netty worker threads
    fun offer(packet: InboundPacket) {
        queue.offer(packet)
    }

    // Called by game thread during the input phase
    fun drain(maxPackets: Int = 20): List<InboundPacket> {
        val result = ArrayList<InboundPacket>(maxPackets)
        repeat(maxPackets) {
            result.add(queue.poll() ?: return result)
        }
        return result
    }
}
```

### 6.2 The Game Thread Input Phase

During each tick, the game loop's input phase drains each player's queue and dispatches the packets as game events:

```kotlin
// In the Game Loop Input Phase (net.dodian.uber.game.engine.phases.InputPhase)
// NOTE: The current codebase uses InteractionProcessor — this is the target pattern
for (player in PlayerRegistry.activePlayers) {
    val packets = player.inboundQueue.drain()
    for (packet in packets) {
        when (packet.opcode) {
            PacketOpcodes.NPC_CLICK_OPT1 -> {
                player.pendingInteraction = NpcInteractionIntent.from(packet)
            }
            PacketOpcodes.BUTTON_CLICK -> {
                GameEventBus.post(ButtonClickEvent(player, packet.readButtonId()))
            }
            PacketOpcodes.COMMAND -> {
                val cmd = packet.readCommand()
                GameEventBus.post(CommandEvent(player, cmd.name, cmd.args))
            }
            // etc.
        }
    }
}
```

### 6.3 Current State vs Target State

Currently, `UpstreamHandler.java` in Java handles much of this dispatch logic directly. The full migration of inbound packet routing to a Kotlin input phase is a future goal. For now:

- Packets that set `pendingInteraction` (NPC clicks, object clicks) continue to be wired through `UpstreamHandler.java`
- Packets that post directly to `GameEventBus` (button clicks, commands, dialogue continues) should be transitioned to Kotlin wiring
- This is a **partial** migration — complete packet migration is out of scope for this phase

---

## 7. Login Bootstrap Packet Sequence

On successful login, the server sends an ordered sequence of packets to initialize the client UI state. This sequence is critical — packets in the wrong order cause the client to display incorrectly. The current sequence (must be preserved):

```kotlin
// Current/target login bootstrap order (from existing code — verify exact order):
1. Magic Level sidebar setup: setSidebar(MAGIC, SIDEBAR_MAGIC)
2. Skill levels refresh: refreshAllSkills()
3. Inventory refresh
4. Equipment refresh
5. Run energy
6. Prayer points
7. Sidebars set: setSidebar for each of combat, stats, quests, inv, equip, prayer, magic, friends, logout, settings, emotes, music
8. Refresh bank (or mark bank as dirty for refresh on open)
9. Player options (right-click options)
10. World time / varbit setup
11. Spawn known NPCs in viewport
12. Spawn known ground items in viewport
```

This sequence lives in the login handler and must not be reorganized.

---

## 8. Disconnection Handling

A player disconnects when:
1. TCP connection closes (socket close event)
2. Netty fires `channelInactive()` on `UpstreamHandler`
3. `client.disconnected = true` is set
4. On the next game tick, the processing phase detects `client.disconnected`
5. `PlayerSaveService.queueSave(client)` is called
6. After save completes, `PlayerRegistry.remove(client)` is called

Content code must:
- Check `client.disconnected` and `client.isActive` at the top of repeating tasks
- Never hold a reference to a `Client` object past its session (use `WeakHashMap` for session maps)

---

## 9. Packet Size Limits

The 317 protocol defines maximum packet sizes per opcode. Exceeding these causes client parsing errors. Key limits:

| Type | Max Size |
|:---|:---|
| Standard packet | 4096 bytes |
| Bank display | 720 slots × 8 bytes = ~5.7KB (fragmented if needed) |
| Chat message | 80 characters |
| Interface string | 255 characters |

Content developers sending `String` values longer than 255 characters via `setInterfaceText()` must truncate:
```kotlin
fun Client.setInterfaceTextSafe(componentId: Int, text: String) {
    setInterfaceText(componentId, text.take(255))
}
```

---

## 10. Adding a New Custom Packet

If custom server-side packets need to be added (for server-to-server communication or a custom client), follow this procedure:
1. Choose an available opcode number (verify not already used by the 317 client protocol)
2. Add the opcode constant to `PacketOpcodes.kt`
3. Create the Java packet class in `netty/listener/out/` following the existing class pattern
4. Add a Kotlin extension function wrapper in `systems.api.packet.OutboundPackets.kt`
5. **Do not modify `GamePacketEncoder.java` or the codec chain** without also updating the client binary

---

## 11. Definition of Done for Spec 08

- [ ] `PacketOpcodes.kt` created with all inbound and outbound opcode constants from Section 4
- [ ] All Kotlin extension functions on `Client` documented in `OutboundPackets.kt` (Section 5)
- [ ] Zero `io.netty.*` imports exist in any `content.*` file
- [ ] Zero raw opcode integers exist in any `content.*` file
- [ ] `LoginProcessorHandler.java` is untouched
- [ ] `UpstreamHandler.java` is untouched
- [ ] `GamePacketEncoder.java` is untouched
- [ ] All 60+ Java packet classes in `netty/listener/out/` are untouched
- [ ] `PlayerLoginEvent` and `PlayerLogoutEvent` fire from the login/logout flow (see Spec 02)
- [ ] The login bootstrap packet sequence is documented and preserved
- [ ] Content developers have all needed extension functions to send any standard packet without importing Java packets
