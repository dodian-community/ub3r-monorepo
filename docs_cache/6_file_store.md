# Mystic Cache: The FileStore Format (`main_file_cache.dat/idx`)

## Overview
The `FileStore` class is the lowest-level file system for the RuneScape 317 client. It manages reading and writing raw bytes to the massive `main_file_cache.dat` file and its accompanying index files (`main_file_cache.idx0` through `idx4`).

This format is designed to allow fast, random-access reading and writing of fragmented data without rewriting the entire 50MB+ data file when a single model or animation changes.

## The Store Indices
The client separates data into different "Stores" (index files) to keep IDs organized. Each store points to blocks in the same shared `.dat` file.
*   **Store 0 (`idx0`)**: Archives (`title.jag`, `config.jag`, etc.)
*   **Store 1 (`idx1`)**: 3D Models
*   **Store 2 (`idx2`)**: Animations
*   **Store 3 (`idx3`)**: Music / MIDI
*   **Store 4 (`idx4`)**: Maps & Landscapes

## How it Works: The 520-Byte Sector
The `main_file_cache.dat` file is divided into exactly **520-byte** chunks called "Sectors". 
-   **8 Bytes**: Header (Metadata)
-   **512 Bytes**: Payload (The actual file data)

When a file (like a 3D model) is larger than 512 bytes, it spans across multiple sectors. These sectors do not have to be contiguous; they act like a linked list.

### 1. The Index File (`.idx`)
When you request File ID `10` from Store `1` (Models), the client first opens `main_file_cache.idx1`.
It seeks to byte position `10 * 6` (because each index entry is exactly 6 bytes long).

The 6 bytes contain:
*   **Bytes 0-2** (`size`): The total length of the decompressed file in bytes (24-bit integer).
*   **Bytes 3-5** (`sector`): The ID of the first 520-byte sector in the `.dat` file where this file's data begins (24-bit integer).

### 2. Reading the Data File (`.dat`)
Once the starting sector is known, the client opens `main_file_cache.dat`.
It seeks to byte position `sector * 520`.

It reads the first 8 bytes (The Sector Header):
*   **Bytes 0-1** (`currentIndex`): Should match the requested File ID (e.g., 10).
*   **Bytes 2-3** (`currentPart`): The current chunk number of the file (starts at 0, increments by 1 for each subsequent sector).
*   **Bytes 4-6** (`nextSector`): The ID of the *next* 520-byte sector to jump to if the file is larger than 512 bytes. If this is the last chunk, this value is 0.
*   **Byte 7** (`currentFile`): Should match the Store Index (e.g., 1 for Models).

It then reads up to 512 bytes of the payload. If `totalRead < size`, it seeks to `nextSector * 520` and repeats the process, expecting `currentPart` to be 1.

## Repacking & Writing
When building a custom cache packing tool, you must replicate this linked-list sector allocation perfectly.

1.  **Check if Exists**: When overwriting a file (e.g., updating a Model), the client first reads the `.idx` to find the existing starting sector. It attempts to overwrite the existing sectors to prevent file bloat.
2.  **Allocating New Sectors**: If a file is new, or if an updated file requires *more* chunks than the old version, the client allocates a new sector at the very end of the `.dat` file:
    `nextSector = (dataFile.length() + 519) / 520;`
3.  **Writing**: It writes the 8-byte header followed by up to 512 bytes of data, then repeats for the next chunk. Finally, it updates the 6-byte entry in the `.idx` file with the total size and the starting sector.

## Security & Verification
The 8-byte header acts as a security check. If the client jumps to a sector and the `currentIndex` or `currentFile` do not match what it expects, it returns `null` and aborts reading. This prevents the client from crashing if the cache becomes corrupted or heavily fragmented.