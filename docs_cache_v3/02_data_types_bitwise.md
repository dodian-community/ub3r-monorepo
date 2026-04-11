# 02. Data Types & Bitwise Rosetta Stone

## Overview
The Mystic client uses a heavily modified byte-stream implementation to obfuscate data and optimize storage. Standard libraries cannot read this data. You must implement the exact mathematical logic below in your tool.

---

## 1. Value Type Mutations
Standard bytes are often transformed by adding or subtracting `128` or negating the value.

| Type | Write Operation | Read Operation (Java) | Bitwise Logic |
| :--- | :--- | :--- | :--- |
| **Standard** | `b` | `b & 0xFF` | (None) |
| **Type A** | `b + 128` | `(b - 128) & 0xFF` | Addition of 128 |
| **Type S** | `128 - b` | `(128 - b) & 0xFF` | Subtraction from 128 |
| **Type C** | `-b` | `(-b) & 0xFF` | Negation (2's complement) |

---

## 2. Multi-Byte Endianness (Verified)
The client mixes Big-Endian (BE), Little-Endian (LE), and custom "Middle-Endian" (ME) ordering.

### Short (16-bit)
- **Standard (BE)**: `[b0, b1]` -> `(b0 << 8) + b1`
- **Little-Endian (LE)**: `[b1, b0]` -> `(b0 << 8) + b1`
- **LEShortA**: `[b1+128, b0]` -> `(b0 << 8) + (b1 - 128 & 0xFF)`

### TriByte (24-bit)
- **Standard (BE)**: `[b0, b1, b2]` -> `(b0 << 16) + (b1 << 8) + b2`

### Integer (32-bit)
- **Standard (BE)**: `[b0, b1, b2, b3]`
- **Middle-Endian (ME)**: `[b2, b3, b0, b1]` -> `(b2 << 24) + (b3 << 16) + (b0 << 8) + b1`
- **Inverse Middle-Endian (IME)**: `[b1, b0, b3, b2]` -> `(b1 << 24) + (b0 << 16) + (b3 << 8) + b2`

---

## 3. Dynamic "Smart" Integers (Verified)
Smarts are used for space optimization. They are either 1 or 2 bytes depending on the value.

### Unsigned Smart (`readUSmart`)
Used for IDs and counts where values are positive.
- **If first byte < 128**: Value is `byte & 0xFF`.
- **Else**: Value is `(ushort & 0xFFFF) - 32768`.

### Signed Smart (`readSmart`)
Used for coordinate offsets (can be negative).
- **If first byte < 128**: Value is `(byte & 0xFF) - 64`.
- **Else**: Value is `(ushort & 0xFFFF) - 49152`.

---

## 4. The `readShort2` Custom Math (Verified)
Found in `Buffer.java`, this is a specialized version of a signed short used in the animation system.
```java
int i = ((payload[pos - 2] & 0xff) << 8) + (payload[pos - 1] & 0xff);
if(i > 32767) i -= 65537; 
return i;
```
**CRITICAL**: Standard Java `short` (signed) wraps at `65536`. This client uses `65537`. This 1-byte difference will break every animation if not implemented correctly in your packer.

---

## 5. String & Byte Array Termination
- **Strings**: Terminated by byte `10` (`\n`). 
- **NewStrings**: Terminated by byte `0` (`\0`).
- **Logic**: Read until the terminator is hit, then convert the preceding bytes to a UTF-8 or CP1252 string.

---

## 6. Bit-Level Access (Verified)
Used for Entity Updating and some map data. 
- **Initialization**: `bitPosition = bytePosition * 8`.
- **Masking**: Use a bit-mask array where `BIT_MASKS[n] = (1 << n) - 1`.
- **Cross-Byte Read**:
    ```python
    # Pseudo-code for reading 'n' bits
    byte_offset = bit_pos >> 3
    bit_offset = 8 - (bit_pos & 7)
    # ... loop and shift bits from payload[byte_offset] ...
    ```
- **Disabling**: `bytePosition = (bitPosition + 7) / 8` (Rounds up to the next full byte).
