#!/usr/bin/env python3
from __future__ import annotations

import bz2
import collections
import json
import struct
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1] / "data" / "cache"


def read_medium(data: bytes, offset: int) -> int:
    return (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2]


def read_store_file(root: Path, cache: int, file_id: int) -> bytes:
    data_path = root / "main_file_cache.dat"
    index_path = root / f"main_file_cache.idx{cache}"
    with data_path.open("rb") as data_file, index_path.open("rb") as index_file:
        index_file.seek(file_id * 6)
        index_entry = index_file.read(6)
        file_size = read_medium(index_entry, 0)
        sector = read_medium(index_entry, 3)
        payload = bytearray()
        part = 0
        while len(payload) < file_size:
            data_file.seek(sector * 520)
            header = data_file.read(8)
            current_file = (header[0] << 8) | header[1]
            current_part = (header[2] << 8) | header[3]
            next_sector = read_medium(header, 4)
            current_store = header[7]
            expected_store = cache + 1
            if current_file != file_id or current_part != part or current_store != expected_store:
                raise RuntimeError(
                    f"Corrupt cache chain for store={cache} file={file_id}: "
                    f"got file={current_file} part={current_part} store={current_store}, "
                    f"expected file={file_id} part={part} store={expected_store}"
                )
            unread = min(512, file_size - len(payload))
            payload.extend(data_file.read(unread))
            sector = next_sector
            part += 1
        return bytes(payload)


def unpack_bzip(payload: bytes) -> bytes:
    return bz2.decompress(b"BZh1" + payload)


def archive_hash(name: str) -> int:
    value = 0
    for char in name.upper():
        value = (value * 61 + ord(char)) - 32
        value &= 0xFFFFFFFF
    return value


def read_archive_file(archive_bytes: bytes, name: str) -> bytes:
    extracted_size = read_medium(archive_bytes, 0)
    compressed_size = read_medium(archive_bytes, 3)
    extracted = compressed_size != extracted_size
    if extracted:
        payload = unpack_bzip(archive_bytes[6 : 6 + compressed_size])
        start = 0
    else:
        payload = archive_bytes
        start = 6

    count = (payload[start] << 8) | payload[start + 1]
    table_offset = start + 2
    data_offset = table_offset + count * 10
    pointer = data_offset
    target = archive_hash(name)

    for index in range(count):
        offset = table_offset + index * 10
        file_hash = struct.unpack(">I", payload[offset : offset + 4])[0]
        compressed_len = read_medium(payload, offset + 7)
        file_bytes = payload[pointer : pointer + compressed_len]
        if file_hash == target:
            return file_bytes if extracted else unpack_bzip(file_bytes)
        pointer += compressed_len

    raise FileNotFoundError(name)


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def seek(self, pos: int) -> None:
        self.pos = pos

    def u8(self) -> int:
        value = self.data[self.pos]
        self.pos += 1
        return value

    def u16(self) -> int:
        value = (self.data[self.pos] << 8) | self.data[self.pos + 1]
        self.pos += 2
        return value

    def string(self) -> str:
        start = self.pos
        while self.data[self.pos] != 10:
            self.pos += 1
        value = self.data[start : self.pos].decode("latin-1", errors="replace")
        self.pos += 1
        return value

    def skip(self, count: int) -> None:
        self.pos += count


def skip_opcode_payload(reader: Reader, opcode: int) -> None:
    if opcode == 1:
        amount = reader.u8()
        reader.skip(amount * 3)
    elif opcode in (2, 3):
        reader.string()
    elif opcode == 5:
        amount = reader.u8()
        reader.skip(amount * 2)
    elif opcode in (14, 15, 19, 28, 29, 39, 69, 75, 81):
        reader.skip(1)
    elif opcode in (17, 18, 21, 22, 23, 27, 62, 64, 73, 74, 89):
        return
    elif 30 <= opcode < 39:
        reader.string()
    elif opcode in (24, 60, 61, 65, 66, 67, 68, 82):
        reader.skip(2)
    elif opcode in (40, 41):
        amount = reader.u8()
        reader.skip(amount * 4)
    elif opcode in (70, 71, 72):
        reader.skip(2)
    elif opcode in (77, 92):
        reader.skip(2)
        reader.skip(2)
        if opcode == 92:
            reader.skip(2)
        child_count = reader.u8()
        reader.skip((child_count + 1) * 2)
    elif opcode == 78:
        reader.skip(3)
    elif opcode == 79:
        reader.skip(2)
        reader.skip(2)
        reader.skip(1)
        amount = reader.u8()
        reader.skip(amount * 2)
    elif opcode == 249:
        amount = reader.u8()
        for _ in range(amount):
            is_string = reader.u8() == 1
            reader.skip(3)
            if is_string:
                reader.string()
            else:
                reader.skip(4)
    else:
        raise ValueError(f"Unsupported opcode {opcode}")


def build_inventory(root: Path) -> dict[str, object]:
    archive = read_store_file(root, cache=0, file_id=2)
    loc_dat = read_archive_file(archive, "loc.dat")
    loc_idx = read_archive_file(archive, "loc.idx")

    idx_reader = Reader(loc_idx)
    count = idx_reader.u16()
    indices = []
    offset = 2
    for _ in range(count):
        indices.append(offset)
        offset += idx_reader.u16()

    opcode_entries = collections.Counter()
    opcode_objects = collections.Counter()
    objects_with_unknown = {}

    data_reader = Reader(loc_dat)
    for object_id, start in enumerate(indices):
        seen = set()
        data_reader.seek(start)
        while True:
            opcode = data_reader.u8()
            opcode_entries[opcode] += 1
            seen.add(opcode)
            if opcode == 0:
                break
            try:
                skip_opcode_payload(data_reader, opcode)
            except ValueError:
                objects_with_unknown.setdefault(opcode, []).append(object_id)
                raise
        for opcode in seen:
            opcode_objects[opcode] += 1

    return {
        "object_count": count,
        "opcodes": [
            {
                "opcode": opcode,
                "entry_count": opcode_entries[opcode],
                "object_count": opcode_objects[opcode],
            }
            for opcode in sorted(opcode_entries)
        ],
        "unknown": objects_with_unknown,
    }


def main() -> None:
    inventory = build_inventory(ROOT)
    print(json.dumps(inventory, indent=2))


if __name__ == "__main__":
    main()

