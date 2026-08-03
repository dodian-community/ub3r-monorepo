plugins {
    kotlin("jvm")
    id("application")
    `java-library`
}

application {
    mainClass.set("net.dodian.uber.game.Server")
    applicationDefaultJvmArgs += arrayOf(
        "-XX:+UseZGC",
        "-XX:+ZGenerational",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseStringDeduplication",
        "-XX:AutoBoxCacheMax=65535",
        "-XX:+AlwaysPreTouch",
        "-Xms1g",
        "-Xmx4g"
    )
}

tasks.withType<Tar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Keep the server's Kotlin configuration API present and inside the main source set. A partial
// checkout can otherwise produce a long list of unrelated-looking unresolved references (for
// example metricsEnabled and gameMaxPlayers) that are easy to mistake for a Windows-only issue.
val serverConfigRelPath = "src/main/kotlin/net/dodian/uber/game/engine/config/DotEnv.kt"
val serverConfigSource = layout.projectDirectory.file(serverConfigRelPath).asFile
val repoRoot = rootProject.layout.projectDirectory.asFile
val serverConfigRepoRelPath = "game-server/$serverConfigRelPath"
val serverConfigSymbols = listOf(
    "databaseHost",
    "databasePort",
    "databaseName",
    "databaseTablePrefix",
    "databaseUsername",
    "databasePassword",
    "databaseInitialize",
    "clientVersion",
    "gameClientCustomVersion",
    "serverEnv",
    "gameWorldId",
    "discordApiEnabled",
    "discordClientId",
    "discordClientSecret",
    "discordRedirectUrl",
    "discordMaxAccountsPerDiscord",
    "rsaModulus",
    "rsaExponent",
    "serverPort",
    "serverDebugMode",
    "nettyLeakDetection",
    "dodianLogLevel",
    "webApiEnabled",
    "webApiPort",
    "webApiOpsToken",
    "webApiAllowedOrigins",
    "webApiTrustedProxyCidrs",
    "gameConnectionsPerIp",
    "databasePoolMinSize",
    "databasePoolMaxSize",
    "databasePoolConnectionTimeout",
    "databasePoolIdleTimeout",
    "databasePoolMaxLifetime",
    "runtimePhaseWarnMs",
    "gameMultiplierGlobalXp",
    "gameMaxPlayers",
    "rankBannedGroupId",
    "rankModGroupIds",
    "rankAdminGroupIds",
    "rankDebugBypassGroupIds",
    "metricsEnabled",
    "metricsPrometheusEndpoint",
    "metricsDashboardEnabled",
    "discordRequireVerifiedEmail",
)

val verifyServerConfigSource = tasks.register("verifyServerConfigSource") {
    group = "verification"
    description = "Verifies the server Kotlin configuration source and public properties."

    doLast {
        // Runs a git command against the repo root; returns null (rather than throwing) on any
        // failure so a missing git binary, a shallow clone, or a source export still falls back to
        // the plain symbol-list failure message below instead of breaking the preflight itself.
        fun git(vararg args: String): String? = try {
            val proc = ProcessBuilder(listOf("git", *args)).directory(repoRoot).start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            if (proc.waitFor() == 0) out else null
        } catch (_: Exception) {
            null
        }

        // Best-effort explanation of *why* symbols might be missing. In practice this has always
        // meant a stale local copy of this one file (see git history for verifyServerConfigSource),
        // not code that actually needs to be written - so lead with that, backed by evidence pulled
        // from git rather than asserted.
        fun diagnose(): String {
            val lines = mutableListOf<String>()

            val headText = git("show", "HEAD:$serverConfigRepoRelPath")
            if (headText != null && serverConfigSource.isFile) {
                val workingLineCount = serverConfigSource.readText().lines().size
                val headLineCount = headText.lines().size
                if (workingLineCount != headLineCount) {
                    lines += "Working copy: $workingLineCount lines. Committed at HEAD: $headLineCount lines. " +
                        "This file is very likely stale, not missing code."
                }
            }

            val lsFiles = git("ls-files", "-v", "--", serverConfigRepoRelPath)
            val flagChar = lsFiles?.trim()?.firstOrNull()
            when (flagChar) {
                'S', 's' -> lines += "git reports the skip-worktree flag is set on this file, which " +
                    "blocks it from being updated by checkout/pull. Clear it with:\n" +
                    "  git update-index --no-skip-worktree -- $serverConfigRepoRelPath"
                in 'a'..'z' -> lines += "git reports the assume-unchanged flag is set on this file, " +
                    "which blocks it from being updated by checkout/pull. Clear it with:\n" +
                    "  git update-index --no-assume-unchanged -- $serverConfigRepoRelPath"
                else -> {}
            }

            lines += "If the file is simply out of date, restore it from the current branch with:\n" +
                "  git checkout HEAD -- $serverConfigRepoRelPath"

            return lines.joinToString("\n")
        }

        val failurePrefix = """
            Server config preflight failed.
            This almost always means the local copy of DotEnv.kt is stale (an old checkout, a
            skip-worktree/assume-unchanged flag, or a partial pull), not that new Kotlin symbols
            need to be written. The .env and Settings.toml files provide runtime values; they do
            not define Kotlin symbols.
        """.trimIndent()

        check(serverConfigSource.isFile) {
            "$failurePrefix\nMissing expected source file: ${serverConfigSource.path}\n${diagnose()}"
        }

        val mainSourceFiles = sourceSets.main.get().allSource.files
            .map { it.canonicalFile }
            .toSet()
        check(serverConfigSource.canonicalFile in mainSourceFiles) {
            "$failurePrefix\nExpected source file is not included in the :game-server:main source set: ${serverConfigSource.path}\n${diagnose()}"
        }

        val sourceText = serverConfigSource.readText()
        check(Regex("(?m)^\\s*package\\s+net\\.dodian\\.uber\\.game\\.engine\\.config\\s*$")
            .containsMatchIn(sourceText)) {
            "$failurePrefix\nThe expected config package declaration is missing from: ${serverConfigSource.path}\n${diagnose()}"
        }

        val missingSymbols = serverConfigSymbols.filterNot { symbol ->
            Regex("(?m)^\\s*val\\s+$symbol\\b").containsMatchIn(sourceText)
        }
        check(missingSymbols.isEmpty()) {
            "$failurePrefix\nMissing public config properties in ${serverConfigSource.path}: ${missingSymbols.joinToString()}\n${diagnose()}"
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn(verifyServerConfigSource)
}

// Netty patches DoS/security CVEs frequently; pin one version here and force it
// below so Ktor's transitive Netty modules can't drift to a different, older
// version than the one we depend on explicitly.
val nettyVersion = "4.1.133.Final"

val syncTestSourceSet = sourceSets.create("syncTest") {
    java.srcDirs("src/syncTest/java", "src/syncTest/kotlin")
    resources.srcDir("src/syncTest/resources")
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

tasks.jar {
    dependsOn(provider { configurations.runtimeClasspath.get().buildDependencies })
    dependsOn(":routefinder:jar")
    dependsOn(":content-platform:api:jar", ":content-platform:runtime:jar")
    dependsOn(":skills:api:jar", ":skills:runtime:jar")
    dependsOn(":quests:api:jar", ":quests:runtime:jar")

    manifest {
        attributes["Main-Class"] = "net.dodian.uber.game.Server"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output

    from(contents)
}

dependencies {
    implementation(project(":routefinder"))
    implementation(project(":content-platform:runtime"))
    implementation(project(":skills:runtime"))
    implementation(project(":skills:fletching"))
    implementation(project(":skills:agility")); implementation(project(":skills:cooking")); implementation(project(":skills:crafting"))
    implementation(project(":skills:farming")); implementation(project(":skills:firemaking")); implementation(project(":skills:fishing"))
    implementation(project(":skills:herblore")); implementation(project(":skills:mining")); implementation(project(":skills:prayer"))
    implementation(project(":skills:runecrafting")); implementation(project(":skills:slayer")); implementation(project(":skills:smithing"))
    implementation(project(":skills:thieving")); implementation(project(":skills:woodcutting")); implementation(project(":skills:skillguide"))
    implementation(project(":quests:runtime"))
    implementation(project(":quests:tutorial-island"))
    implementation(project(":social:runtime"))
    implementation(project(":social:trading"))
    implementation(project(":social:dueling"))
    implementation(project(":economy:runtime"))
    implementation(project(":economy:shops"))
    implementation(project(":economy:price-checker"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.jctools:jctools-core:4.0.5")

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.0")
    implementation("org.apache.commons:commons-compress:1.21")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.mybatis:mybatis:3.5.10")

    implementation("io.prometheus:simpleclient:0.16.0")
    implementation("io.prometheus:simpleclient_common:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")

    implementation("com.google.code.gson:gson:2.7")

    implementation("io.netty:netty-all:$nettyVersion")
    implementation("io.netty:netty-transport-native-epoll:$nettyVersion:linux-x86_64")
    implementation("io.netty:netty-transport-native-epoll:$nettyVersion:linux-aarch_64")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("com.displee:rs-cache-library:8.1.0")
    implementation("com.displee:disio:2.3")
    implementation("it.unimi.dsi:fastutil:8.5.18")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("com.h2database:h2:2.2.224")

    val ktor_version = "2.3.12"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-rate-limit:$ktor_version")

    implementation(kotlin("reflect"))
}

// Ktor's ktor-server-netty pulls its own transitive Netty modules, which can end
// up on a different (older) patch version than the explicit netty-all dependency
// above. Force everything under io.netty to the same pinned version so there's
// a single, current Netty on the runtime classpath.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty" && requested.name.startsWith("netty-")) {
            useVersion(nettyVersion)
            because("keep a single patched Netty version on the classpath (see CVE-2025-58057, CVE-2025-55163, CVE-2025-24970, CVE-2024-47535)")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("realWorldSkillTest") {
    description = "Runs real-world (real cache/game-loop) skill integration tests; needs data/cache locally, no-ops in CI"
    group = "verification"
    useJUnitPlatform { includeTags("real-world") }
}

configurations[syncTestSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[syncTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
tasks.register<Test>("syncTest") {
    description = "Runs focused synchronization and transport verification tests"
    group = "verification"
    testClassesDirs = syncTestSourceSet.output.classesDirs
    classpath = syncTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.register("betaCheck") {
    group = "verification"
    description = "Comprehensive beta-readiness gate: client, server, sync, content platform, skills, and quests"

    dependsOn(":skillsCheck")
    dependsOn(":questsCheck")
    dependsOn(":socialCheck")
    dependsOn(":content-platform:api:check")
    dependsOn(":content-platform:runtime:check")
    dependsOn(":content-platform:testkit:check")
    dependsOn(":game-client:test")
    dependsOn(":game-server:check")
    dependsOn(":game-server:syncTest")

    doLast {
        println("Beta readiness gate passed: client, server, sync, content-platform, skill, quest, and social-module tests completed.")
    }
}

tasks.register<JavaExec>("runSyncBenchmark") {
    group = "verification"
    description = "Run the synchronization pipeline benchmark harness"
    classpath = syncTestSourceSet.runtimeClasspath
    mainClass.set("net.dodian.uber.game.runtime.sync.SyncPipelineBenchmark")
}

tasks.register<JavaExec>("runCollisionMatrixBenchmark") {
    group = "verification"
    description = "Compares dense and paged collision lookup throughput and retained payload size"
    classpath = syncTestSourceSet.runtimeClasspath
    mainClass.set("net.dodian.uber.game.runtime.sync.CollisionMatrixBenchmark")
}

tasks.register<JavaExec>("runCollisionCacheMemoryProbe") {
    group = "verification"
    description = "Loads real cache collision data, enforces the memory budget, and optionally dumps heap"
    classpath = syncTestSourceSet.runtimeClasspath
    mainClass.set("net.dodian.uber.game.runtime.sync.CollisionCacheMemoryProbe")
}

tasks.register<JavaExec>("dumpObjectDefs") {
    group = "tools"
    description = "Dump all object definitions from cache to JSON"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.ObjectDefinitionDumper")
    args = listOf("data/cache")
}

tasks.register<JavaExec>("dumpNpcDefs") {
    group = "tools"
    description = "Dump all NPC definitions from cache to JSON"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.NpcDefinitionDumper")
    args = listOf("data/cache")
}

tasks.register<JavaExec>("dumpSpotAnimDefs") {
    group = "tools"
    description = "Dump all spot animation (graphic) definitions from cache to JSON, optionally enriched with rsmod names"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.SpotAnimDefinitionDumper")
    args = listOf(
        "data/cache",
        "/Users/tylercovalt/Desktop/rsmod-main/.data/symbols/spotanim.sym",
    )
}

tasks.register<JavaExec>("dumpInterfaceDefs") {
    group = "tools"
    description = "Dump all interface/component definitions from cache to JSON"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.InterfaceDefinitionDumper")
    args = listOf("data/cache")
}

tasks.register<JavaExec>("runSkillTargetAudit") {
    group = "tools"
    description = "Audit active skill object/NPC bindings against committed rev218 mappings and an optional Tarnish root"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.audit.SkillTargetCompatibilityAuditMain")
    val tarnishRoot = providers.gradleProperty("tarnishRoot")
    args = tarnishRoot.map { listOf(it) }.getOrElse(emptyList())
}

tasks.register<JavaExec>("generateRscm") {
    group = "tools"
    description = "Generate developer-reference mappings from a pinned OSRS dump"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.RscmGenerator")
    val dumpPath = providers.gradleProperty("osrsDump")
    doFirst {
        require(dumpPath.isPresent) { "Pass -PosrsDump=/absolute/path/to/osrs-dump" }
    }
    args("--dump", dumpPath.getOrElse(""), "--output", "reference/cache-rev218", "--cache", "data/cache")
}

tasks.register<JavaExec>("checkRscm") {
    group = "verification"
    description = "Verify committed developer-reference mappings match the pinned OSRS dump"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.engine.systems.cache.RscmGenerator")
    val dumpPath = providers.gradleProperty("osrsDump")
    doFirst {
        require(dumpPath.isPresent) { "Pass -PosrsDump=/absolute/path/to/osrs-dump" }
    }
    args("--dump", dumpPath.getOrElse(""), "--output", "reference/cache-rev218", "--cache", "data/cache", "--check")
}
