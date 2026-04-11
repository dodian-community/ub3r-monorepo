# 02. Master Buffer Specification (Verified)

## Overview
This document serves as the absolute "Golden Truth" for reading and writing data in the Mystic Cache and its network protocol. Every bit-shift and mask here has been verified against `Buffer.java`.

---

## 1. Fundamental Primitives

### UInt24 (TriByte) - Standard (BE)
Used for file sizes and colors.
- **Read**: `(b[0] << 16) | (b[1] << 8) | b[2]`
- **Write**: `[ (val >> 16), (val >> 8), val ]`

### UShort2 (Standard Signed Short with Offset)
Found in `readShort2()`. Used for animation frame deltas.
- **Read**: 
    ```java
    int i = (b[0] << 8) + b[1];
    if (i > 32767) i -= 65537; // Verified: 65537, not 65536
    return i;
    ```
- **Write**: `val < 0 ? val + 65537 : val` (then write as UShort).

---

## 2. Dynamic Types (Smart Integers)

### USmart (Unsigned)
- **Condition**: Read first byte `B`.
- **If `B < 128`**: Return `B`.
- **If `B >= 128`**: Read next byte `B2`. Return `((B & 0xFF) << 8 | (B2 & 0xFF)) - 32768`.

### Smart (Signed)
- **Condition**: Read first byte `B`.
- **If `B < 128`**: Return `B - 64`.
- **If `B >= 128`**: Read next byte `B2`. Return `((B & 0xFF) << 8 | (B2 & 0xFF)) - 49152`.

### USmart2 (Looping Smart)
Used for IDs exceeding 32,767 (modern objects/npcs).
```python
def read_usmart2(stream):
    base = 0
    while True:
        val = stream.read_usmart()
        if val != 32767:
            return base + val
        base += 32767
```

---

## 3. String & Byte Blocks
- **Standard String**: Read bytes until `10` (`\n`).
- **New String**: Read bytes until `0` (`\0`).
- **Byte Array**: `readBytes()` reads until `10`. The length is `current - start - 1`.

---

## 4. Endianness & Byte Mutations

| Method | Order | Mutation | Formula |
| :--- | :--- | :--- | :--- |
| `readUByteA` | 1 byte | A | `val - 128 & 0xFF` |
| `readUByteS` | 1 byte | S | `128 - val & 0xFF` |
| `readNegUByte`| 1 byte | C | `-val & 0xFF` |
| `readUShortA` | BE | A | `(b[0] << 8) + (b[1] - 128 & 0xFF)` |
| `readLEUShort`| LE | None | `(b[1] << 8) + b[0]` |
| `readLEUShortA`| LE | A | `(b[1] << 8) + (b[0] - 128 & 0xFF)` |
| `readMEInt` | ME | None | `(b[2] << 24) + (b[3] << 16) + (b[0] << 8) + b[1]` |
| `readIMEInt` | IME | None | `(b[1] << 24) + (b[0] << 16) + (b[3] << 8) + b[2]` |

---

## 5. Bit Access
- **`readBits(n)`**: Extracts exactly `n` bits across byte boundaries.
- **Rule**: You **must** call `initBitAccess()` before calling `readBits`.
- **Rule**: `disableBitAccess()` rounds the current byte pointer **UP** to the next full byte. If you were at bit 10, you are now at byte 2.
