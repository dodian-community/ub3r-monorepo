# 04. Entity Definitions: The DAT/IDX Relationship

## Overview
Inside the archives (like `title.jag`), entities (Items, NPCs, Objects) are stored in pairs of files: a `.dat` (Data) file and an `.idx` (Index) file.

Unlike the primary cache indices (`idx0-idx4`), these internal indices do **not** store absolute byte offsets. They store **Relative Lengths**. This is a common point of failure for new cache tools.

---

## 1. The Index File Specification (`.idx`)
The internal index file defines the sequence and size of every record in the corresponding data file.

### Structure
- **Header (2 Bytes)**: `UInt16` representing the total number of entries in the file.
- **Body**: A contiguous array of `UInt16` values.

| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `total_entries` | Total entities (e.g., total items). |
| `2 + (N*2)` | `UInt16` | `entry_length` | The length of Entity `N`'s data in the `.dat` file. |

---

## 2. The Data File Specification (`.dat`)
The `.dat` file is a giant blob of opcode-encoded data. 
- **The First Byte**: Always an Opcode (`0-255`).
- **Opcode 0**: Strictly reserved as the "End of Definition" marker.

---

## 3. Mathematical Offset Resolution (Verified)
To find the absolute byte offset of Entity `N` in the `.dat` file, you must iterate from the beginning of the index and sum the lengths.

**Implementation (Java/Kotlin)**:
```java
int total = stream.readUShort();
int[] offsets = new int[total];
int currentOffset = 2; // Start after the 'total' UShort header

for (int i = 0; i < total; i++) {
    offsets[i] = currentOffset;
    currentOffset += stream.readUShort(); // Add length of item 'i'
}
```

**Tool Builder Warning**: 
- **The +21 Offset**: In the Mystic client, `ItemDefinition` adds a hardcoded `21` to the count (`stream.readUShort() + 21`). Ensure your tool handles these legacy "empty slot" offsets correctly to avoid ID-shifting.
- **ID 0**: ID 0 is a valid entry. Its offset is always `2` (immediately after the header).

---

## 4. Packing Logic (How to add a New Entity)

To add a new Item (Abyssal Whip) at the end of the cache:

1.  **Extract**: Extract `obj.dat` and `obj.idx` from `title.jag`.
2.  **Increment Count**: Read the first 2 bytes of `obj.idx` (`total_entries`), increment by 1, and write it back.
3.  **Construct Payload**: Encode your new item's properties (Name, Model, etc.) using opcodes. Terminate with Opcode `0`.
4.  **Append Data**: Write your new payload to the very end of `obj.dat`.
5.  **Append Length**: Determine the `UInt16` length of your new payload. Write this length to the very end of `obj.idx`.
6.  **Verify**: Ensure the sum of all lengths in `obj.idx` plus the 2-byte header exactly matches the new size of `obj.dat`.
7.  **Repack**: Re-compress the files into the JAG archive and update the main cache.

---

## 5. Capacity Limits
Because lengths are stored as `UInt16`, a single entity's definition (all opcodes combined) **cannot exceed 65,535 bytes**. Attempting to pack a larger definition will cause the index to overflow and corrupt the entire definition sequence.
