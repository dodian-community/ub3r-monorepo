# Cache Master Specification (Byte-Perfect)

This document provides the exact technical specification for the Mystic Client cache system. It is designed to be the only resource needed to build a complete cache editing and packing tool.

---

## 1. File Structure Overview
The cache consists of a single data file (`main_file_cache.dat`) and multiple index files (`main_file_cache.idx0` to `main_file_cache.idx4`).

### Store Index Mapping
| Index File | Data Category | Typical Content |
| :--- | :--- | :--- |
| **`idx0`** | Archives | `config.jag`, `title.jag`, `media.jag` |
| **`idx1`** | 3D Models | Character parts, NPC bodies, World objects. |
| **`idx2`** | Animations | Skeletal movement data. |
| **`idx3`** | Music | MIDI files. |
| **`idx4`** | Maps | Terrain (`m`) and Landscape (`l`) files. |

---

## 2. Index File Format (`.idx`)
Each index file is a sequential array of 6-byte entries.
- **Entry Size**: 6 bytes.
- **Entry `i` Offset**: `i * 6`.

| Bytes | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| **0-2** | `UInt24` | `file_size` | Total size of the file in bytes (decompressed). |
| **3-5** | `UInt24` | `first_sector`| The ID of the first 520-byte sector in the `.dat` file. |

---

## 3. Data File Format (`.dat`)
The data file is divided into 520-byte **Sectors**.

### Sector Structure
| Bytes | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| **0-1** | `UInt16` | `file_id` | Should match the File ID requested from the index. |
| **2-3** | `UInt16` | `part_id` | The sequential chunk index (starts at 0). |
| **4-6** | `UInt24` | `next_sector`| The ID of the next sector in the chain (0 if end of file). |
| **7** | `UInt8` | `cache_id` | Should match the Store Index (e.g., 1 for Models). |
| **8-519**| `Byte[512]`| `payload` | The actual file data. |

---

## 4. Archive Format (`.jag`)
Archives found in `idx0` are containers for multiple sub-files.

### Global Header
- **0-2**: `UInt24` (Uncompressed Size)
- **3-5**: `UInt24` (Compressed Size)

**Compression Logic**:
If `Uncompressed != Compressed`, the data from byte 6 onwards is compressed as a whole using **BZip2** (with the 'BZh9' header often stripped).

### Directory Table
- **0-1**: `UInt16` (Number of Entries `N`)
- **2 to (2 + N*10)**: Directory Entries. Each entry is 10 bytes:
    - **0-3**: `Int32` (Filename Hash)
    - **4-6**: `UInt24` (Decompressed File Size)
    - **7-9**: `UInt24` (Compressed File Size)

### Hashing Algorithm (Filename to ID)
```python
def get_hash(filename):
    hash_val = 0
    filename = filename.upper()
    for char in filename:
        hash_val = (hash_val * 61 + ord(char)) - 32
    return hash_val
```

---

## 5. Implementation Notes for Tool Builders

### BZip2 Compatibility
RuneScape's BZip2 implementation often strips the first 4 bytes (`BZh9`) from the compressed stream to save space. When decompressing, you must prepend these bytes if they are missing. When packing, you must strip them before writing the `Compressed Size` to the header.

### 24-Bit Integers (`UInt24`)
Since standard programming languages do not have a 3-byte primitive, you must manually pack/unpack them:
- **Write**: `[ (val >> 16), (val >> 8), val ]`
- **Read**: `(b[0] << 16) | (b[1] << 8) | b[2]`

### Space Management
When overwriting a file in the cache:
1.  Read the old size from the index.
2.  Calculate number of sectors: `(size + 511) // 512`.
3.  If the new file fits in the old sectors, overwrite them sequentially.
4.  If the new file is larger, allocate new sectors at `dataFile.length() // 520`.
