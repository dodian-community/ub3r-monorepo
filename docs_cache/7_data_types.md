# Mystic Cache: Protocol Rosetta Stone (Data Types)

## Overview
To build a tool that can edit the Mystic cache (or communicate with the server), you must perfectly replicate the custom data types used in the RuneScape 317 protocol. Standard Java `DataOutputStream` or C# `BinaryWriter` classes **will not work** because Jagex heavily modified how bytes, shorts, and integers are packed to prevent simple packet sniffing.

This document details the exact bitwise operations required to read and write these custom types.

---

## 1. The "A" and "S" Mutations (Value Types)
Many properties add or subtract `128` from the standard byte value.

### ValueType A (Add 128)
- **Write `ByteA`**: `buffer[pos++] = (byte) (value + 128);`
- **Read `UByteA`**: `return buffer[pos++] - 128 & 0xff;`

### ValueType S (Subtract from 128)
- **Write `ByteS`**: `buffer[pos++] = (byte) (128 - value);`
- **Read `UByteS`**: `return 128 - buffer[pos++] & 0xff;`

### ValueType C (Negate)
- **Write `NegByte`**: `buffer[pos++] = (byte) (-value);`
- **Read `NegUByte`**: `return -buffer[pos++] & 0xff;`

---

## 2. Integer Types (Endianness)
Standard network byte order is **Big-Endian** (most significant byte first). RuneScape uses combinations of Little-Endian and custom "Middle-Endian" packing.

### Standard Short (Big-Endian)
- **Write**: `buf[0] = (val >> 8); buf[1] = val;`
- **Read**: `((buf[0] & 0xff) << 8) + (buf[1] & 0xff)`

### Little-Endian Short (`LEShort`)
- **Write**: `buf[0] = val; buf[1] = (val >> 8);`
- **Read**: `((buf[1] & 0xff) << 8) + (buf[0] & 0xff)`

### Standard Int (Big-Endian)
- **Write**: `buf[0] = (val >> 24); buf[1] = (val >> 16); buf[2] = (val >> 8); buf[3] = val;`
- **Read**: `((buf[0] & 0xff) << 24) + ((buf[1] & 0xff) << 16) + ((buf[2] & 0xff) << 8) + (buf[3] & 0xff)`

### Little-Endian Int (`LEInt`)
- **Write**: `buf[0] = val; buf[1] = (val >> 8); buf[2] = (val >> 16); buf[3] = (val >> 24);`
- **Read**: `((buf[3] & 0xff) << 24) + ((buf[2] & 0xff) << 16) + ((buf[1] & 0xff) << 8) + (buf[0] & 0xff)`

### Middle-Endian Int (V1 / `MEInt`)
- **Read**: `((buf[2] & 0xff) << 24) + ((buf[3] & 0xff) << 16) + ((buf[0] & 0xff) << 8) + (buf[1] & 0xff)`

### Middle-Endian Int (V2 / `IMEInt`)
- **Read**: `((buf[1] & 0xff) << 24) + ((buf[0] & 0xff) << 16) + ((buf[3] & 0xff) << 8) + (buf[2] & 0xff)`

---

## 3. Combinations (Type + Endianness)
RuneScape frequently combines a Byte Mutation with Little-Endian packing.

### `LEShortA` (Little-Endian, Type A)
- **Write**: `buf[0] = (val + 128); buf[1] = (val >> 8);`
- **Read**: `((buf[1] & 0xff) << 8) + (buf[0] - 128 & 0xff)`

### `ShortA` (Big-Endian, Type A)
- **Write**: `buf[0] = (val >> 8); buf[1] = (val + 128);`
- **Read**: `((buf[0] & 0xff) << 8) + (buf[1] - 128 & 0xff)`

---

## 4. Specialized RuneScape Types

### The `Smart` Integer
A "Smart" integer uses 1 byte if the value is small, but dynamically expands to 2 bytes if the value is large. This saves massive amounts of space in the cache (especially in Landscape files).
- **Condition**: Read the first byte. If the value is `< 128`, the total value is just that byte. If it's `>= 128`, read a standard Short and subtract `32768`.
- **Implementation**:
  ```java
  public int readSmart() {
      int value = payload[currentPosition] & 0xff;
      if (value < 128)
          return readUnsignedByte() - 64; // Signed smart
      else
          return readUShort() - 49152;
  }
  
  public int readUSmart() { // Unsigned smart
      int value = payload[currentPosition] & 0xff;
      if (value < 128)
          return readUnsignedByte();
      else
          return readUShort() - 32768;
  }
  ```

### The `TriByte` (24-bit Integer)
Used heavily in the `.jag` Archive headers for file sizes.
- **Write**: `buf[0] = (val >> 16); buf[1] = (val >> 8); buf[2] = val;`
- **Read**: `((buf[0] & 0xff) << 16) + ((buf[1] & 0xff) << 8) + (buf[2] & 0xff)`

### Strings
RuneScape strings do **not** use standard null-terminators (`0x00`) in all places, nor do they use length-prefixes (like standard Java `writeUTF`).
- **Terminator**: Most strings in the cache and protocol are terminated by the byte value `10` (`\n`).
- **Implementation**:
  ```java
  public String readString() {
      int index = currentPosition;
      while (payload[currentPosition++] != 10);
      return new String(payload, index, currentPosition - index - 1);
  }
  ```

---

## 5. Bit Access (Entity Updating & Map Parsing)
The byte stream can be switched into "Bit Access" mode, allowing the reading and writing of non-byte-aligned data (e.g., writing exactly 3 bits, then 11 bits).

- **Bit Masks**: To read bits, the client uses a pre-calculated array of powers of 2 minus 1 (`0, 1, 3, 7, 15, 31, 63, 127, 255...`).
- **State Change**:
    - `initBitAccess()` sets `bitPosition = currentPosition * 8;`
    - `disableBitAccess()` sets `currentPosition = (bitPosition + 7) / 8;` (Rounds up to the nearest byte boundary).
- **Rule**: You cannot read/write standard bytes while in Bit Access mode. You must disable it first, which forces the stream to skip to the next whole byte if it ended mid-byte.