# Mystic Cache Documentation

This directory contains deep-dive, byte-level documentation on how the Mystic Updated Client stores, compresses, and reads game data. 

## 🏗️ Technical Specifications
The most critical documents for building a cache tool from scratch. These define the physical byte-order and mathematical logic of the file system.

*   [0. Cache Master Specification](0_master_spec.md): Byte-perfect definitions of Indices, Sectors, and the Archive container logic.
*   [0. Index 0 Archive Mapping](0_idx0_mapping.md): Verified mapping of which archives (config, media, title) live at which File IDs in `idx0`.
*   [7. Protocol Rosetta Stone (Data Types)](7_data_types.md): The definitive guide to the custom byte/short/int parsing methods (`readLEShortA`, `readSmart`, etc.) needed to read any of these files.

## 📦 Data Definitions (Opcodes & Packing)
These documents explain how the client reads the `idx` and `dat` files to construct entity definitions in memory. They detail every single opcode, the data type it reads, and any hardcoded overrides present in the client source code.

*   [1. Item Definitions (`obj.dat`)](1_item_definitions.md)
*   [2. Object Definitions (`loc.dat`)](2_object_definitions.md)
*   [3. NPC Definitions (`npc.dat`)](3_npc_definitions.md)

## 🗺️ World Data
*   [4. Map & Landscape Loading](4_map_loading.md): Explains how the client unpacks the bit-shifted map region data to spawn thousands of trees, walls, and bridges on the correct height planes.
*   [13. Terrain & Floors (`flo.dat`)](13_map_terrain.md): Explains the underlay/overlay system and the custom HSL blending math.

## ⚙️ Game State & UI
*   [10. Interfaces & Widgets (`interfaces.jag`)](10_interfaces.md): Explains the parent/child hierarchy, CS1 scripts, and the hardcoded Java interfaces that override the cache.
*   [14. Variable Bits (`varbit.dat`)](14_variables.md): Explains how bitmasks are used to track player state and transform Objects/NPCs dynamically.

## 🗄️ Low-Level File Systems
These documents explain the core compression and fragmentation algorithms that the RuneScape client uses to manage the 50MB+ cache without running out of memory.

*   [5. The FileArchive Format (`.jag`)](5_archive_format.md): Explains the BZip2 compression, the global header, and the custom string hashing algorithm used to find files inside an archive.
*   [6. The FileStore Format (`main_file_cache.dat/idx`)](6_file_store.md): A detailed breakdown of the 520-byte Linked-List Sector allocation system. This is the most critical document for building a tool that writes new 3D models or animations into the cache safely.
*   [7. Protocol Rosetta Stone (Data Types)](7_data_types.md): The definitive guide to the custom byte/short/int parsing methods (`readLEShortA`, `readSmart`, etc.) needed to read any of these files.
*   [8. 3D Models (`main_file_cache.idx1`)](8_3d_models.md): A detailed breakdown of the highly compressed RS2 model format, including separated coordinate blocks, bit flags, and triangle fan structures.
*   [9. 2D Sprites (`index.dat` & `[name].dat`)](9_sprites.md): Explains the 255-color palette system, the magic pink transparency hack, and the row/column-major pixel arrays.
*   [11. Animations & Frames (`seq.dat` & `idx2`)](11_animations.md): Explains the sequence metadata, bit-packed Frame IDs, the `FrameBase` skeleton structure, and the custom `readShort2` math required for vertex manipulation.
*   [12. Graphics & SpotAnims (`spotanim.dat`)](12_graphics.md): Explains how 3D models and animations are combined to create visual effects like spells and projectiles.