import os
import struct
import bz2

def read_medium(data, offset):
    return (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2]

def read_string(data, offset):
    s = b""
    while data[offset] != 10 and data[offset] != 0:
        s += bytes([data[offset]])
        offset += 1
    return s.decode('utf-8', errors='ignore'), offset + 1

def hash_name(name):
    h = 0
    name = name.upper()
    for char in name:
        h = (h * 61 + ord(char) - 32) & 0xFFFFFFFF
        if h > 0x7FFFFFFF:
            h -= 0x100000000
    return h

def decompress_rs(data):
    if data[:2] == b'BZ':
        return bz2.decompress(data)
    try:
        return bz2.decompress(b'BZh1' + data)
    except:
        return data

def inspect_object(target_id, idx_data, dat_data):
    count = struct.unpack(">H", idx_data[:2])[0]
    if target_id >= count:
        print(f"ID {target_id} out of bounds (max {count})")
        return

    indices = []
    offset = 2
    for i in range(count):
        indices.append(offset)
        offset += struct.unpack(">H", idx_data[2+i*2:4+i*2])[0]

    ptr = indices[target_id]
    print(f"--- Object {target_id} ---")
    
    while True:
        if ptr >= len(dat_data): break
        opcode = dat_data[ptr]
        ptr += 1
        if opcode == 0:
            print(f"End of definition at {ptr}")
            break
        elif opcode == 1:
            len_val = dat_data[ptr]
            ptr += 1
            print(f"Op 1: Model count {len_val}")
            ptr += len_val * 3
        elif opcode == 2:
            name, ptr = read_string(dat_data, ptr)
            print(f"Op 2: Name '{name}'")
        elif opcode == 3:
            desc, ptr = read_string(dat_data, ptr)
            print(f"Op 3: Description '{desc}'")
        elif opcode == 5:
            len_val = dat_data[ptr]
            ptr += 1
            print(f"Op 5: Model count {len_val}")
            ptr += len_val * 2
        elif opcode == 14:
            val = dat_data[ptr]
            ptr += 1
            print(f"Op 14: SizeX {val}")
        elif opcode == 15:
            val = dat_data[ptr]
            ptr += 1
            print(f"Op 15: SizeY {val}")
        elif opcode == 17:
            print("Op 17: Solid=false")
        elif opcode == 18:
            print("Op 18: Impenetrable=false")
        elif opcode == 19:
            val = dat_data[ptr]
            ptr += 1
            print(f"Op 19: Interactive {val == 1}")
        elif opcode in [21, 22, 23, 27, 62, 64, 89]:
            print(f"Op {opcode}: Flag set")
        elif opcode == 24:
            val = struct.unpack(">H", dat_data[ptr:ptr+2])[0]
            ptr += 2
            print(f"Op 24: Animation {val}")
        elif opcode in [28, 29, 39, 69]:
            val = dat_data[ptr]
            ptr += 1
            print(f"Op {opcode}: Value {val}")
        elif 30 <= opcode <= 38:
            action, ptr = read_string(dat_data, ptr)
            print(f"Op {opcode}: Action '{action}'")
        elif opcode == 40 or opcode == 41:
            count_val = dat_data[ptr]
            ptr += 1
            ptr += count_val * 4
            print(f"Op {opcode}: Count {count_val}")
        elif opcode in [60, 61, 65, 66, 67, 68, 82]:
            ptr += 2
            print(f"Op {opcode}: Skip 2")
        elif opcode in [70, 71, 72]:
            ptr += 2
            print(f"Op {opcode}: Skip 2 (Signed)")
        elif opcode == 73:
            print("Op 73: ObstructsGround=true")
        elif opcode == 74:
            print("Op 74: Hollow=true")
        elif opcode == 75:
            val = dat_data[ptr]
            ptr += 1
            print(f"Op 75: SupportItems {val}")
        elif opcode == 77 or opcode == 92:
            ptr += 4
            if opcode == 92: ptr += 2
            child_count = dat_data[ptr]
            ptr += 1
            ptr += (child_count + 1) * 2
            print(f"Op {opcode}: Morphs (count {child_count+1})")
        elif opcode == 78:
            ptr += 3
            print("Op 78: Skip 3")
        elif opcode == 79:
            ptr += 5
            amt = dat_data[ptr-1]
            ptr += amt * 2
            print(f"Op 79: Skip complex (count {amt})")
        elif opcode == 249:
            amt = dat_data[ptr]
            ptr += 1
            print(f"Op 249: Params {amt}")
            for _ in range(amt):
                is_str = dat_data[ptr] == 1
                ptr += 4
                if is_str:
                    _, ptr = read_string(dat_data, ptr)
                else:
                    ptr += 4
        else:
            print(f"Unknown opcode {opcode} at {ptr}")
            break

def extract_and_inspect():
    cache_dir = "game-server/data/cache"
    dat_path = os.path.join(cache_dir, "main_file_cache.dat")
    idx_path = os.path.join(cache_dir, "main_file_cache.idx0")
    
    with open(idx_path, 'rb') as f:
        idx_data = f.read()
    with open(dat_path, 'rb') as f:
        dat_data = f.read()

    config_index_offset = 2 * 6
    file_size = read_medium(idx_data, config_index_offset)
    sector = read_medium(idx_data, config_index_offset + 3)
    
    payload = bytearray()
    while len(payload) < file_size:
        offset = sector * 520
        header = dat_data[offset:offset+8]
        next_sector = read_medium(header, 4)
        unread = min(512, file_size - len(payload))
        payload.extend(dat_data[offset+8:offset+8+unread])
        sector = next_sector

    ext_size = read_medium(payload, 0)
    comp_size = read_medium(payload, 3)
    
    if ext_size != comp_size:
        archive_data = decompress_rs(payload[6:6+comp_size])
    else:
        archive_data = payload

    count = struct.unpack(">H", archive_data[:2])[0]
    ptr = 2
    files = {}
    data_ptr = 2 + count * 10
    for i in range(count):
        name_hash = struct.unpack(">i", archive_data[ptr:ptr+4])[0]
        ext_len = read_medium(archive_data, ptr + 4)
        comp_len = read_medium(archive_data, ptr + 7)
        file_data = archive_data[data_ptr:data_ptr+comp_len]
        
        if ext_len != comp_len:
             file_data = decompress_rs(file_data)
             
        files[name_hash] = file_data
        ptr += 10
        data_ptr += comp_len

    loc_dat = files.get(hash_name("loc.dat"))
    loc_idx = files.get(hash_name("loc.idx"))
    
    if loc_dat and loc_idx:
        inspect_object(1870, loc_idx, loc_dat)
        inspect_object(356, loc_idx, loc_dat)
        inspect_object(980, loc_idx, loc_dat)
        inspect_object(1533, loc_idx, loc_dat) # The door mentioned in a previous log
    else:
        print("Failed to extract loc.dat/loc.idx")

if __name__ == "__main__":
    extract_and_inspect()
