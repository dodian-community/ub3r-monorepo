# 03. Archive Management: The JAG Format

## Overview
High-level configuration data (Items, NPCs, Objects, UI) is not stored as individual files in the main cache. Instead, it is bundled into **JAG Archives** (often ending in `.jag`). These archives are stored in Store 0 (`idx0`).

A JAG archive is a mini-filesystem that supports individual file compression via BZip2 and filename discovery via name-hashing.

---

## 1. Physical Structure
A JAG file consists of a Global Header, a Directory Table, and the Payload.

### A. Global Header (6 Bytes)
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt24` | `uncompressed_length` | Total size of the archive after inflation. |
| `3` | `UInt24` | `compressed_length` | Total size of the archive on disk. |

**Verification Logic**:
- If `uncompressed_length != compressed_length`, the entire archive (starting at byte 6) is BZip2 compressed.
- If they are equal, the archive is "Extracted" (not whole-compressed), and individual files inside may or may not be compressed.

---

## 2. The Directory Table
Immediately following the header (or the inflated body) is the Directory.

| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `entries` | Number of files in the archive. |

For each entry (10 bytes each):
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt32` | `identifier` | The hashed filename (e.g., `OBJ.DAT`). |
| `4` | `UInt24` | `extracted_size` | Size of the file when fully decompressed. |
| `7` | `UInt24` | `compressed_size` | Size of the file as stored in the payload. |

---

## 3. Filename Hashing Algorithm (Verified)
The client does not store strings. It hashes them. To extract a specific file, your tool must implement this exact algorithm:

```python
def get_rs_hash(filename: str) -> int:
    hash_val = 0
    filename = filename.upper()
    for char in filename:
        # RS uses a custom multiplier 61
        hash_val = (hash_val * 61 + ord(char)) - 32
    return hash_val & 0xFFFFFFFF
```

**Common Hashes**:
- `OBJ.DAT`: `519328695`
- `OBJ.IDX`: `1919114014`
- `LOC.DAT`: `4067724704`
- `NPC.DAT`: `2365629959`

---

## 4. Packing/Unpacking Protocol (BZip2)

### The "BZh9" Header Hack
RuneScape's BZip2 implementation strips the 4-byte header (`BZh9`) from compressed blocks. 
- **Tool Requirement**: When decompressing a file from an archive, if the first 3 bytes are not `BZh`, you must prepend `BZh9` before passing the data to a standard BZip2 library.
- **Tool Requirement**: When packing, you must compress the file and then strip the first 4 bytes.

### Extraction Algorithm:
1. Read the 6-byte global header.
2. If compressed, inflate the entire body.
3. Read the `UShort` entry count.
4. Loop through the 10-byte entries.
5. Identify the target file via its `identifier` (hash).
6. Sum the `compressed_size` of all preceding files to find the starting `offset` in the payload.
7. Slice the data. If the global archive was NOT whole-compressed, inflate this specific slice.

---

## 5. Summary of Store 0 Files
| File ID | Likely Name | Contents |
| :--- | :--- | :--- |
| **1** | `title.jag` | `obj.dat`, `npc.dat`, `loc.dat`, `flo.dat`, `map_index`. |
| **2** | `config.jag` | `varbit.dat`, `seq.dat`, `spotanim.dat`. |
| **3** | `interfaces.jag` | Interface `data`, UI component sprites. |
| **4** | `media.jag` | General game icons and sprites. |
