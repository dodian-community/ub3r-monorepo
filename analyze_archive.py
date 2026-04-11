import os
import bz2

def read_cache_file(path, index_id, file_id):
    idx_path = os.path.join(path, f"main_file_cache.idx{index_id}")
    dat_path = os.path.join(path, "main_file_cache.dat")
    
    with open(idx_path, "rb") as f:
        f.seek(file_id * 6)
        entry = f.read(6)
        file_size = int.from_bytes(entry[0:3], 'big')
        sector_id = int.from_bytes(entry[3:6], 'big')
    
    if file_size == 0: return None
    
    data = bytearray()
    with open(dat_path, "rb") as f:
        remaining = file_size
        part = 0
        while remaining > 0:
            f.seek(sector_id * 520)
            header = f.read(8)
            # Validation
            srv_file_id = int.from_bytes(header[0:2], 'big')
            srv_part_id = int.from_bytes(header[2:4], 'big')
            next_sector = int.from_bytes(header[4:7], 'big')
            srv_cache_id = header[7]
            
            chunk_size = min(remaining, 512)
            data.extend(f.read(chunk_size))
            
            remaining -= chunk_size
            sector_id = next_sector
            part += 1
    return data

def decompress_bz2(data):
    # RS BZip2 often has the 'BZh9' header stripped to save 4 bytes.
    # If the first 3 bytes aren't BZh, we prepend them.
    if data[0:3] != b'BZh':
        # Header is: BZh + '9' (compression level)
        data = b'BZh9' + data
    return bz2.decompress(data)

def analyze_archive(data):
    # Archive format (FileArchive.java)
    # 3 bytes: Uncompressed size
    # 3 bytes: Compressed size
    uncompressed_len = int.from_bytes(data[0:3], 'big')
    compressed_len = int.from_bytes(data[3:6], 'big')
    
    print(f"Archive: Uncompressed={uncompressed_len}, Compressed={compressed_len}")
    
    body = data[6:]
    if uncompressed_len != compressed_len:
        print("Archive is compressed as a whole. Inflating...")
        body = decompress_bz2(body)
    
    # After potential inflation, read directory
    # 2 bytes: Entries count
    entries = int.from_bytes(body[0:2], 'big')
    print(f"Entries in archive: {entries}")
    
    ptr = 2
    for i in range(entries):
        # 10 bytes per entry
        ident = int.from_bytes(body[ptr:ptr+4], 'big')
        ext_size = int.from_bytes(body[ptr+4:ptr+7], 'big')
        size = int.from_bytes(body[ptr+7:ptr+10], 'big')
        print(f"  Entry {i}: ID={ident}, ExtSize={ext_size}, CompSize={size}")
        ptr += 10

cache_path = "temp_cache_analysis"
# Index 0, File 1 is usually 'config.jag' or 'title.jag'
config_data = read_cache_file(cache_path, 0, 1)
if config_data:
    analyze_archive(config_data)
