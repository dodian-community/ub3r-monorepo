# Mystic Cache: 2D Sprites (`index.dat` & `[name].dat`)

## Overview
Sprites are the 2D images used for the user interface, icons, minimap markers, and background images in the client. They are stored inside the `FileArchive` (usually `media.jag` or `interfaces.jag`).

Instead of storing images in standard `.png` or `.jpg` formats, RuneScape uses a custom, lightweight rasterized format. This format is heavily optimized for fast drawing onto the client's internal `Rasterizer2D.pixels` array.

## Sprite File Structure
A "Sprite Group" (like `chatbox.dat` inside `media.jag`) consists of two files:
1.  **The Index (`index.dat`)**: Contains the width, height, palette (colors), and offsets.
2.  **The Data (`[name].dat`)**: Contains the actual pixel indices for each sprite in the group.

The client parses these in `Sprite.java`:
```java
public Sprite(FileArchive archive, String name, int i)
```
*Note: `i` is the index of the specific sprite inside the group.*

## 1. Parsing the Index File (`index.dat`)

The `index.dat` file contains the metadata and the **Color Palette** for the sprite group.

### The Header
-   `indexBuffer.currentPosition = dataBuffer.readUShort();`
    -   *Crucial Detail*: The `data.dat` file's *first two bytes* dictate where in the `index.dat` file the metadata for this specific sprite group begins.
-   `maxWidth = indexBuffer.readUShort();`
-   `maxHeight = indexBuffer.readUShort();`
-   `pixelCount = indexBuffer.readUnsignedByte();` (Number of unique colors used in this sprite group).

### The Color Palette
RuneScape sprites do not store RGB values for every pixel. They use a palette.
The client allocates `int raster[] = new int[pixelCount]`.
It loops `pixelCount - 1` times:
-   `raster[pixel + 1] = indexBuffer.readTriByte();` (Reads a 24-bit RGB color).
-   If the color is exactly `0` (Black), it changes it to `1`. *This is because `0` is strictly reserved for absolute transparency.*

### Finding the Specific Sprite
A sprite group can contain multiple images (e.g., `magic_icons.dat` might have 20 different spell icons).
To find Sprite `#i`, the client skips forward in the buffers:
```java
for (int index = 0; index < i; index++) {
    indexBuffer.currentPosition += 2; // Skip X/Y offsets
    // Calculate total pixels of the skipped sprite and jump the data buffer forward
    dataBuffer.currentPosition += indexBuffer.readUShort() * indexBuffer.readUShort();
    indexBuffer.currentPosition++; // Skip format type
}
```

### The Sprite Header
Once it reaches the correct sprite index:
-   `drawOffsetX = indexBuffer.readUnsignedByte();`
-   `drawOffsetY = indexBuffer.readUnsignedByte();`
-   `myWidth = indexBuffer.readUShort();`
-   `myHeight = indexBuffer.readUShort();`
-   `type = indexBuffer.readUnsignedByte();` (0 = Row-Major, 1 = Column-Major).

## 2. Parsing the Data File (`[name].dat`)

The actual pixel data is incredibly simple now that the palette and dimensions are known.

The client allocates `myPixels = new int[myWidth * myHeight];`

### Format Type 0 (Row-Major)
The pixels are read left-to-right, top-to-bottom.
```java
for (int pixel = 0; pixel < spriteSize; pixel++) {
    // Read 1 byte. Use that byte as the index into the color palette.
    myPixels[pixel] = raster[dataBuffer.readUnsignedByte()];
}
```

### Format Type 1 (Column-Major)
The pixels are read top-to-bottom, left-to-right.
```java
for (int x = 0; x < myWidth; x++) {
    for (int y = 0; y < myHeight; y++) {
        myPixels[x + y * myWidth] = raster[dataBuffer.readUnsignedByte()];
    }
}
```

## 3. Transparency & Magic Pink
After loading the pixels, the client calls `setTransparency(255, 0, 255);`.
This loops through the entire `myPixels` array. If any pixel is exactly `RGB(255, 0, 255)` (Magic Pink), it sets the pixel value to `0`. 

During rendering, the `Rasterizer` ignores any pixel with a value of `0`. This is how the client supports transparent backgrounds on sprites without using heavy 32-bit ARGB PNG files.

## Building a Sprite Packer
If you are building a tool to pack `.png` files into the `media.jag` format:
1.  **Palette Generation**: You must scan all images in the group, extract up to 255 unique colors, and write them to the `index.dat` file.
2.  **Magic Pink**: You must ensure the transparent background of the PNG is converted to RGB `255, 0, 255`.
3.  **Pixel Mapping**: For each pixel in the PNG, you must find its color in your generated palette and write that *palette index* (a single byte) to the `[name].dat` file.