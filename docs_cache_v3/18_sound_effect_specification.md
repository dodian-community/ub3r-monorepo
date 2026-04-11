# 18. Sound Specification: Software Synthesis

## Overview
Unlike Music (which uses external MIDI), Sound Effects (like combat sounds or spell effects) are synthesized entirely within the client using a custom software synthesizer. This data is stored in the `sounds.jag` archive.

---

## 1. The Synthesis Logic
The client does not store raw audio waves. It stores "Recipes" for sounds. 
- **Format**: Jagex Software Synthesizer.
- **Output**: 8-bit Mono PCM, 22050Hz.
- **Container**: The client generates a standard **RIFF WAV** header in memory and appends the synthesized samples.

---

## 2. File Format (`sounds.jag`)
The archive contains a single stream of sound definitions.

### The Loader Loop
1. Read `UInt16` `id`. If `0xFFFF`, terminate.
2. For each ID, read up to **10 Synthesizers** (oscillators/layers).
3. For each Synthesizer:
    - Read `UInt8` `valid`.
    - If `valid != 0`, call `Synthesizer.decode()`.
4. Read `UInt16` `loop_start`.
5. Read `UInt16` `loop_end`.

---

## 3. The Synthesizer Specification (`Synthesizer.java`)
Each of the 10 oscillators reads a complex set of envelopes and filters.

### Opcode-based Decoding (Simplified)
| Field | Description |
| :--- | :--- |
| **Envelopes** | Pitch, Amplitude, and Gating. Uses segmented line-based data. |
| **Oscillators**| Sine, Square, Sawtooth, or Noise. |
| **Filters** | Low-pass or Band-pass. |
| **Effects** | Reverb and Echo parameters. |

---

## 4. Building a Sound Editor
If you want to add new sound effects:
1.  **Do not pack WAVs**: You cannot inject raw audio into `sounds.jag`.
2.  **Algorithm**: You must implement an interface that allows users to manipulate:
    - **Pitch Envelopes**: (Attack, Decay, Sustain, Release).
    - **Amplitude Envelopes**: Controls the volume over time.
    - **Filters**: Cutoff frequency and resonance.
3.  **Encoding**: Serialize these parameters using the same byte-order as `Track.java` and append them to the definition stream.

---

## 5. Summary of WAV Header (Generated at Runtime)
When the client prepares a sound for playback, it writes this 44-byte header to a buffer:
- `ChunkID`: "RIFF"
- `Format`: "WAVE"
- `Subchunk1ID`: "fmt "
- `AudioFormat`: 1 (PCM)
- `NumChannels`: 1 (Mono)
- `SampleRate`: 22050
- `BitsPerSample`: 8
