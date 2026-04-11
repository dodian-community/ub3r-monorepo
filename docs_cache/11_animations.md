# Mystic Cache: Animations & Frames

## Overview
Animations in RuneScape (like swinging a sword, walking, or a tree falling) are broken down into two distinct systems:
1.  **Sequences (`seq.dat`)**: High-level metadata. It defines *which* frames to play, in what order, and for how long.
2.  **Frames (`Frame.java` / `FrameBase.java`)**: Low-level 3D manipulation data. It defines exactly how the vertices/triangles of a 3D model move across the X, Y, and Z axes.

---

## 1. Sequences (`seq.dat`)
Loaded from `seq.dat` inside `config.jag` during `Animation.init()`.

The file contains sequential definitions for every "Animation ID" in the game.
It begins by reading `length` (a `UShort`), followed by a standard opcode loop.

### Opcode Map
| Opcode | Property | Description |
| :--- | :--- | :--- |
| **1** | Frames & Durations | The core of the sequence. Reads `frameCount` (`UShort`). Then loops to read `primaryFrames` (`Int`), and loops again to read `durations` (`UnsignedByte` representing game ticks). |
| **2** | `loopOffset` | `UShort`. When an animation loops, it doesn't always start from frame 0. This dictates which frame to loop back to. |
| **3** | `interleaveOrder` | Array of `UnsignedByte`s. Used for complex blending (like walking while attacking). Dictates which bones prioritize this animation over another. |
| **4** | `stretches` | (Boolean). If true, the animation can dynamically stretch to fit a gap (e.g., a long jump). |
| **5** | `forcedPriority` | `UnsignedByte`. 1-10. Higher priority animations override lower ones. |
| **6** | `playerOffhand` | `UShort`. If set, the player's shield/offhand is hidden during this animation. |
| **7** | `playerMainhand` | `UShort`. If set, the player's weapon is hidden (e.g., during the Zombie random event dance). |
| **8** | `maximumLoops` | `UnsignedByte`. Max times the animation can repeat. |
| **9** | `animatingPrecedence` | `UnsignedByte`. Determines if the animation is cancelled when walking. |
| **10** | `priority` | `UnsignedByte`. General priority level. |
| **11** | `replayMode` | `UnsignedByte`. Controls how the animation resets if the same animation is requested again while playing. |

---

## 2. Frames & Skeletons (`Frame.java` / `FrameBase.java`)
While `seq.dat` tells you to "Play Frame ID 5000", `Frame.java` actually loads Frame 5000 from the `.dat` file (often fetched on-demand from Store 2 / `idx2` if not pre-loaded).

### Frame IDs (The 32-bit Integer)
A Frame ID stored in `primaryFrames` (from `seq.dat`) is actually a bit-packed integer:
```java
int file = frameId >> 16;  // The ID of the file in idx2 (Store 2)
int k = frameId & 0xffff;  // The specific frame index within that file
```
Animations for a specific entity are usually grouped into a single file in the cache. 

### The `FrameBase` (Skeleton)
Before a frame can manipulate a model, it needs a "Skeleton" (`FrameBase`). The skeleton dictates the different types of transformations that can occur.
When a Frame file is loaded, it first instantiates the `FrameBase`:
1.  Reads the number of bones/transformations.
2.  Reads the `transformationType` for each bone:
    -   `0`: Base definition / Origin
    -   `1`: Translation (Moving along X/Y/Z)
    -   `2`: Rotation
    -   `3`: Scaling (128 is 100%)

### Parsing a Frame
After the `FrameBase` is loaded, it parses the actual `Frame` data:
1.  Reads the number of frames in the file.
2.  For each frame:
    - Reads the `transformationCount` (how many bones move in this frame).
    - Reads a bit-flag (`f2`).
    - If `(f2 & 0x1) != 0`, reads `transformX` (a custom `readShort2()` which offsets negative values by `-65537`).
    - If `(f2 & 0x2) != 0`, reads `transformY`.
    - If `(f2 & 0x4) != 0`, reads `transformZ`.

### Applying a Frame to a Model
When the client renders a model, it calls `empty.applyTransform(frame)`.
The rendering engine iterates through the `transformX/Y/Z` arrays. If the corresponding `FrameBase` type is `1`, it moves the vertices attached to that bone. If it's `2`, it applies trig functions (Sine/Cosine arrays) to rotate the vertices.

## Cache Editor Implications
If you are building an animation packer:
1.  You must first pack the `FrameBase` skeleton correctly.
2.  You must pack the `Frame` translations/rotations into `idx2`.
3.  You must pack the `seq.dat` sequence tying those frames together with durations.
4.  **Important**: The `transformX/Y/Z` values use a very specific read method:
    ```java
    public int readShort2() {
        currentPosition += 2;
        int i = ((payload[currentPosition - 2] & 0xff) << 8) + (payload[currentPosition - 1] & 0xff);
        if(i > 32767) i -= 65537; // Standard short would be -65536, this is a custom RS modification.
        return i;
    }
    ```