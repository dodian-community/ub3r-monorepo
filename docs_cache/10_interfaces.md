# Mystic Cache: Interfaces / Widgets (`interfaces.jag`)

## Overview
The `interfaces.jag` archive contains the layouts and components for every user interface in the game (Bank, Inventory, Chatbox, Equipment screen, etc.). 

Inside this archive, the primary file is simply named `data`. This file contains a sequential list of "Widgets" (previously known as `RSInterface` in early 317 refactors). 

The `Widget.java` class is responsible for unpacking this data and constructing the interface tree in memory.

## The Widget Tree Structure
Interfaces in RuneScape are hierarchical.
- **Parent Interface**: A container (like the Bank screen background). It has no visual graphics itself other than its children.
- **Children**: The buttons, text labels, sprites, and item containers that live inside the Parent.

When a Parent interface is drawn on the screen, the client recursively draws all of its children at specific X/Y offsets relative to the Parent.

## Unpacking `data` (`Widget.load`)

The `load()` method reads the `data` buffer sequentially. It loops until the buffer is fully consumed.

### 1. The ID and Parent
Every widget starts with its ID.
```java
int interfaceId = buffer.readUShort();
```
If `interfaceId == 65535` (`0xFFFF`), it means the following widgets belong to a *new* Parent interface. The stream then reads the `defaultParentId` and the *actual* `interfaceId`.

### 2. Core Metadata
Next, the core properties common to all widgets are read:
- `type`: `UnsignedByte` (0 = Container, 4 = Text, 5 = Sprite, etc.)
- `atActionType`: `UnsignedByte` (1 = Ok, 2 = Usable, 3 = Close, etc.)
- `contentType`: `UShort`
- `width`: `UShort`
- `height`: `UShort`
- `opacity`: `UnsignedByte` (0 = fully opaque, 255 = fully transparent)
- `hoverType`: `UnsignedByte` (If != 0, reads another byte to get the ID of the widget to show when hovering over this one).

### 3. Client Scripts (CS1)
Early interfaces supported basic logical operators (CS1) to determine things like "If Player Level < 40, draw this text red".
- Reads number of `operators` (`UnsignedByte`).
- If `> 0`, loops to read `valueCompareType` (`UnsignedByte`) and `requiredValues` (`UShort`).
- Reads number of `scripts` (`UnsignedByte`).
- If `> 0`, loops to read a 2D array of `valueIndexArray` (`UShort` arrays).

### 4. Type-Specific Parsing
The stream then branches based on the `type` byte read earlier.

#### Type 0: Container (Parent)
- `scrollMax`: `UShort` (How far the interface can scroll).
- `invisible`: `UnsignedByte` (1 = hidden by default).
- `length`: `UShort` (The number of children).
- Loops `length` times to read the `children` array (`UShort`), and the `childX` and `childY` offset arrays (`Short`).

#### Type 2: Inventory (Item Container)
Used for the bank, inventory, and trade screens.
- Reads booleans (`UnsignedByte == 1`) for `allowSwapItems`, `hasActions`, `usableItems`, `replaceItems`.
- `spritePaddingX` and `spritePaddingY` (`UnsignedByte`).
- Loops 20 times to read up to 20 background sprites (e.g., the empty slot background in the equipment screen).
- Loops 5 times to read the `actions` array (`String`) (e.g., "Wield", "Eat").

#### Type 3: Rectangle
- `filled`: `UnsignedByte` (1 = solid, 0 = outline).
- Reads `textColor`, `secondaryColor`, `defaultHoverColor`, `secondaryHoverColor` (`Int` / 4 bytes).

#### Type 4: Text
- `centerText`: `UnsignedByte == 1`.
- `fontId`: `UnsignedByte` (Index into the `textDrawingAreas` array).
- `textShadow`: `UnsignedByte == 1`.
- `defaultText` and `secondaryText`: `String` (Text terminated by byte `10`). *Note: The client automatically replaces the string "RuneScape" with the configured `CLIENT_NAME`.*
- Reads color `Int`s similar to Type 3.

#### Type 5: Sprite
- `defaultSprite`: `String` (A comma-separated string like `MAGIC_ICONS,5`). The client parses this and fetches the sprite from the `spriteCache` (which loads from `media.jag`).
- `secondarySprite`: `String`.

#### Type 6: 3D Model
Used for drawing items or NPCs on an interface (like the chathead or the spinning item on the Smithing screen).
- Reads `defaultMedia` (`UShort`), `secondaryMedia` (`UShort`).
- Reads `defaultAnimationId` and `secondaryAnimationId`.
- Reads `modelZoom`, `modelRotation1`, and `modelRotation2` (`UShort`).

## Hardcoded Interfaces
A massive portion of the `Widget.java` class is dedicated to **Hardcoded Interfaces**. 
Because packing the `data` file was notoriously difficult in the early 2000s, developers opted to manually construct widgets in Java code after the cache loaded.

At the bottom of `Widget.load()`, you will see calls like:
- `clanChatTab(textDrawingAreas)`
- `bankInterface(textDrawingAreas)`
- `fKeys(textDrawingAreas)`

These methods programmatically create new `Widget` objects, assign them IDs (often > 20000 to avoid conflicts with cache IDs), manually set their X/Y child offsets, and load custom sprites.

**Warning for Tool Builders**: If you build an interface editor, you will *not* see the Bank interface or the F-Keys tab if you only read the `data` file. These only exist in the Java source code of the client. To properly edit them, you must either edit the Java code directly, or rewrite them into your custom cache packer and remove the hardcoded methods from the client.
