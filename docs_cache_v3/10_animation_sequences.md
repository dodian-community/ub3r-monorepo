# 10. Animation Specification: Sequences & SpotAnims

## Overview
Animations in RuneScape are high-level instructions that tie low-level "Frames" (3D movements) to game ticks. There are two primary types of animation definitions:
1.  **Sequences (`seq.dat`)**: Applied to entities (Players/NPCs).
2.  **SpotAnims (`spotanim.dat`)**: Visual effects like projectiles or spells (GFX).

---

## 1. Sequences (`seq.dat`)
Stored in `config.jag`. Defines which frames play and for how long.

### Opcode Mapping (Verified)
| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `UInt16`, `UInt32[]`, `UInt8[]`| `frames` | `Count`, then `Count` 32-bit Frame IDs, then `Count` durations (ticks). |
| **2** | `UInt16` | `loop_offset`| Frame index to jump back to when looping. |
| **3** | `UByte`, `UInt8[]` | `interleave`| `Count`, then `Count` bone indices to prioritize. |
| **4** | (None) | `stretches` | If present, animation stretches to fit movement distance. |
| **5** | `UInt8` | `priority` | Priority level (1-10). |
| **6** | `UInt16` | `offhand` | Item ID to hide in the offhand during anim. |
| **7** | `UInt16` | `mainhand` | Item ID to hide in the mainhand during anim. |
| **8** | `UInt8` | `max_loops` | Maximum repetitions allowed. |
| **9** | `UInt8` | `precedence` | Determines if walking cancels the animation. |
| **10** | `UInt8` | `anim_priority`| General animation priority. |
| **11** | `UInt8` | `replay_mode` | Behavior when re-triggering the same animation. |

---

## 2. SpotAnims / Graphics (`spotanim.dat`)
Stored in `config.jag`. Defines 3D visual effects.

### Opcode Mapping (Verified)
| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `UInt16` | `model_id` | 3D Model ID from Store 1 (`idx1`). |
| **2** | `UInt16` | `seq_id` | Animation Sequence ID from `seq.dat`. |
| **4** | `UInt16` | `scale_xy` | Model width/length scale (Default 128). |
| **5** | `UInt16` | `scale_z` | Model height scale (Default 128). |
| **6** | `UInt16` | `rotation` | Initial orientation. |
| **7** | `UInt8` | `brightness` | Light modifier. |
| **8** | `UInt8` | `contrast` | Shadow modifier. |
| **40** | `UByte`, `UInt16[]`| `colors` | `Len`, then `Len` pairs of (Old Color, New Color). |

---

## 3. Tool Builder Implementation
To create a new GFX (e.g., a "Divine Blast"):
1.  **Model**: Pack the 3D geometry into `idx1`.
2.  **Frames**: Pack the movement transformations into `idx2`.
3.  **Sequence**: Create an entry in `seq.dat` linking your frames.
4.  **SpotAnim**: Create an entry in `spotanim.dat` linking your `model_id` and `seq_id`.

**Warning**: The `seq_id` must refer to a valid index in the `seq.dat` file. If you append to `seq.dat`, you must update the 2-byte header at the start of the file.
