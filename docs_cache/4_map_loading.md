# Mystic Cache: Landscape / Map Loading

## Overview
The world map in RuneScape is divided into "Regions" (64x64 tiles). The client loads the visual data for these regions from the cache. There are typically two files per region:
1.  **Map Data**: Defines the terrain (height, color, textures, collision).
2.  **Landscape Data**: Defines the placement of static world objects (trees, walls, bank booths).

This document details the **Landscape** (Object placement) packing format, which is processed by `MapRegion.java` (often in a method like `method190`).

## Landscape Data Format (Byte Structure)
The landscape file is a compressed byte array containing a list of Object IDs and their positions/orientations. To save space, it uses "Smart" integers (1 or 2 bytes depending on the value) and stores coordinates as *offsets* rather than absolute values.

### The Parsing Loop
The parsing logic consists of an outer loop (iterating over Object IDs) and an inner loop (iterating over the locations where that specific Object ID is spawned).

#### 1. Outer Loop (Object IDs)
```java
int objectId = -1;
do {
    int idOffset = stream.readUSmart();
    if (idOffset == 0) break; // 0 marks the end of the file
    objectId += idOffset;
    
    // ... enter inner loop ...
} while(true);
```
**Explanation**: Instead of storing the full Object ID every time, the cache stores the *difference* between the current Object ID and the previous one. If `objectId` is 10, and `idOffset` is 5, the next object spawned is ID 15.

#### 2. Inner Loop (Locations)
```java
int locationData = 0;
do {
    int locationOffset = stream.readUSmart();
    if (locationOffset == 0) break; // 0 marks the end of locations for this Object ID
    
    locationData += locationOffset - 1;
    
    // Extract coordinates from the packed integer
    int localY = locationData & 0x3f;                 // 6 bits (0-63)
    int localX = (locationData >> 6) & 0x3f;          // 6 bits (0-63)
    int plane  = locationData >> 12;                  // Remaining bits (0-3)
    
    // Read the Object Type and Orientation
    int config = stream.readUnsignedByte();
    int type = config >> 2;                           // Top 6 bits
    int orientation = config & 3;                     // Bottom 2 bits
    
    // Spawn the object at (localX, localY, plane)
} while(true);
```
**Explanation**: 
-   Similar to Object IDs, the `locationData` is an offset from the previous location.
-   The `locationData` integer is heavily bit-packed:
    -   Bits 0-5 (Value `0-63`): The `localY` coordinate within the 64x64 region.
    -   Bits 6-11 (Value `0-63`): The `localX` coordinate within the 64x64 region.
    -   Bits 12+: The plane (Height level, `0-3`).
-   The final byte read per spawn (`config`) is also bit-packed:
    -   Bits 0-1 (Value `0-3`): The orientation/rotation (North, East, South, West).
    -   Bits 2-7 (Value `0-22`): The object "Type" (0-3 for walls, 10 for interactive objects, 22 for floor decorations).

## Important Edge Cases
-   **Bridge Logic**: When rendering objects, the client checks the underlying `tileFlags` (Map Terrain Data). If `(tileFlags[1][x][y] & 2) == 2`, it indicates a bridge or an area where the "ground" is technically on plane 1 instead of 0. In this case, objects spawned on plane 1 are rendered as if they were on plane 0, and their collision is applied to plane 0.
-   **Boundary Checks**: The parsing logic enforces boundary checks `if (actualX > 0 && actualY > 0 && actualX < 103 && actualY < 103 && plane >= 0 && plane < 4)` to prevent crashing when spawning objects on the very edge of a region.