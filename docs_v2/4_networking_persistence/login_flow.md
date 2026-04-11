# Login & Handshake Flow

## Overview
Before a player can move or see the world, the server and client must negotiate a secure connection. This process involves a two-stage handshake, RSA/ISAAC cipher setup, and asynchronous database loading.

## Step-by-Step Breakdown

### 1. Connection Acceptance (Netty Thread)
The Netty `BossGroup` accepts a new TCP connection on port `43594` and hands it to a `WorkerGroup`.
`GameChannelInitializer` sets up the initial pipeline with `LoginHandshakeHandler`.

### 2. The Handshake (Netty Thread)
1.  **Client Request**: The client sends a single byte: `14` (indicating a standard login request).
2.  **Server Response**: The server reads the `14` and writes exactly 17 bytes back to the client:
    - 8 bytes of zeros (padding).
    - 1 byte response code (`0` = Exchange keys).
    - 8 bytes of random `long` (the `serverSeed`).

### 3. Payload Collection (Netty Thread)
The client sends the "Login Block" (opcode 16 for new login, 18 for reconnect). `LoginPayloadDecoder` collects these bytes until the specified size is reached, then passes it to `LoginProcessorHandler`.

### 4. The "Pseudo-RSA" Login Block (Netty Thread)
The `LoginProcessorHandler` parses the login block.
*   **Protocol Deviation**: Standard 317 RSPS uses RSA decryption here. Ub3r uses **Pseudo-RSA**. The Mystic Client wraps the data in an RSA opcode (10) but sends it in *plaintext*. The server parses it sequentially:
    1.  `clientSessionKey` (long)
    2.  `serverSessionKey` (long)
    3.  `username` (string)
    4.  `password` (string)

### 5. Cipher Initialization (Netty Thread)
The server uses the `clientSessionKey` and `serverSessionKey` to seed two `ISAACCipher` instances. 
- One for encrypting outbound packet opcodes.
- One for decrypting inbound packet opcodes.
The Netty pipeline is reconfigured: `LoginProcessorHandler` is removed, and `GamePacketDecoder`/`GamePacketHandler` are added.

### 6. Asynchronous Loading (Database Thread)
The server does **not** query the database on the Netty thread, nor does it block the Game Thread.
`AccountPersistenceService.submitLoginLoad()` is called. A worker thread from the HikariCP pool fetches the player's account, skills, inventory, and bank from MySQL.

### 7. Game Thread Registration (Game Thread)
Once the database query returns successfully, the player is scheduled for insertion into the game world via `GameThreadIngress`.
On the next 600ms tick, the `GameLoopService` processes the ingress queue:
1.  The player is assigned a slot in `PlayerRegistry`.
2.  `PlayerInitializer` sends the first burst of outbound packets (Skills, Inventory, Map Region).
3.  The player is officially "online" and visible to others.