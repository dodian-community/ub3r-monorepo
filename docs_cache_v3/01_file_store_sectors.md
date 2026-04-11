# 01. Physical Layer: File Store & Sector Management

## Overview
The Mystic cache uses the legacy RuneScape 2 (317-era) file system. It is a virtualized block-storage system designed to handle thousands of small files without OS overhead. 

The physical structure consists of a single **Data File** (`main_file_cache.dat`) and multiple **Index Files** (`main_file_cache.idx0` - `idx4`).

---

## 1. Index File Specification (`.idx`)
Each index file is a contiguous array of 6-byte entries. 
- **Lookup Formula**: To find metadata for File `ID`, seek to `ID * 6`.

| Byte Offset | Data Type | Field Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt24` (BE) | `file_size` | The exact byte length of the file's payload. |
| `3` | `UInt24` (BE) | `first_sector`| The ID of the sector in the `.dat` file where data begins. |

*Note: A `file_size` of 0 indicates the file does not exist in this store.*

---

## 2. Sector Specification (520 Bytes)
The `.dat` file is divided into blocks called **Sectors**. Every sector is exactly **520 bytes**. 
- **Structure**: 8-byte Header + 512-byte Payload.

### Sector Header
| Byte Offset | Data Type | Field Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` (BE) | `file_id` | Must match the ID requested from the index. |
| `2` | `UInt16` (BE) | `part_id` | The zero-based chunk index (0, 1, 2...). |
| `4` | `UInt24` (BE) | `next_sector`| ID of the next 520-byte sector. `0` if end of file. |
| `7` | `UInt8` | `cache_id` | Must match the Store Index (e.g., 1 for Models). |

### Sector Payload
- Bytes `8` to `519` (512 bytes) contain the raw file data.
- If the remaining data for a file is less than 512 bytes, the remainder of the sector is ignored (padding).

---

## 3. Tool Builder's Algorithm (Verified)

### Safe Read Algorithm:
1. Seek to `ID * 6` in the `.idx` file.
2. Read `size` and `current_sector`.
3. Allocate a byte array of length `size`.
4. While `bytes_read < size`:
    - Seek to `current_sector * 520` in the `.dat` file.
    - Read 8-byte header.
    - **Verify**: `header.file_id == ID` AND `header.cache_id == expected_id` AND `header.part_id == current_part`.
    - If verification fails, the cache is corrupted. Stop.
    - Read `min(512, size - bytes_read)` into the buffer.
    - Set `current_sector = header.next_sector`.
    - Increment `current_part`.

### Safe Write/Pack Algorithm:
1. Determine `new_size` and target `ID`.
2. Seek to `ID * 6` in `.idx`. If entry exists, identify existing sectors.
3. Calculate required sectors: `(new_size + 511) / 512`.
4. **Sector Allocation Strategy**:
    - If `new_sectors <= old_sectors`, you can overwrite the original sector chain to prevent fragmentation.
    - If `new_sectors > old_sectors`, or if it's a new file, append new sectors to the **end** of the `.dat` file.
    - **End of File Formula**: `new_sector_id = dataFile.length() / 520`.
5. Update the `.idx` file with `new_size` and the starting `sector_id`.
6. Write each sector, ensuring the `next_sector` pointer is updated correctly. The final sector MUST have a `next_sector` of `0`.

---

## 4. Logical Store Mapping
| Store ID | Index Extension | Content Type |
| :--- | :--- | :--- |
| **0** | `.idx0` | JAG Archives (Config, Media, Interfaces) |
| **1** | `.idx1` | 3D Models |
| **2** | `.idx2` | Animations (Frames & Skeletons) |
| **3** | `.idx3` | Music / MIDI |
| **4** | `.idx4` | Maps (Terrain & Landscape) |
