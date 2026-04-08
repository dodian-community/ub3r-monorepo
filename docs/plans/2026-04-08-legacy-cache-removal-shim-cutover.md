# Legacy Cache Removal Shim Cutover Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove `game-server/src/main/java/net/dodian/cache` entirely while keeping the server bootable and gameplay systems running through explicit compatibility stubs until the new cache system is ready.

**Architecture:** Replace the legacy Java cache package with a minimal Kotlin compatibility surface that keeps the same package names (`net.dodian.cache.*`) but removes all cache-file decoding and index parsing. Keep behavior deterministic and non-blocking by using in-memory defaults/fallbacks only, then guard with architecture tests so no deleted legacy classes reappear.

**Tech Stack:** Kotlin/JVM 1.6.21, Java 17, Gradle, JUnit 5 architecture boundary tests.

---

### Task 1: Lock In Guardrails Before Deletion

**Files:**
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/LegacyCachePackageBoundaryTest.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ArchitectureBoundaryTest.kt` (only if you prefer centralizing checks there)

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyCachePackageBoundaryTest {
    @Test
    fun `legacy java cache package is removed`() {
        val root = Paths.get("src/main/java/net/dodian/cache")
        if (!Files.exists(root)) return

        val javaFiles = Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "java" }.toList()
        }
        assertTrue(
            javaFiles.isEmpty(),
            "Legacy Java cache files must be removed. Found: ${javaFiles.joinToString(",")}",
        )
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.LegacyCachePackageBoundaryTest"`
Expected: FAIL listing Java files under `src/main/java/net/dodian/cache`.

**Step 3: Add import-boundary test for cache internals**

```kotlin
@Test
fun `source does not import removed cache internals`() {
    val root = Paths.get("src/main")
    val forbidden = listOf(
        "import net.dodian.cache.Cache",
        "import net.dodian.cache.Archive",
        "import net.dodian.cache.index",
        "import net.dodian.cache.map",
        "import net.dodian.cache.obj",
    )
    val violations = mutableListOf<String>()

    Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && (it.extension == "kt" || it.extension == "java") }
            .forEach { file ->
                Files.readAllLines(file).forEachIndexed { idx, line ->
                    val trimmed = line.trim()
                    if (forbidden.any { trimmed.startsWith(it) }) {
                        violations += "${file}:${idx + 1} -> $trimmed"
                    }
                }
            }
    }

    assertTrue(violations.isEmpty(), "Removed cache internals must not be imported.\n${violations.joinToString("\n")}")
}
```

**Step 4: Run tests to verify both fail first**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.LegacyCachePackageBoundaryTest"`
Expected: FAIL (package still exists + old imports still present).

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/LegacyCachePackageBoundaryTest.kt
git commit -m "test: add legacy cache removal guardrails"
```

### Task 2: Add Kotlin Compatibility Surface (No Cache Decoding)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/cache/object/GameObjectData.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/object/GameObjectDef.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/object/CacheObject.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/object/ObjectLoader.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/object/ObjectDef.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/region/Region.kt`
- Create: `game-server/src/main/kotlin/net/dodian/cache/util/ByteStream.kt`

**Step 1: Write the failing test for fallback object data contract**

Create in `game-server/src/test/kotlin/net/dodian/uber/game/systems/cache/LegacyObjectDataShimTest.kt`:

```kotlin
package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyObjectDataShimTest {
    @Test
    fun `forId always returns fallback definition`() {
        val data = GameObjectData.forId(999999)
        assertNotNull(data)
        assertTrue(data.name.isNotBlank())
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.cache.LegacyObjectDataShimTest"`
Expected: FAIL because shim classes do not exist yet.

**Step 3: Implement minimal shims**

`GameObjectData.kt` (minimal shape used across Kotlin/Java call sites):

```kotlin
package net.dodian.cache.`object`

import java.util.concurrent.ConcurrentHashMap

class GameObjectData(
    private val idValue: Int,
    private var nameValue: String,
    private val descriptionValue: String,
    private val sizeXValue: Int,
    private val sizeYValue: Int,
    private val solidValue: Boolean,
    private val walkableValue: Boolean,
    private val hasActionsValue: Boolean,
    private val unknownValue: Boolean,
    private val walkType: Int,
) {
    val id: Int get() = idValue
    val name: String get() = nameValue

    fun getId(): Int = idValue
    fun getName(): String = nameValue
    fun getDescription(): String = descriptionValue
    fun getSizeX(): Int = sizeXValue
    fun getSizeY(): Int = sizeYValue
    fun getSizeX(rotation: Int): Int = if (rotation == 1 || rotation == 3) sizeYValue else sizeXValue
    fun getSizeY(rotation: Int): Int = if (rotation == 1 || rotation == 3) sizeXValue else sizeYValue
    fun isSolid(): Boolean = solidValue
    fun isWalkable(): Boolean = walkableValue
    fun hasActions(): Boolean = hasActionsValue
    fun unknown(): Boolean = unknownValue
    fun isRangeAble(): Boolean = walkType <= 1 || (walkType == 2 && !solidValue)
    fun canShootThru(): Boolean = !solidValue

    companion object {
        private val definitions = ConcurrentHashMap<Int, GameObjectData>()

        @JvmStatic
        fun init() {
            // Intentionally no-op during transitional cutover.
        }

        @JvmStatic
        fun addDefinition(def: GameObjectData?) {
            if (def != null) definitions[def.getId()] = def
        }

        @JvmStatic
        fun forId(id: Int): GameObjectData {
            return definitions.computeIfAbsent(id) {
                GameObjectData(
                    idValue = id,
                    nameValue = "Object: #$id",
                    descriptionValue = "Legacy cache removed; fallback definition",
                    sizeXValue = 1,
                    sizeYValue = 1,
                    solidValue = false,
                    walkableValue = true,
                    hasActionsValue = true,
                    unknownValue = false,
                    walkType = 2,
                )
            }
        }
    }
}
```

`ObjectLoader.kt` (no legacy cache usage):

```kotlin
package net.dodian.cache.`object`

class ObjectLoader {
    fun load() {
        // Transitional no-op: old cache decoding removed.
    }

    companion object {
        @JvmStatic fun `object`(x: Int, y: Int, z: Int): CacheObject? = null
        @JvmStatic fun `object`(id: Int, x: Int, y: Int, z: Int): CacheObject? = null
        @JvmStatic fun `object`(name: String, x: Int, y: Int, z: Int): CacheObject? = null
    }
}
```

`Region.kt` should expose same static calls used by code paths:

```kotlin
package net.dodian.cache.region

object Region {
    @JvmStatic fun load() { /* no-op */ }

    @JvmStatic
    fun addClippingForVariableObject(x: Int, y: Int, height: Int, type: Int, direction: Int, flag: Boolean) { }

    @JvmStatic
    fun removeClippingForVariableObject(x: Int, y: Int, height: Int, type: Int, direction: Int, flag: Boolean) { }

    @JvmStatic
    fun canMove(startX: Int, startY: Int, endX: Int, endY: Int, height: Int, xLength: Int, yLength: Int): Boolean = true
}
```

Also create minimal `GameObjectDef`, `CacheObject`, `ObjectDef`, and `ByteStream` with only fields/methods referenced by current callers.

**Step 4: Run targeted test and compile check**

Run:
- `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.cache.LegacyObjectDataShimTest"`
- `./gradlew :game-server:compileKotlin :game-server:compileJava`

Expected: PASS for shim test and compilation succeeds.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/cache game-server/src/test/kotlin/net/dodian/uber/game/systems/cache/LegacyObjectDataShimTest.kt
git commit -m "feat: add kotlin compatibility shims for removed legacy cache"
```

### Task 3: Stop Startup From Touching Removed Cache Bootstraps

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/Server.java`

**Step 1: Write failing test for startup references**

Create `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ServerLegacyCacheBootstrapBoundaryTest.kt`:

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ServerLegacyCacheBootstrapBoundaryTest {
    @Test
    fun `server bootstrap no longer calls removed cache loaders`() {
        val serverPath = Paths.get("src/main/java/net/dodian/uber/game/Server.java")
        val source = Files.readString(serverPath)
        assertFalse(source.contains("Cache.load()"))
        assertFalse(source.contains("ObjectDef.loadConfig()"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ServerLegacyCacheBootstrapBoundaryTest"`
Expected: FAIL (methods still referenced).

**Step 3: Remove old startup hooks and replace with explicit compatibility init**

In `Server.java`:

```java
// Remove:
// Cache.load();
// ObjectDef.loadConfig();
// ObjectLoader objectLoader = new ObjectLoader();
// objectLoader.load();

// Keep only compatibility-safe initialization:
Region.load();
GameObjectData.init();
```

Also remove unused imports after this change.

**Step 4: Run tests and compile**

Run:
- `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ServerLegacyCacheBootstrapBoundaryTest"`
- `./gradlew :game-server:compileJava`

Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/Server.java game-server/src/test/kotlin/net/dodian/uber/game/architecture/ServerLegacyCacheBootstrapBoundaryTest.kt
git commit -m "refactor: remove legacy cache bootstrap from server startup"
```

### Task 4: Delete Legacy Java Cache Package

**Files:**
- Delete directory: `game-server/src/main/java/net/dodian/cache/**`

**Step 1: Verify compatibility shims compile before deletion**

Run: `./gradlew :game-server:compileKotlin :game-server:compileJava`
Expected: PASS.

**Step 2: Delete legacy directory**

Run:

```bash
rm -rf game-server/src/main/java/net/dodian/cache
```

**Step 3: Run guardrail test**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.LegacyCachePackageBoundaryTest"`
Expected: PASS.

**Step 4: Run full module test**

Run: `./gradlew :game-server:test`
Expected: PASS (or only pre-existing unrelated failures).

**Step 5: Commit**

```bash
git add -A
git commit -m "chore: delete legacy java cache package"
```

### Task 5: Ensure Object/Interaction Paths Remain Safe With Stubbed Data

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketObjectService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketMagicService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/interaction/InteractionProcessor.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketObjectServiceTest.kt` (create if absent)

**Step 1: Write failing safety test**

```kotlin
@Test
fun `object handlers tolerate fallback object metadata`() {
    val fallback = GameObjectData.forId(123456)
    assertTrue(fallback.name.startsWith("Object:"))
}
```

**Step 2: Run test to verify initial failure (if no test harness yet)**

Run: `./gradlew :game-server:test --tests "*PacketObjectServiceTest*"`
Expected: FAIL or no tests found.

**Step 3: Add explicit fallback-safe guard logic where assumptions exist**

Example pattern:

```kotlin
val objectData = GameObjectData.forId(objectId)
if (objectData.name.isBlank()) {
    client.sendMessage("That object is temporarily unavailable.")
    return
}
```

Do this only where needed; avoid changing gameplay behavior elsewhere.

**Step 4: Run targeted tests**

Run: `./gradlew :game-server:test --tests "*PacketObjectServiceTest*" --tests "*Interaction*Test*"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketObjectService.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketMagicService.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/interaction/InteractionProcessor.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketObjectServiceTest.kt
git commit -m "fix: harden object interaction for cache fallback metadata"
```

### Task 6: Verify No Blocking Work Reintroduced on Tick Path

**Files:**
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/LegacyCacheTickSafetyBoundaryTest.kt`

**Step 1: Write failing boundary test for forbidden calls in shim package**

```kotlin
@Test
fun `legacy cache shim package has no blocking io or sleeps`() {
    val root = Paths.get("src/main/kotlin/net/dodian/cache")
    val forbidden = listOf("Thread.sleep(", "RandomAccessFile(", "FileInputStream(", "executeQuery(", "executeUpdate(")
    val violations = mutableListOf<String>()

    if (Files.exists(root)) {
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        if (forbidden.any { token -> line.contains(token) }) {
                            violations += "${file}:${idx + 1} -> ${line.trim()}"
                        }
                    }
                }
        }
    }

    assertTrue(violations.isEmpty(), "Shim must stay non-blocking.\n${violations.joinToString("\n")}")
}
```

**Step 2: Run test**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.LegacyCacheTickSafetyBoundaryTest"`
Expected: PASS (or FAIL if any forbidden API was used).

**Step 3: Fix violations (if any)**

Replace blocking calls with no-op/fallback behavior in shim package only.

**Step 4: Re-run test**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.LegacyCacheTickSafetyBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/LegacyCacheTickSafetyBoundaryTest.kt game-server/src/main/kotlin/net/dodian/cache
git commit -m "test: enforce non-blocking legacy cache shims"
```

### Task 7: End-to-End Verification and Runtime Smoke

**Files:**
- No code changes expected.

**Step 1: Run static verification suite**

Run:
- `./gradlew :game-server:compileKotlin :game-server:compileJava`
- `./gradlew :game-server:test`

Expected: All green except pre-existing known failures.

**Step 2: Run runtime startup smoke**

Run: `./gradlew :game-server:run`
Expected log includes server boot completion and no `ClassNotFoundException`/`NoClassDefFoundError` for `net.dodian.cache.*`.

**Step 3: Validate object-click path still executes**

Manual smoke in local environment:
- Log in with test account.
- Click at least one object that triggers `PacketObjectService`.
- Confirm no runtime exceptions in console.

**Step 4: Capture verification evidence in commit message body**

Include exact commands and PASS/FAIL counts.

**Step 5: Commit (if any tiny fixes were needed)**

```bash
git add -A
git commit -m "test: verify legacy cache removal shim cutover"
```

### Task 8: Optional Cleanup Once New Cache System Lands

**Files (future):**
- Replace all `net.dodian.cache.*` shim imports with new cache API package.
- Remove temporary shims in `game-server/src/main/kotlin/net/dodian/cache/**`.

**Step 1: Write migration checklist test**

Add boundary test that fails if any source imports `net.dodian.cache.`.

**Step 2: Run test to verify it fails now**

Run: `./gradlew :game-server:test --tests "*LegacyCachePackageBoundaryTest*"`

**Step 3: Migrate callers to new cache API**

Convert object metadata + clipping call sites to the new runtime package.

**Step 4: Delete shim package**

`rm -rf game-server/src/main/kotlin/net/dodian/cache`

**Step 5: Commit final shim removal**

```bash
git add -A
git commit -m "refactor: replace temporary cache shims with new cache runtime"
```

## Notes for Implementation
- Keep all shim methods deterministic and allocation-light in tick paths.
- Prefer `ConcurrentHashMap` + direct loops; avoid streams in frequently called APIs.
- Do not perform file I/O, SQL, or sleeping inside shim methods.
- If a call path requires unavailable metadata, return safe fallbacks instead of throwing.
