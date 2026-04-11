# Dialogue System (DSL)

## Overview
Old RSPS architectures often relied on massive, hard-to-read text files or gigantic `switch` statements (`case 100:` -> "Hello", `case 101:` -> "How are you?") for NPC dialogues. 

Ub3r uses a stateful, nested **Kotlin DSL** (Domain Specific Language) that makes writing complex, branching conversations incredibly easy and readable.

## The `dialogue` Builder
You can start a dialogue anywhere (e.g., inside an `NpcModule` or an `ItemContent` plugin) by calling the `player.dialogue {}` extension function.

### Example: Branching Conversation
```kotlin
player.dialogue {
    // Step 1: NPC speaks
    npc(npcId = 553, "Welcome to my rune shop!", "Are you looking to buy?") {
        
        // Step 2: Present options to the player
        options("Select an Option",
            "Yes, please." to {
                // Step 3a: Player chose Yes. Player speaks, then action occurs.
                player("Yes, please.") {
                    Server.shopManager.openShop(client, 2) // Open Rune Shop
                }
            },
            "No, I'm just browsing." to {
                // Step 3b: Player chose No. NPC responds.
                player("No, I'm just browsing.") {
                    npc(553, "Well, let me know if you change your mind.")
                    // Dialogue naturally ends here
                }
            }
        )
    }
}
```

## How it Works Internally

1.  **State Capture**: When `player.dialogue {}` is called, the DSL builder executes and captures the entire conversation tree into memory (an instance of `DialogueFactory`).
2.  **Display**: The `DialogueDisplayService` sends the packets to open the interface (e.g., the 2-line chatbox) and sends the text and the "talking" animation for the NPC's head.
3.  **Waiting**: The server stops executing that specific dialogue branch and waits for the player.
4.  **Resumption**: When the player clicks "Click here to continue" (opcode 40), `DialogueService` looks at the active `DialogueFactory`, finds the *next* node in the tree, and executes it.
5.  **Options**: If the next node is an `options` block, it sends a multi-choice interface. When the player clicks an option (opcode 185), `DialogueOptionService` looks up which lambda closure corresponds to that button and executes it.

## Key Features
- **Expressions**: You can change the "head animation" of the NPC or player to convey emotion (e.g., `DialogueEmote.SAD`, `DialogueEmote.LAUGH`).
- **Conditionals**: Because it's Kotlin, you can put standard `if/else` logic right inside the dialogue block (e.g., if the player has an item, show a different response).
- **Type Safety**: No more guessing if "line 3" goes on Interface ID 2451 or 2452. The `DialogueDisplayService` automatically selects the correct interface ID based on how many lines of text you provide.