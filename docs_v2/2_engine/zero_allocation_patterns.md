# Zero-Allocation Patterns (GC Optimization)

This document outlines safe patterns to eliminate object allocations in high-frequency code paths. Reducing allocations is the most effective way to eliminate cycle-time "spikes" caused by Garbage Collection (GC).

---

## 🚀 1. Primitive-Based Position Logic
**Problem**: Calling `entity.getPosition()` often returns a new `Position` object or encourages the creation of one for comparisons.
**Optimization**: Provide primitive-based overloaded methods for all distance and boundary checks.
- **Instead of**: `if (player.getPosition().withinDistance(npc.getPosition(), 1))`
- **Use**: `if (player.isWithinDistance(npc, 1))` which compares `player.x` and `player.y` directly without object overhead.
- **Tech**: Use bit-packing to store X, Y, and Z in a single `Long` for storage, and unpack them only when needed.

## 🚀 2. Index-Based Loops (Avoid Iterators)
**Problem**: The `for (Npc npc : localNpcs)` syntax creates a hidden `Iterator` object every time it is called.
**Optimization**: Use indexed for-loops for all internal collections.
- **Instead of**: `localPlayers.forEach { ... }`
- **Use**: 
  ```java
  for (int i = 0; i < playerListSize; i++) {
      Player p = playerList[i];
      if (p == null) continue;
      // logic
  }
  ```
- **Note**: This is only safe if the underlying data structure is an Array. `PlayerRegistry` and `NpcManager` already use arrays, making this a very safe win.

## 🚀 3. Thread-Local Scratch Buffers
**Problem**: Building update blocks for 200+ players often involves allocating temporary `ByteMessage` or `ByteBuf` objects.
**Optimization**: Utilize the `ThreadLocalSyncScratch` pattern already present in the engine.
- Every thread (Netty and Game) should have one pre-allocated, large `DirectByteBuf`.
- Instead of `new ByteMessage()`, call `scratch.clear()`.
- **Safe because**: The Game Thread is single-threaded, so there is zero risk of one player's data overwriting another's during the scratch use.

## 🚀 4. Reusable Update Flags
**Problem**: Tracking what changed (`APPEARANCE`, `ANIM`) often uses `EnumSet` or `HashSet<UpdateFlag>`.
**Optimization**: Use a **Bitmask (Int or Long)** inside the `Entity` class.
- `private int updateMask = 0;`
- Setting a flag: `updateMask |= UpdateFlag.ANIM.getMask();`
- Checking a flag: `(updateMask & UpdateFlag.ANIM.getMask()) != 0;`
- **Result**: Zero objects created to track entity state changes.

## 🚀 5. Packet Pooling (Advanced but Safe)
**Problem**: Netty creates a `new GamePacket()` for every single opcode received.
**Optimization**: Implement a simple **Object Pool** for the `GamePacket` class.
- When a packet is processed and finished, call `packet.release()`.
- The `release()` method puts the object back into a `ConcurrentStack`.
- The `GamePacketDecoder` grabs from the stack instead of calling `new`.

---

## Summary of GC Impact
| Pattern | Hot Path | GC Reduction | Risk |
| :--- | :--- | :--- | :--- |
| **Primitive Distance** | Combat/Pathing | ⭐⭐⭐⭐⭐ | Low |
| **Indexed Loops** | Synchronization | ⭐⭐⭐⭐ | Low |
| **Bitmask Flags** | State Management | ⭐⭐⭐ | Very Low |
| **Scratch Buffers** | Networking | ⭐⭐⭐⭐ | Low |
| **Packet Pooling** | Networking | ⭐⭐⭐ | Medium |
