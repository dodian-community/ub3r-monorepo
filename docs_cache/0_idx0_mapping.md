# Index 0 Archive Mapping

Through bitwise analysis of the `main_file_cache.idx0` file and extraction of the payloads, the following files have been identified. 

| File ID | Archive Name | Description |
| :--- | :--- | :--- |
| **1** | `title.jag` | Core config files: `obj.dat`, `loc.dat`, `npc.dat`, `flo.dat`. |
| **2** | `config.jag` | Additional configs: `varbit.dat`, `seq.dat`, `spotanim.dat`. |
| **3** | `interfaces.jag`| UI data file (`data`) and sprites used in interfaces. |
| **4** | `media.jag` | Game-wide sprites: icons, chatbox, buttons. |
| **5** | `versionlist.jag`| Version hashes for all files in the cache. |
| **6** | `textures.jag` | Texture definitions used by `flo.dat` overlays. |
| **7** | `wordpack.jag` | Chat filtering and word packs. |
| **8** | `sounds.jag` | Sound effect metadata. |

---

## Technical Proof
Analysis of `main_file_cache.idx0` entry 1 (Sector 1):
- `FileID: 1`, `Part: 0`, `Next: 2`, `CacheID: 1` (verified in `analyze_cache_core.py`).
- Extraction of Sector 1-618 results in 316,205 bytes of data.
- The 10-byte directory entries in this payload contain hashes matching `obj.dat`, `npc.dat`, and `loc.dat`.
