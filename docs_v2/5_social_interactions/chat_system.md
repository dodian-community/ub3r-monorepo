# Chat & Private Messaging

## Overview
The chat system handles public chat, clan chat (if implemented), and private messaging. It relies on the `GameEventBus` and specialized Netty packet listeners.

## Public Chat
When a player types a message, it is sent to the server via opcode `4`.
1.  **Listener**: `ChatListener.java` decodes the packet. It extracts the text color, effects (like wave or scroll), and the actual text bytes.
2.  **Unpacking**: The 317 protocol sends chat text packed in a specialized format to save bytes. `Utils.textUnpack()` decodes this into a standard string.
3.  **Event**: The listener posts a `ChatEvent` to the `GameEventBus`.
4.  **Filters**: If a player is muted, an `EventFilter` on the `GameEventBus` intercepts the event and cancels it.
5.  **State Update**: If the event passes, `player.updateFlags.setRequired(UpdateFlag.CHAT, true)` is called, and the text is stored in the player object.
6.  **Broadcast**: During the Entity Updating phase, any player whose Viewport contains this player will see the chat text overhead and in their chatbox.

## Private Messaging (Friends & Ignore)
Private messaging involves routing a message directly from one client to another, regardless of where they are in the world.

### The Friends List
-   When a player logs in, the server sends the online status of everyone on their friends list.
-   **Opcode 188 (Add Friend)**: Validates the name and adds it to the list. If the added friend is online, their status is sent back.
-   **Opcode 215 (Remove Friend)**: Removes the name from the list.

### Sending a Message
1.  **Listener**: `SendPrivateMessageListener` (opcode 126) receives the target username and the packed message.
2.  **Routing**: `PacketChatService.handlePrivateMessage()` looks up the target player in the `PlayerRegistry`.
3.  **Validation**: It checks:
    - Is the sender muted?
    - Is the target offline?
    - Has the target added the sender to their Ignore List?
4.  **Delivery**: If valid, the server creates a `ReceivePrivateMessage` outgoing packet and sends it directly to the target player's Netty channel.

## Audit Logging
All public chat and private messages are logged asynchronously to the database or a log file (`logs/console-audit.log`) to assist moderators in enforcing rules against botting, spamming, and harassment.