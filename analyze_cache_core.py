import os

def analyze_index(path, index_id):
    filename = f"main_file_cache.idx{index_id}"
    full_path = os.path.join(path, filename)
    if not os.path.exists(full_path):
        print(f"Index {index_id} not found.")
        return
    
    size = os.path.getsize(full_path)
    entries = size // 6
    print(f"--- Analysis of {filename} ---")
    print(f"Total size: {size} bytes")
    print(f"Total entries: {entries}")
    
    with open(full_path, "rb") as f:
        # Read first 5 entries
        for i in range(min(5, entries)):
            data = f.read(6)
            file_size = int.from_bytes(data[0:3], byteorder='big')
            first_sector = int.from_bytes(data[3:6], byteorder='big')
            print(f"Entry {i}: Size={file_size}, First Sector={first_sector}")

def analyze_dat_sector(path, sector_id):
    full_path = os.path.join(path, "main_file_cache.dat")
    with open(full_path, "rb") as f:
        f.seek(sector_id * 520)
        header = f.read(8)
        file_id = int.from_bytes(header[0:2], byteorder='big')
        part_id = int.from_bytes(header[2:4], byteorder='big')
        next_sector = int.from_bytes(header[4:7], byteorder='big')
        cache_id = header[7]
        print(f"--- Sector {sector_id} Header ---")
        print(f"FileID: {file_id}, Part: {part_id}, Next: {next_sector}, CacheID: {cache_id}")

cache_path = "temp_cache_analysis"
for i in range(5):
    analyze_index(cache_path, i)

# Analyze the first sector of the first file in idx0
analyze_dat_sector(cache_path, 1)
