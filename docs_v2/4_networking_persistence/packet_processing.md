# Packet Processing Pipeline

## Overview
A "Packet" is the fundamental unit of communication between the client and the server. The server must safely and efficiently translate a stream of raw bytes into actionable game logic.

## The Inbound Pipeline

### 1. Decoding (`GamePacketDecoder`)
When bytes arrive, Netty pushes them to `GamePacketDecoder.java`.
- **Opcode Decryption**: The first byte is the opcode. The server subtracts the next value from the inbound `ISAACCipher` to reveal the true opcode.
- **Size Resolution**: Based on a hardcoded array (`Constants.PACKET_SIZES`), the decoder knows if this opcode has a fixed size (e.g., 2 bytes) or a variable size (where the next 1 or 2 bytes dictate the length).
- **Collection**: Once enough bytes are gathered, it creates a `GamePacket` object and passes it down the pipeline.

### 2. Thread Hand-off (`GamePacketHandler`)
Netty worker threads are *not* allowed to modify the game world. 
When `GamePacketHandler.java` receives the `GamePacket`:
1.  It checks the player's rate limit (preventing packet spam).
2.  It calls `client.queueInboundPacket(packet)`, which places the packet into a `ConcurrentLinkedQueue` (the `InboundPacketMailbox`).

### 3. Execution (`InboundPacketPhase`)
Every 600ms, the Game Thread iterates through all online players and empties their mailboxes.
1.  For each packet, it looks up the opcode in the `PacketListenerManager`.
2.  If a listener is registered (e.g., `ObjectInteractionListener` for opcode 132), it calls `listener.handle(client, packet)`.
3.  The listener reads the data using specific methods (like `readLEShortA()` or `readUnsignedByte()`) and triggers the corresponding game logic.

## The Outbound Pipeline

### 1. Encoding (`ByteMessage`)
When game logic wants to send data to the client, it constructs an outbound packet.
```kotlin
val msg = SendMessage("Welcome to Dodian!")
client.send(msg)
```

### 2. Encryption (`ByteMessageEncoder`)
The `send()` method passes the message to Netty's `ByteMessageEncoder.java`.
- It writes the opcode, adding the next value from the outbound `ISAACCipher` for encryption.
- It writes the payload.

### 3. Flushing (`OutboundPacketProcessor`)
To optimize network I/O, the server does not instantly push every single packet to the TCP socket. Instead, packets are buffered. At the very end of the 600ms tick, `OutboundPacketProcessor.run()` triggers a `flush()` on all active Netty channels, sending the data across the network in one efficient burst.

## Important: Custom Data Types
As detailed in `1_architecture/client_protocol.md`, reading and writing packets requires precise alignment with the Mystic Client's custom data types (e.g., `ValueType.ADD`, `ByteOrder.LITTLE`). Always refer to the client's `Buffer.java` to ensure your packet structures match.