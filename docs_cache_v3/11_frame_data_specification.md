# 11. Low-Level Animation: Frame Data Specification

## Overview
Low-level animation data is stored in Store 2 (`idx2`). This data defines the exact vertex transformations (movement, rotation, scaling) for 3D models. It consists of two layers:
1.  **Skeletons (`FrameBase`)**: Definitions of bones and transformation types.
2.  **Frames (`Frame`)**: The actual delta values for a specific movement at a specific point in time.

---

## 1. Frame IDs (The 32-bit Pointer)
In the high-level `seq.dat` file, frames are referenced as 32-bit integers.
- **Top 16 bits**: The File ID in `idx2`.
- **Bottom 16 bits**: The local index of the frame within that file.

```python
file_id = global_id >> 16
frame_index = global_id & 0xFFFF
```

---

## 2. Skeleton Format (`FrameBase`)
Every Frame file in `idx2` begins with a **Skeleton** definition.

| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `bone_count` | Total number of bones/groups in the skeleton. |
| `2` | `UInt16[N]` | `types` | Transformation type for each bone. |
| `2 + N*2` | `UInt16[N]` | `group_lens` | How many model-bones belong to each group. |
| `2 + N*4` | `UInt16[N][M]`| `bone_indices`| The actual model-bone indices for each group. |

**Transformation Types**:
- `0`: No-op / Base Position.
- `1`: Translation (X, Y, Z movement).
- `2`: Rotation (Angle change).
- `3`: Scaling (Size change).

---

## 3. Frame Format (`Frame`)
Immediately following the `FrameBase` in the same file are the individual frames.

| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `Header` | `UInt16` | `total_frames` | Number of frames in this file. |

**For each frame**:
1.  Read `id` (`UInt16`). This is the local index.
2.  Read `move_count` (`UByte`).
3.  Loop `move_count` times:
    - Read `bone_index` (`UByte`). This maps back to the `FrameBase` skeleton.
4.  Loop `move_count` times:
    - Read `flags` (`UByte`). Bit 1 = X, Bit 2 = Y, Bit 4 = Z.
5.  Read the transformations:
    - If Bit 1: `dx = readShort2()`.
    - If Bit 2: `dy = readShort2()`.
    - If Bit 4: `dz = readShort2()`.

---

## 4. The `readShort2` Mathematical Constant (Verified)
This is the most critical logic for an animation tool. If your math is off by 1, models will explode during animation.

```java
// Logic from Buffer.java
int i = readUShort();
if (i > 32767) {
    i -= 65537; // NOT 65536!
}
return i;
```

---

## 5. Summary for Tool Builders
- **Skeleton Sharing**: Multiple frames (e.g., walk, run, stand) often share the same skeleton in a single `idx2` file.
- **Rotation Units**: Rotations are encoded as integers from `0-2048`, representing `0-360` degrees.
- **Scaling Units**: A scale value of `128` represents `1.0x` (100% size).
