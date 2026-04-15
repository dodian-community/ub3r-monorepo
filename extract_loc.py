import os
import struct
import bz2

def read_medium(data, offset):
    return (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2]

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

def extract():
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

    # JagArchive header
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
        with open("loc.dat", "wb") as f: f.write(loc_dat)
        with open("loc.idx", "wb") as f: f.write(loc_idx)
        print("Extracted loc.dat/idx")
    else:
        print("Failed to find files")

if __name__ == "__main__":
    extract()
