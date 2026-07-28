# Skill Plugin Modules

Gameplay skills live in independent Gradle modules under `game-server/plugins/skills/`. A module must
apply `ub3r.skill-plugin`, declare its implementation class, own its route
bindings, bundle TOML data, and test its behavior without a `Client` instance.

```kotlin
plugins { id("ub3r.skill-plugin") }

skillModule {
    implementationClass.set("net.dodian.uber.skills.example.ExampleModule")
}
```

Use `SkillPlugin` for a trainable skill and `SkillContentModule` for supporting
content such as Skill Guide. The convention plugin generates the module
descriptor consumed by startup discovery.

## Data

Keep data in `src/main/resources/<skill>/`. Every TOML file begins with
`schema_version = 1`, has one array-of-tables record kind, uses snake_case
fields, and has stable record keys. IDs, rates, requirements, and rewards are
data; policy choices and orchestration stay in Kotlin.

The shared reader fails on missing or malformed files. Module loaders must also
validate semantic constraints such as duplicate IDs, invalid levels, and broken
cross references.

## Tests

Use `FakeSkillPlayer` for route and action tests. Cover successful outcomes,
resource/level failures, cancellation, and transactional rollback. Run:

```text
./gradlew :skills:<name>:check
./gradlew skillsCheck
```

Modules may not import `Client`, unwrap engine adapters, or depend on
`:game-server`. Engine packet rendering, persistence wiring, and world access
belong in thin adapters only.
