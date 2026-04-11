# Mystic Cache: The FileArchive Format (`.jag`)

## Overview
Before the modern "Index/Data" (`.idx`/`.dat`) cache format took over everything in RuneScape, early engines relied heavily on "Archives" (often ending in `.jag` or `.a`). 

The `FileArchive.java` class is responsible for reading these archives. An archive is essentially a mini-filesystem wrapped in a single file, compressed using **BZip2**.

## Structure of a `.jag` Archive

An archive can be compressed in two ways:
1.  **Whole Archive Compression**: The entire file is compressed. Once unzipped, it contains raw bytes for multiple files.
2.  **Individual File Compression**: The "Header" (directory structure) is uncompressed, but the individual files inside the archive are compressed.

### 1. The Global Header
The first 6 bytes of the file dictate the compression state:
-   `decompressedLength` (3 bytes, `readTriByte()`)
-   `compressedLength` (3 bytes, `readTriByte()`)

**Logic**:
-   If `compressedLength != decompressedLength`, the **entire archive is compressed**. The client immediately uses `BZip2Decompressor` to inflate the rest of the bytes.
-   If they are equal, the archive is **not** compressed as a whole (meaning individual files inside are).

### 2. The Directory Table
After the decompression step (if applicable), the client reads the "Directory":
-   `entries` (2 bytes, `readUShort()`): The number of files inside this archive.

For each entry, it reads a 10-byte block:
-   `identifier` (4 bytes, `readInt()`): A custom hash of the file's name.
-   `extractedSize` (3 bytes, `readTriByte()`)
-   `compressedSize` (3 bytes, `readTriByte()`)

### 3. The File Data
After the directory table (which is `entries * 10` bytes long), the actual file data begins sequentially. The client calculates the starting index (`offset`) for each file during the directory read.

## Finding a File by Name
Because strings are expensive to store and search, RuneScape caches don't store file names (like `"obj.dat"`). Instead, they store a **Hash** of the filename.

When you call `archive.readFile("obj.dat")`:
1.  The client converts `"obj.dat"` to uppercase `"OBJ.DAT"`.
2.  It runs a custom hashing algorithm:
    ```java
    int hash = 0;
    for (int index = 0; index < name.length(); index++) {
        hash = (hash * 61 + name.charAt(index)) - 32;
    }
    ```
3.  It searches the `identifiers` array for this integer.
4.  If found, it pulls the bytes using the calculated `offset` and `compressedSize`.
5.  If `extracted` was false (individual compression), it runs `BZip2Decompressor` on just that chunk of bytes.

## Repacking Implications
If you intend to build a tool to pack new `.jag` archives:
1.  You must implement the exact hashing algorithm above for your filenames.
2.  You must implement the standard `TriByte` (24-bit integer) read/write methods.
3.  You must use a compatible BZip2 compression library (stripping the "BZh1" header bytes, which RS strips to save 4 bytes per file).