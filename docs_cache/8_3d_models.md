# Mystic Cache: 3D Models (`main_file_cache.idx1`)

## Overview
3D Models in the RuneScape cache (stored in `idx1`/Store 1) define the geometry (vertices and triangles), colors, textures, and animation weighting for everything in the game: Characters, NPCs, Objects, and Items.

The client parses these models in `Model.java` via the `readOldModel` (or `readNewModel`) methods.

## The Old Model Format (RS2 317)
The model file is a highly compressed byte array consisting of several distinct blocks. Unlike standard 3D formats (like `.obj` or `.gltf`), RuneScape models separate the X, Y, and Z coordinates into entirely different byte blocks to maximize compression.

### 1. The Header (The "Footer")
Counter-intuitively, the "Header" of an old RS2 model is stored at the **very end** of the file. The client seeks to `data.length - 18` to read the metadata before parsing anything else.

| Offset | Type | Description |
| :--- | :--- | :--- |
| `0` | `UShort` | Number of Vertices (`numVertices`). |
| `2` | `UShort` | Number of Triangles/Faces (`numTriangles`). |
| `4` | `UnsignedByte`| Number of Textured Faces. |
| `5` | `UnsignedByte`| Has Face Types (1 = yes). |
| `6` | `UnsignedByte`| Has Face Priorities (255 = yes). |
| `7` | `UnsignedByte`| Has Face Alpha/Transparency (1 = yes). |
| `8` | `UnsignedByte`| Has Triangle Skin/Anim Bones (1 = yes). |
| `9` | `UnsignedByte`| Has Vertex Skin/Anim Bones (1 = yes). |
| `10` | `UShort` | Size of X Coordinate Data. |
| `12` | `UShort` | Size of Y Coordinate Data. |
| `14` | `UShort` | Size of Z Coordinate Data. |
| `16` | `UShort` | Size of Triangle Index Data. |

### 2. Memory Allocation & Offsets
Based on the header flags, the client allocates several memory blocks. Because the data is stored sequentially, the client calculates the "starting offset" for each block.
For example, the Z-coordinates start at: `numVertices + numTriangles + (if priority == 255 ? numTriangles : 0) + ...`

### 3. Parsing Vertices (Points in 3D Space)
Vertices are not stored as `[x, y, z], [x, y, z]`. Instead, the client reads a `flag` array first.
-   The flag byte determines which axes changed since the last vertex.
-   `flag & 0x1`: The X coordinate changed. Read a `Smart` integer from the X-block.
-   `flag & 0x2`: The Y coordinate changed. Read a `Smart` integer from the Y-block.
-   `flag & 0x4`: The Z coordinate changed. Read a `Smart` integer from the Z-block.

Because models are usually continuous meshes, vertices are often very close to the previous vertex. Storing the *difference* as a 1-byte `Smart` integer (rather than a 4-byte `Int`) saves massive amounts of space.

### 4. Parsing Triangles (Faces)
Triangles connect three vertices (A, B, C) together.
-   **Colors**: The client reads an array of `UShort`s. This is the HSL color value for the face. (If the face is textured, the color is ignored later).
-   **Types & Textures**: If the `type_opcode == 1`, it reads a flag. If `(flag & 0x2) != 0`, the face has a texture.
-   **Alpha**: If `alpha_opcode == 1`, it reads a `SignedByte` for each face. 0 is solid, 255 is invisible.
-   **Priorities**: Dictates rendering order (what draws on top of what). If `priority_opcode == 255`, each face gets its own priority byte. Otherwise, the whole model uses the priority defined in the opcode byte itself.

### 5. Connecting the Triangles
To connect the vertices, the client reads an opcode stream:
-   **Opcode 1**: All three vertices (A, B, C) changed. Read three `Smart` integers.
-   **Opcode 2**: Vertices B and C changed. Vertex A remains the same as the last triangle.
-   **Opcode 3**: Vertices A and C changed. Vertex B remains the same.
-   **Opcode 4**: Vertices A and B changed. Vertex C remains the same.

This is a form of **Triangle Strip/Fan compression**.

### 6. Skin/Bones (Animation Data)
If the model is animated (like a player or an NPC), it contains "Skin" data.
-   `vSkin_opcode`: Reads a byte for each vertex indicating which "Bone" it is attached to.
-   `tSkin_opcode`: Reads a byte for each triangle indicating which "Bone" it is attached to.
When an animation frame is applied, the client moves the bones, and the attached vertices/triangles move with them.

## Building a Model Packer
If you are building a tool to inject `.obj` or `.dat` models into the cache:
1.  **Smart Integers**: You must pack coordinates using the RS Smart Integer format (1 byte for values `-64 to 63`, 2 bytes for `-16384 to 16383`).
2.  **Block Ordering**: You must write the blocks in the exact sequential order the client expects: `Flags -> FaceTypes -> Priorities -> Alpha -> TSkin -> VSkin -> TriangleOpcode -> FacePoints -> Textures -> X -> Y -> Z -> Header`.
3.  **Color Space**: RS uses a modified 16-bit HSL color space. You will need an algorithm to convert standard RGB colors from a `.obj` file to RS HSL.