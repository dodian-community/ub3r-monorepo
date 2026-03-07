package net.dodian.utilities

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()
private fun requiredEnv(key: String): String =
    dotenv[key]
        ?: throw IllegalStateException("Missing required environment variable: $key")

private fun requiredNonBlankEnv(key: String): String =
    requiredEnv(key).takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Missing required environment variable: $key")

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: false
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"
val nettyLeakDetection = dotenv["NETTY_LEAK_DETECTION"] ?: "disabled"

// Database Settings
val databaseHost = requiredNonBlankEnv("DATABASE_HOST")
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = requiredNonBlankEnv("DATABASE_NAME")
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = requiredNonBlankEnv("DATABASE_USERNAME")
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: ""
val databaseInitialize = dotenv["DATABASE_INITIALIZE"]?.toBoolean() ?: false

// Game Settings - Various
val gameWorldId = dotenv["GAME_WORLD_ID"]?.toInt() ?: 1
val gameConnectionsPerIp = dotenv["GAME_CONNECTIONS_PER_IP"]?.toInt() ?: 2

// Game Settings - Client
val gameClientCustomVersion = dotenv["CLIENT_CUSTOM_VERSION"] ?: "dodian_client"

// Database Pool Settings
val databasePoolMinSize = dotenv["DATABASE_POOL_MIN_SIZE"]?.toInt() ?: 5
val databasePoolMaxSize = dotenv["DATABASE_POOL_MAX_SIZE"]?.toInt() ?: 20
val databasePoolConnectionTimeout = dotenv["DATABASE_POOL_CONNECTION_TIMEOUT"]?.toLong() ?: 30000L
val databasePoolIdleTimeout = dotenv["DATABASE_POOL_IDLE_TIMEOUT"]?.toLong() ?: 600000L
val databasePoolMaxLifetime = dotenv["DATABASE_POOL_MAX_LIFETIME"]?.toLong() ?: 1800000L

val gameLoopEnabled = dotenv["GAME_LOOP_ENABLED"]?.toBoolean() ?: true
val interactionPipelineEnabled = dotenv["INTERACTION_PIPELINE_ENABLED"]?.toBoolean() ?: false
val updatePrepEnabled = dotenv["UPDATE_PREP_ENABLED"]?.toBoolean() ?: false
val synchronizationEnabled = dotenv["SYNCHRONIZATION_ENABLED"]?.toBoolean() ?: true
val syncRootBlockCacheEnabled = dotenv["SYNC_ROOT_BLOCK_CACHE_ENABLED"]?.toBoolean() ?: true
val syncViewportSnapshotEnabled = dotenv["SYNC_VIEWPORT_SNAPSHOT_ENABLED"]?.toBoolean() ?: true
val syncSkipEmptyNpcPacketEnabled = true
val syncPlayerActivityIndexEnabled = dotenv["SYNC_PLAYER_ACTIVITY_INDEX_ENABLED"]?.toBoolean() ?: true
val syncSkipEmptyPlayerPacketEnabled = dotenv["SYNC_SKIP_EMPTY_PLAYER_PACKET_ENABLED"]?.toBoolean() ?: true
val syncPlayerTemplateCacheEnabled = dotenv["SYNC_PLAYER_TEMPLATE_CACHE_ENABLED"]?.toBoolean() ?: false
val syncScratchBufferReuseEnabled = dotenv["SYNC_SCRATCH_BUFFER_REUSE_ENABLED"]?.toBoolean() ?: true
val syncAppearanceCacheEnabled = dotenv["SYNC_APPEARANCE_CACHE_ENABLED"]?.toBoolean() ?: true
val playerSynchronizationEnabled = dotenv["PLAYER_SYNCHRONIZATION_ENABLED"]?.toBoolean() ?: true
val syncPlayerRootDiffEnabled = dotenv["SYNC_PLAYER_ROOT_DIFF_ENABLED"]?.toBoolean() ?: true
val syncPlayerSelfOnlyEnabled = dotenv["SYNC_PLAYER_SELF_ONLY_ENABLED"]?.toBoolean() ?: true
val syncPlayerIncrementalBuildEnabled = dotenv["SYNC_PLAYER_INCREMENTAL_BUILD_ENABLED"]?.toBoolean() ?: true
val syncPlayerFullRebuildFallbackEnabled = dotenv["SYNC_PLAYER_FULL_REBUILD_FALLBACK_ENABLED"]?.toBoolean() ?: true
val syncPlayerReasonMetricsEnabled = dotenv["SYNC_PLAYER_REASON_METRICS_ENABLED"]?.toBoolean() ?: true
val syncPlayerDesiredLocalsEnabled = dotenv["SYNC_PLAYER_DESIRED_LOCALS_ENABLED"]?.toBoolean() ?: true
val syncPlayerAdmissionQueueEnabled = dotenv["SYNC_PLAYER_ADMISSION_QUEUE_ENABLED"]?.toBoolean() ?: true
val syncPlayerIncrementalAddsEnabled = dotenv["SYNC_PLAYER_INCREMENTAL_ADDS_ENABLED"]?.toBoolean() ?: true
val syncPlayerMovementFragmentCacheEnabled = dotenv["SYNC_PLAYER_MOVEMENT_FRAGMENT_CACHE_ENABLED"]?.toBoolean() ?: false
val syncPlayerAllocationLightEnabled = dotenv["SYNC_PLAYER_ALLOCATION_LIGHT_ENABLED"]?.toBoolean() ?: true
val syncPlayerFragmentReuseEnabled = dotenv["SYNC_PLAYER_FRAGMENT_REUSE_ENABLED"]?.toBoolean() ?: false
val syncPlayerStateValidationEnabled = dotenv["SYNC_PLAYER_STATE_VALIDATION_ENABLED"]?.toBoolean() ?: true
val syncNpcActivityIndexEnabled = dotenv["SYNC_NPC_ACTIVITY_INDEX_ENABLED"]?.toBoolean() ?: true
val farmingSchedulerEnabled = dotenv["FARMING_SCHEDULER_ENABLED"]?.toBoolean() ?: true
val zoneUpdateBatchingEnabled = dotenv["ZONE_UPDATE_BATCHING_ENABLED"]?.toBoolean() ?: false
val queueTasksEnabled = dotenv["QUEUE_TASKS_ENABLED"]?.toBoolean() ?: false
val opcode248HasExtra14ByteSuffix = dotenv["OPCODE_248_HAS_EXTRA_14_BYTE_SUFFIX"]?.toBoolean() ?: false
val clientUiDeltaProcessorEnabled = dotenv["CLIENT_UI_DELTA_PROCESSOR_ENABLED"]?.toBoolean() ?: true
val databaseConnectionProxyEnabled = dotenv["DATABASE_CONNECTION_PROXY_ENABLED"]?.toBoolean() ?: false
val runtimePhaseTimingEnabled = dotenv["RUNTIME_PHASE_TIMING_ENABLED"]?.toBoolean() ?: true
val runtimePhaseWarnMs = dotenv["RUNTIME_PHASE_WARN_MS"]?.toLong() ?: 300L
val runtimeCycleLogEnabled = dotenv["RUNTIME_CYCLE_LOG_ENABLED"]?.toBoolean() ?: true
val clientUiTraceEnabled = dotenv["CLIENT_UI_TRACE_ENABLED"]?.toBoolean() ?: false
val clientPacketTraceEnabled = dotenv["CLIENT_PACKET_TRACE_ENABLED"]?.toBoolean() ?: false
val buttonTraceEnabled = dotenv["BUTTON_TRACE_ENABLED"]?.toBoolean() ?: false

// Inbound packet profiling (debug-only; keep disabled in production unless investigating spikes)
val inboundOpcodeProfilingEnabled = dotenv["INBOUND_OPCODE_PROFILING_ENABLED"]?.toBoolean() ?: false
val inboundOpcodeProfilingWarnMs = dotenv["INBOUND_OPCODE_PROFILING_WARN_MS"]?.toLong() ?: 2L

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1
