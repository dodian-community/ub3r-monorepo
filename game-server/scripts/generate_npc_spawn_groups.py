#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_DIR = Path(__file__).resolve().parent
NPC_DEF_FILE = SCRIPT_DIR / "npc_Def.json"
NPC_SPAWN_FILE = SCRIPT_DIR / "npc_Spawn.json"
OUT_DIR = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "spawns"
LEGACY_CONTENT_OUT = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "LegacySqlNpcSpawns.kt"
SQL_CONTENT_OUT = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "SqlNpcSpawns.kt"

LEADING_ADJECTIVES = {
    "baby", "blue", "black", "brown", "dark", "deadly", "dungeon", "fire", "giant", "greater", "green",
    "ice", "jungle", "king", "lesser", "little", "master", "mighty", "mini", "mithril", "poison", "queen",
    "red", "small", "white", "young", "old", "war", "battle", "cave", "mountain", "wild", "ancient",
}

GENERIC_TAIL = {"the", "of", "and", "a", "an"}

TYPE_ALIASES = {
    "adventur": "adventurer",
    "officier": "officer",
    "salesm": "salesman",
}

# For placeholder/utility names, pin known gameplay-driven buckets.
SAFE_ID_TYPE_OVERRIDES = {
    556: "premium_store",
    5792: "party_announcer",
    3306: "population_announcer",
    2978: "big_snake",
    4362: "ogre_chieftain",
    4382: "ogre_shaman",
    4401: "ogre_trader",
    5971: "mounted_terrorbird_gnome",
    6370: "flambeed",
    7102: "glough",
    7704: "jal_tok_jad",
    7707: "ancestral_glyph",
    7894: "sand_snake_hard",
    7895: "sand_snake",
}


def _as_int(value: object, default: int = 0) -> int:
    if value is None:
        return default
    if isinstance(value, bool):
        return int(value)
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    if isinstance(value, str):
        stripped = value.strip()
        if stripped == "":
            return default
        return int(stripped)
    return int(value)


def load_json_rows(path: Path) -> list[dict[str, object]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError(f"Expected JSON array in {path}")
    rows: list[dict[str, object]] = []
    for row in data:
        if isinstance(row, dict):
            rows.append(row)
    return rows


def parse_npc_definitions() -> dict[int, dict[str, int | str]]:
    definitions: dict[int, dict[str, int | str]] = {}
    for row in load_json_rows(NPC_DEF_FILE):
        try:
            npc_id = _as_int(row.get("id"))
        except Exception:
            continue
        if npc_id <= 0:
            continue
        definitions[npc_id] = {
            "name": str(row.get("name") or ""),
            "hitpoints": _as_int(row.get("hitpoints"), 0),
            "respawn": _as_int(row.get("respawn"), 60),
            "attack": _as_int(row.get("attack"), 0),
            "strength": _as_int(row.get("strength"), 0),
            "defence": _as_int(row.get("defence"), 0),
            "ranged": _as_int(row.get("ranged"), 0),
            "magic": _as_int(row.get("magic"), 0),
        }
    return definitions


def parse_spawn_rows() -> list[dict[str, int | None]]:
    rows: list[dict[str, int | None]] = []
    for row in load_json_rows(NPC_SPAWN_FILE):
        try:
            npc_id = _as_int(row.get("id"))
            x = _as_int(row.get("x"))
            y = _as_int(row.get("y"))
            z = _as_int(row.get("height"), 0)
            face = _as_int(row.get("face"), 0)
        except Exception:
            continue

        hp_raw = row.get("hitpoints")
        spawn_hp = None if hp_raw is None else _as_int(hp_raw, 0)
        rows.append({"npc_id": npc_id, "x": x, "y": y, "z": z, "spawn_hp": spawn_hp, "face": face})
    return rows


def canonical_type(name: str | None, npc_id: int) -> str:
    if npc_id in SAFE_ID_TYPE_OVERRIDES:
        return SAFE_ID_TYPE_OVERRIDES[npc_id]

    if not name:
        return f"npc_{npc_id}"

    normalized = re.sub(r"[^a-z0-9_ ]+", "", name.lower().replace("-", "_")).replace("_", " ")
    if normalized in {"", "null", "no name"}:
        return f"npc_{npc_id}"
    if normalized.isdigit():
        return f"npc_{npc_id}"

    tokens = [t for t in normalized.split() if t and t not in GENERIC_TAIL]
    if not tokens:
        return f"npc_{npc_id}"
    if all(t.isdigit() for t in tokens):
        return f"npc_{npc_id}"

    # Collapse color/descriptor variants to a single base type.
    while len(tokens) > 1 and tokens[0] in LEADING_ADJECTIVES:
        tokens = tokens[1:]

    base = tokens[-1]
    if base.isdigit():
        return f"npc_{npc_id}"

    if base == "spot" and "fishing" in tokens:
        base = "fish_spot"
    elif base == "spawn" and "death" in tokens:
        base = "death_spawn"

    if base.endswith("ies") and len(base) > 4:
        base = base[:-3] + "y"
    elif base.endswith("s") and len(base) > 3 and not base.endswith("ss"):
        base = base[:-1]

    if base == "men":
        base = "man"

    base = TYPE_ALIASES.get(base, base)

    return base or f"npc_{npc_id}"


def to_pascal(s: str) -> str:
    cleaned = re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")
    if not cleaned:
        return "Unknown"
    out = "".join(part.capitalize() for part in cleaned.split("_") if part)
    if out and out[0].isdigit():
        out = "Npc" + out
    return out


def kotlin_preset_call(npc_definition: dict[str, int | str]) -> str:
    return (
        "NpcDataPreset("
        f"respawnTicks = {int(npc_definition['respawn'])}, "
        f"attack = {int(npc_definition['attack'])}, "
        f"defence = {int(npc_definition['defence'])}, "
        f"strength = {int(npc_definition['strength'])}, "
        f"hitpoints = {int(npc_definition['hitpoints'])}, "
        f"ranged = {int(npc_definition['ranged'])}, "
        f"magic = {int(npc_definition['magic'])}"
        ")"
    )


def kotlin_spawn_call(row: dict[str, int | None], npc_definition: dict[str, int | str] | None) -> str:
    args = [
        f"npcId = {row['npc_id']}",
        f"x = {row['x']}",
        f"y = {row['y']}",
        f"z = {row['z']}",
        f"face = {row['face']}",
    ]

    if npc_definition is not None:
        args.append(f"preset = {kotlin_preset_call(npc_definition)}")

    if row["spawn_hp"] is not None:
        args.append(f"hitpoints = {row['spawn_hp']}")

    return "NpcSpawnDef(" + ", ".join(args) + ")"


def write_group_files(
    grouped: dict[str, list[tuple[int, dict[str, int | None]]]],
    npc_definitions: dict[int, dict[str, int | str]],
) -> list[str]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    # Clear old generated files, but keep hand-maintained base types.
    keep_files = {"NpcSpawnDef.kt", "NpcDataPreset.kt"}
    for existing in OUT_DIR.glob("*.kt"):
        if existing.name in keep_files:
            continue
        existing.unlink()

    class_names: list[str] = []
    for group_key in sorted(grouped.keys()):
        class_name = to_pascal(group_key)
        class_names.append(class_name)
        file_path = OUT_DIR / f"{class_name}.kt"

        entries = grouped[group_key]
        lines = [
            "package net.dodian.uber.game.content.npcs.spawns",
            "",
            "// AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py",
            "",
            f"internal object {class_name} {{",
            "    val entries: List<SpawnEntry> = listOf(",
        ]

        for index, row in entries:
            spawn_call = kotlin_spawn_call(row, npc_definitions.get(int(row["npc_id"])))
            lines.append(f"        SpawnEntry(index = {index}, spawn = {spawn_call}),")

        lines.extend([
            "    )",
            "}",
            "",
        ])
        file_path.write_text("\n".join(lines), encoding="utf-8")

    return class_names


def write_shared_files(class_names: list[str]) -> None:
    entry_file = OUT_DIR / "SpawnEntry.kt"
    entry_file.write_text(
        "\n".join([
            "package net.dodian.uber.game.content.npcs.spawns",
            "",
            "// AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py",
            "",
            "internal data class SpawnEntry(",
            "    val index: Int,",
            "    val spawn: NpcSpawnDef,",
            ")",
            "",
        ]),
        encoding="utf-8",
    )

    groups_file = OUT_DIR / "SpawnGroups.kt"
    aggregate_lines = [
        "package net.dodian.uber.game.content.npcs.spawns",
        "",
        "// AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py",
        "",
        "/**",
        " * Generated spawn groups from scripts/npc_Spawn.json + scripts/npc_Def.json.",
        " * Aggregation preserves source row order.",
        " */",
        "object SpawnGroups {",
        "    private val allEntries: List<SpawnEntry> = listOf(",
    ]
    for class_name in class_names:
        aggregate_lines.append(f"        {class_name}.entries,")
    aggregate_lines.extend([
        "    ).flatten()",
        "",
        "    private val allSpawns: List<NpcSpawnDef> = allEntries.asSequence()",
        "        .sortedBy { it.index }",
        "        .map { it.spawn }",
        "        .toList()",
        "",
        "    @JvmStatic",
        "    fun all(): List<NpcSpawnDef> = allSpawns",
        "}",
        "",
    ])
    groups_file.write_text("\n".join(aggregate_lines), encoding="utf-8")


def main() -> None:
    npc_definitions = parse_npc_definitions()
    names = {npc_id: str(definition["name"]) for npc_id, definition in npc_definitions.items()}
    spawn_rows = parse_spawn_rows()

    grouped: dict[str, list[tuple[int, dict[str, int | None]]]] = defaultdict(list)
    for idx, row in enumerate(spawn_rows):
        npc_id = int(row["npc_id"])
        group_key = canonical_type(names.get(npc_id), npc_id)
        grouped[group_key].append((idx, row))

    class_names = write_group_files(grouped, npc_definitions)
    write_shared_files(class_names)

    for stale_content in (LEGACY_CONTENT_OUT, SQL_CONTENT_OUT):
        if stale_content.exists():
            stale_content.unlink()

    print(f"Generated {len(spawn_rows)} spawn rows into {len(class_names)} type groups.")
    unresolved_groups = sorted(k for k in grouped.keys() if k.startswith("npc_"))
    if unresolved_groups:
        print(f"Unresolved placeholder groups: {', '.join(unresolved_groups)}")


if __name__ == "__main__":
    main()
