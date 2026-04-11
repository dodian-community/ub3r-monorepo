# 17. Music Specification: MIDI Loading

## Overview
Music in RuneScape consists of standard MIDI files stored in Store 3 (`idx3`). The client does not contain a built-in MIDI renderer; instead, it hands the raw MIDI bytes to the Operating System's native MIDI sequencer via the `SignLink` bridge.

---

## 1. Storage & Identification
Music files are raw, uncompressed `.mid` files stored as individual entries in `idx3`.

- **Discovery**: The client finds the correct Song ID by resolving a "Music Index" (usually `midi_index` inside an archive).
- **Request**: When a song is requested (e.g., entering a new region), the client calls `resourceProvider.provide(3, song_id)`.

---

## 2. Playback Pipeline (Verified)
The playback follows a "Write-to-File" pattern to interface with the Java MIDI library.

1.  **Extraction**: The bytes are fetched from `idx3`.
2.  **Saving**: `saveMidi(flag, bytes[])` is called.
3.  **OS Bridge**: `SignLink.saveMidi()` writes the bytes to a local file (usually `jagex_cl_audio.mid` in the cache directory).
4.  **Sequencing**: The client sends a `"play"` string command to `SignLink`.
5.  **Native Play**: `SignLink` uses `javax.sound.midi.MidiSystem` to open the temporary file and begin playback.

---

## 3. Looping & Fading
The client manages music state via several flags:
- **`currentSong`**: The ID of the song currently playing.
- **`nextSong`**: The ID of the song scheduled to play.
- **`fadeMusic`**: A boolean. If true, `SignLink` gradually lowers the MIDI volume (CC 7) before stopping the current track and starting the next.

---

## 4. Tool Builder Implementation
To pack new music into the cache:
1.  **Format**: Ensure your file is a standard **Type 0 or Type 1 MIDI**.
2.  **Compression**: MIDI files in `idx3` are typically **not** BZip2 compressed (they are usually raw).
3.  **Packing**: Find the next available ID in `idx3` and use the sector allocation algorithm to write the bytes.
4.  **Registration**: Update the `midi_index` file in `config.jag` to give your song a name and a priority level.
