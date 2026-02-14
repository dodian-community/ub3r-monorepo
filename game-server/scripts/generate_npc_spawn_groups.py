#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path
from typing import TypedDict

# DEPRECATION NOTICE:
# This generator remains for compatibility/migration only.
# New or updated NPC spawn work should be authored in Kotlin NPC function files
# under src/main/kotlin/net/dodian/uber/game/content/npcs/spawns.

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_DIR = Path(__file__).resolve().parent
NPC_DEF_FILE = SCRIPT_DIR / "npc_Def.json"
NPC_SPAWN_FILE = SCRIPT_DIR / "npc_Spawn.json"
OUT_DIR = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "spawns"
LEGACY_CONTENT_OUT = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "LegacySqlNpcSpawns.kt"
SQL_CONTENT_OUT = REPO_ROOT / "src" / "main" / "kotlin" / "net" / "dodian" / "uber" / "game" / "content" / "npcs" / "SqlNpcSpawns.kt"

GENERIC_TAIL = {"the", "of", "and", "a", "an"}

TYPE_ALIASES = {
    "banker": "banker_generated",
    "adventur": "adventurer",
    "officier": "officer",
    "salesm": "salesman",
}

# For placeholder/utility names, pin known gameplay-driven buckets.
SAFE_ID_TYPE_OVERRIDES = {
    56: "placeholder_npc",
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
    3640: "beginner_store",
    4218: "plague_warning",
    4965: "thieving_skillcape_shop",
}

NAME_ALIASES = {
    "whatever_the_fuck": "placeholder_npc",
}

# NPC ids that are now owned by Kotlin NPC function files.
# Keep these out of generated SpawnGroups so we maintain one source of truth.
FUNCTION_OWNED_NPC_IDS = {
    1306,  # MakeoverMage
    637,   # Aubury
}


class SpawnRow(TypedDict):
    npc_id: int
    x: int
    y: int
    z: int
    face: int


def _ensure_exists(path: Path) -> None:
    if not path.exists():
        raise FileNotFoundError(f"Missing required input file: {path}")


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
    _ensure_exists(path)
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError(f"Expected JSON array in {path}")

    rows: list[dict[str, object]] = []
    for i, row in enumerate(data):
        if not isinstance(row, dict):
            raise ValueError(f"Row {i} in {path} is not a JSON object")
        rows.append(row)
    return rows


def parse_npc_definitions() -> dict[int, dict[str, int | str]]:
    definitions: dict[int, dict[str, int | str]] = {}
    duplicate_ids: list[int] = []

    for idx, row in enumerate(load_json_rows(NPC_DEF_FILE)):
        try:
            npc_id = _as_int(row.get("id"))
        except Exception:
            raise ValueError(f"Invalid npc definition id at row {idx} in {NPC_DEF_FILE}: {row.get('id')!r}") from None
        if npc_id <= 0:
            raise ValueError(f"Invalid npc definition id at row {idx} in {NPC_DEF_FILE}: {npc_id}")
        if npc_id in definitions:
            duplicate_ids.append(npc_id)

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

    if duplicate_ids:
        sample = ", ".join(str(v) for v in sorted(set(duplicate_ids))[:10])
        print(f"Warning: duplicate NPC definition ids in {NPC_DEF_FILE}; using last row for ids: {sample}")

    return definitions


def parse_spawn_rows() -> list[SpawnRow]:
    rows: list[SpawnRow] = []

    for idx, row in enumerate(load_json_rows(NPC_SPAWN_FILE)):
        try:
            npc_id = _as_int(row.get("id"))
            x = _as_int(row.get("x"))
            y = _as_int(row.get("y"))
            z = _as_int(row.get("height"), 0)
            face = _as_int(row.get("face"), 0)
        except Exception:
            raise ValueError(f"Invalid spawn row {idx} in {NPC_SPAWN_FILE}: {row!r}") from None
        if npc_id <= 0:
            raise ValueError(f"Spawn row {idx} has invalid npc id ({npc_id}) in {NPC_SPAWN_FILE}")
        if npc_id in FUNCTION_OWNED_NPC_IDS:
            continue

        rows.append({"npc_id": npc_id, "x": x, "y": y, "z": z, "face": face})

    if not rows:
        raise ValueError(f"No spawn rows parsed from {NPC_SPAWN_FILE}")

    return rows


def canonical_type(name: str | None, npc_id: int) -> str:
    if npc_id in SAFE_ID_TYPE_OVERRIDES:
        return SAFE_ID_TYPE_OVERRIDES[npc_id]

    if not name:
        return f"unknown_npc_{npc_id}"

    raw_name = name.strip().lower().replace(" ", "_")
    if raw_name in NAME_ALIASES:
        return NAME_ALIASES[raw_name]

    normalized = re.sub(r"[^a-z0-9_ ]+", "", name.lower().replace("-", "_")).replace("_", " ")
    if normalized in {"", "null", "no name"}:
        return f"unknown_npc_{npc_id}"
    if normalized.isdigit():
        return f"unknown_npc_{npc_id}"

    tokens = [t for t in normalized.split() if t]
    if not tokens:
        return f"unknown_npc_{npc_id}"
    if all(t.isdigit() for t in tokens):
        return f"unknown_npc_{npc_id}"

    # Trim common filler words and trailing numeric suffixes like "_2" / "_3".
    tokens = [t for t in tokens if t not in GENERIC_TAIL]
    while len(tokens) > 1 and tokens[-1].isdigit():
        tokens = tokens[:-1]
    if not tokens:
        return f"unknown_npc_{npc_id}"

    last = tokens[-1]
    if last.isdigit():
        return f"unknown_npc_{npc_id}"

    # Pin a few well-known buckets that should remain grouped together.
    if last == "spot" and "fishing" in tokens:
        return "fish_spot"
    if last == "spawn" and "death" in tokens:
        return "death_spawn"

    # Singularize only the last token to keep type names stable while preserving qualifiers.
    if last.endswith("ies") and len(last) > 4:
        last = last[:-3] + "y"
    elif last.endswith("s") and len(last) > 3 and not last.endswith("ss"):
        last = last[:-1]

    if last == "men":
        last = "man"

    last = TYPE_ALIASES.get(last, last)
    tokens[-1] = last

    group_key = "_".join(tokens)
    if not group_key or group_key.isdigit():
        return f"unknown_npc_{npc_id}"
    return group_key


def to_pascal(s: str) -> str:
    cleaned = re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")
    if not cleaned:
        return "Unknown"
    out = "".join(part.capitalize() for part in cleaned.split("_") if part)
    if out and out[0].isdigit():
        out = "Npc" + out
    return out


def kotlin_spawn_call(row: SpawnRow) -> str:
    args = [
        f"npcId = {row['npc_id']}",
        f"x = {row['x']}",
        f"y = {row['y']}",
        f"z = {row['z']}",
        f"face = {row['face']}",
    ]

    return "NpcSpawnDef(" + ", ".join(args) + ")"


def unique_class_name(base_name: str, used: set[str], first_npc_id: int) -> str:
    if base_name not in used:
        return base_name

    candidate = f"{base_name}{first_npc_id}"
    if candidate not in used:
        return candidate

    suffix = 2
    while True:
        candidate = f"{base_name}{suffix}"
        if candidate not in used:
            return candidate
        suffix += 1


def clear_old_generated_files() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    # Clear old generated files, but keep hand-maintained base types.
    keep_files = {"NpcSpawnDef.kt", "NpcDataPreset.kt"}
    for existing in OUT_DIR.glob("*.kt"):
        if existing.name in keep_files:
            continue
        try:
            content = existing.read_text(encoding="utf-8")
        except Exception:
            continue
        if "AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py" in content:
            existing.unlink()


def write_group_files(
    grouped: dict[str, list[SpawnRow]],
    group_order: list[str],
    class_name_by_group: dict[str, str],
    npc_definitions: dict[int, dict[str, int | str]],
) -> None:
    clear_old_generated_files()

    for group_key in group_order:
        class_name = class_name_by_group[group_key]
        file_path = OUT_DIR / f"{class_name}.kt"
        entries = grouped[group_key]
        lines = [
            "package net.dodian.uber.game.content.npcs.spawns",
            "",
            "// AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py",
            "",
            f"internal object {class_name} {{",
        ]
        stats_parts: list[str] = []
        seen_npc_ids: set[int] = set()
        for row in entries:
            npc_id = int(row["npc_id"])
            if npc_id in seen_npc_ids:
                continue
            seen_npc_ids.add(npc_id)
            definition = npc_definitions.get(npc_id)
            if definition is None:
                stats_parts.append(f"{npc_id}: unknown")
                continue
            stats_parts.append(
                f"{npc_id}: r={int(definition['respawn'])} "
                f"a={int(definition['attack'])} d={int(definition['defence'])} "
                f"s={int(definition['strength'])} hp={int(definition['hitpoints'])} "
                f"rg={int(definition['ranged'])} mg={int(definition['magic'])}"
            )
        lines.append("    // Stats: " + "; ".join(stats_parts))
        lines.extend([
            "",
            "    val entries: List<NpcSpawnDef> = listOf(",
        ])

        for row in entries:
            spawn_call = kotlin_spawn_call(row)
            lines.append(f"        {spawn_call},")

        lines.extend([
            "    )",
            "}",
            "",
        ])
        file_path.write_text("\n".join(lines), encoding="utf-8")


def write_shared_files(
    group_order: list[str],
    class_name_by_group: dict[str, str],
    ordered_refs: list[tuple[str, int]],
) -> None:
    groups_file = OUT_DIR / "SpawnGroups.kt"
    aggregate_lines = [
        "package net.dodian.uber.game.content.npcs.spawns",
        "",
        "// AUTO-GENERATED by game-server/scripts/generate_npc_spawn_groups.py",
        "",
        "/**",
        " * Generated spawn groups from scripts/npc_Spawn.json + scripts/npc_Def.json.",
        " * Group files are type-centric; all() preserves exact source row order from npc_Spawn.json.",
        " */",
        "object SpawnGroups {",
        "    private val typeGroups: List<List<NpcSpawnDef>> = listOf(",
    ]
    for group_key in group_order:
        class_name = class_name_by_group[group_key]
        aggregate_lines.append(f"        {class_name}.entries,")
    aggregate_lines.extend([
        "    )",
        "",
        "    private val allSpawns: List<NpcSpawnDef> = listOf(",
    ])

    for group_key, local_idx in ordered_refs:
        class_name = class_name_by_group[group_key]
        aggregate_lines.append(f"        {class_name}.entries[{local_idx}],")

    aggregate_lines.extend([
        "    )",
        "",
        "    @JvmStatic",
        "    fun allByTypeGroup(): List<List<NpcSpawnDef>> = typeGroups",
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

    grouped: dict[str, list[SpawnRow]] = defaultdict(list)
    group_first_row: dict[str, int] = {}
    ordered_refs: list[tuple[str, int]] = []

    for idx, row in enumerate(spawn_rows):
        npc_id = int(row["npc_id"])
        group_key = canonical_type(names.get(npc_id), npc_id)
        if group_key not in group_first_row:
            group_first_row[group_key] = idx
        local_idx = len(grouped[group_key])
        grouped[group_key].append(row)
        ordered_refs.append((group_key, local_idx))

    group_order = sorted(grouped.keys(), key=lambda key: (group_first_row[key], key))
    class_name_by_group: dict[str, str] = {}
    used_class_names: set[str] = set()
    for group_key in group_order:
        base_name = to_pascal(group_key)
        first_npc_id = int(grouped[group_key][0]["npc_id"])
        class_name = unique_class_name(base_name, used_class_names, first_npc_id)
        class_name_by_group[group_key] = class_name
        used_class_names.add(class_name)

    write_group_files(grouped, group_order, class_name_by_group, npc_definitions)
    write_shared_files(group_order, class_name_by_group, ordered_refs)

    for stale_content in (LEGACY_CONTENT_OUT, SQL_CONTENT_OUT):
        if stale_content.exists():
            stale_content.unlink()

    print(f"Generated {len(spawn_rows)} spawn rows into {len(grouped)} type groups.")
    unresolved_groups = sorted(k for k in grouped.keys() if k.startswith("unknown_npc_"))
    if unresolved_groups:
        print(f"Unnamed NPC groups: {', '.join(unresolved_groups)}")

    missing_defs = sorted({int(row["npc_id"]) for row in spawn_rows if int(row["npc_id"]) not in npc_definitions})
    if missing_defs:
        preview = ", ".join(str(v) for v in missing_defs[:20])
        print(
            f"Warning: {len(missing_defs)} spawn npc ids have no npc_Def reference row; "
            f"MySQL defaults will be used at runtime: {preview}"
        )


if __name__ == "__main__":
    main()
