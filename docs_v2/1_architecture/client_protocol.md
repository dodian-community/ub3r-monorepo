# Client Protocol & Compatibility

## Overview
The Ub3r server is specifically engineered to operate with the **Mystic Updated Client** (`mystic-updatedclient`). During the server's lifecycle, several modifications were made to the standard 317 RSPS protocol to support client-side features and obfuscation.

## 1. 14-Bit NPC Indexing
The most significant departure from the standard 317 protocol is the size of the NPC index in the `NpcUpdating` packet.

*   **Standard 317**: Allows a maximum of 4,095 (12-bit) or 8,191 (13-bit) concurrent NPCs.
*   **Mystic Client**: Modified in `Client.updateNPCMovement` to read 14 bits for the NPC index:
    ```java
    // mystic-updatedclient/src/com/runescape/Client.java:3810
    int k = stream.readBits(14); 
    ```
*   **Server Implementation**: `game-server/src/main/java/net/dodian/uber/game/model/entity/npc/NpcUpdating.java` writes `buf.putBits(14, npc.getSlot())` to perfectly match the client.

*Note: The NPC Definition ID (the type of NPC) is also configured to 14 bits via `Configuration.npcBits = 14;`.*

## 2. Pseudo-RSA (Plaintext Login Block)
To prevent packet sniffing, standard RSPS clients use asymmetric RSA encryption for the login block containing the username, password, and session keys.

*   **Mystic Client**: In `Configuration.java`, `ENABLE_RSA` is set to `false`. However, the client *still structures the packet as if it were RSA encrypted*. It prepends the size and sends the byte array, but skips the `BigInteger.modPow` mathematical operation.
*   **Server Implementation**: `LoginProcessorHandler.java` reads the RSA length and parses the block exactly as the client sends it: in plaintext. If true RSA were enabled on the client, the server would need the corresponding private key added to the `LoginProcessorHandler`.

## 3. The 17-Byte Handshake
When a client connects on port 43594, it sends a single byte (`14`) indicating a game login request. The server must respond before the client will send the username and password.

The Mystic client expects exactly a 17-byte response:
*   **8 bytes**: Zeros (Ignored by the client).
*   **1 byte**: Response Code (`0` = Exchange keys).
*   **8 bytes**: Server Session Key (`serverSeed`), a randomized `long` used to seed the ISAAC cipher.

## 4. Custom Data Types (Obfuscation)
The client and server utilize "Custom" byte and short variations intended to obfuscate the protocol from packet sniffers. When modifying or adding packets, these types must align perfectly:

| Server Type | Client Read Method | Mathematical Operation |
| :--- | :--- | :--- |
| `ValueType.ADD` | `readUByteA()`, `readUShortA()` | `Value + 128` |
| `ValueType.SUBTRACT` | `readUByteS()`, `readByteS()` | `128 - Value` |
| `ValueType.NEGATE` | `readNegUByte()`, `readNegByte()` | `-Value` |
| `ByteOrder.LITTLE` | `readLEShort()`, `readLEInt()` | Endianness swapped |
| `ByteOrder.MIDDLE` | `readMEInt()`, `readIMEInt()` | Custom V1/V2 byte order |

## 5. Client Coordinate System (Chunk Offsets)
When sending packets that relate to world positions (like `SendProjectile` or `SendCreateObject`), the server rarely sends absolute absolute X/Y coordinates. Instead, it sends the position *relative* to the player's current loaded chunk (an 8x8 area). 

The server calculates this as:
`int localX = absoluteX - (8 * player.getMapRegionX())`

If the client receives a local coordinate that is out of bounds (e.g., `< 0` or `>= 104`), it will silently drop the object or projectile.