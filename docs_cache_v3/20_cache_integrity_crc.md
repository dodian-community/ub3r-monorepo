# 20. Cache Integrity: CRC & Versioning

## Overview
To prevent the client from loading corrupted data or to detect when a player needs a cache update, RuneScape uses a simple but effective **CRC32 Checksum** system.

This data is stored in the `versionlist.jag` archive (Store 0, File 5).

---

## 1. The CRC Registry
The `versionlist.jag` archive contains four critical files:
1.  **`model_crc`**: Checksums for all files in Store 1 (`idx1`).
2.  **`anim_crc`**: Checksums for all files in Store 2 (`idx2`).
3.  **`midi_crc`**: Checksums for all files in Store 3 (`idx3`).
4.  **`map_crc`**: Checksums for all files in Store 4 (`idx4`).

---

## 2. Technical Format
Each CRC file is a raw, uncompressed array of **32-bit Integers**.
- **Entry Size**: 4 bytes.
- **Entry `i`**: The CRC32 checksum of File ID `i` in the corresponding store.

**Total Entries**: The byte-length of `model_crc` divided by 4 must exactly match the number of entries in `idx1`.

---

## 3. Verification Protocol (Verified)
The client follows this logic when loading any resource:
1.  Extract the raw bytes from the `.dat` file using the sector chain.
2.  Calculate the **CRC32** of the entire byte array (including headers if applicable).
3.  Lookup the expected checksum in the `crcs[store][id]` table loaded from `versionlist.jag`.
4.  **Mismatch**: If the checksums differ, the client identifies the file as "outdated" or "corrupt." In a web-enabled client, this triggers an automatic re-download from the update server.

---

## 4. Building a Cache Tool (CRC Safety)
If you build a tool to pack new models or maps:
1.  **Calculate New CRC**: After writing the new bytes to the `.dat` file, calculate the CRC32 of those bytes.
2.  **Update `versionlist.jag`**:
    - Extract `versionlist.jag` from `idx0`.
    - Find the relevant CRC file (e.g., `model_crc`).
    - Overwrite (or append) the 4-byte CRC at the index matching your new File ID.
    - Repack the archive and write back to `idx0`.
3.  **Skip at your Peril**: If you update a model but forget to update the `model_crc` entry, the client may refuse to load the model or throw an "Error loading cache" message.
