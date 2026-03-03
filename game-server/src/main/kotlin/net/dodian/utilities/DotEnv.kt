package net.dodian.utilities

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()
private fun requiredEnv(key: String): String = dotenv[key]
    ?: throw IllegalStateException("Missing required environment variable: $key")

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: false
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"
val nettyLeakDetection = dotenv["NETTY_LEAK_DETECTION"] ?: "disabled"

// Database Settings
val databaseHost = requiredEnv("DATABASE_HOST")
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = requiredEnv("DATABASE_NAME")
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = requiredEnv("DATABASE_USERNAME")
val databasePassword = requiredEnv("DATABASE_PASSWORD")
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

// Async Persistence / World SQL Settings
// These are optional; existing .env files continue working with these defaults.
val asyncPlayerSaveEnabled = dotenv["ASYNC_PLAYER_SAVE_ENABLED"]?.toBoolean() ?: true
val asyncWorldDbEnabled = dotenv["ASYNC_WORLD_DB_ENABLED"]?.toBoolean() ?: true
val databaseSaveWorkers = dotenv["DATABASE_SAVE_WORKERS"]?.toInt() ?: 2
val databaseSaveRetryBaseMs = dotenv["DATABASE_SAVE_RETRY_BASE_MS"]?.toLong() ?: 250L
val databaseSaveRetryMaxMs = dotenv["DATABASE_SAVE_RETRY_MAX_MS"]?.toLong() ?: 5000L
val databaseSaveBurstAttempts = dotenv["DATABASE_SAVE_BURST_ATTEMPTS"]?.toInt() ?: 8
val accountPersistenceEnabled = dotenv["ACCOUNT_PERSISTENCE_ENABLED"]?.toBoolean() ?: true
val playerSaveShadowEnabled = dotenv["PLAYER_SAVE_SHADOW_ENABLED"]?.toBoolean() ?: false
val playerSaveBatchDelayMs = dotenv["PLAYER_SAVE_BATCH_DELAY_MS"]?.toLong() ?: 100L
val playerSaveRequestTimeoutMs = dotenv["PLAYER_SAVE_REQUEST_TIMEOUT_MS"]?.toLong() ?: 5000L
val gameLoopEnabled = dotenv["GAME_LOOP_ENABLED"]?.toBoolean() ?: true
val interactionPipelineEnabled = dotenv["INTERACTION_PIPELINE_ENABLED"]?.toBoolean() ?: false
val updatePrepEnabled = dotenv["UPDATE_PREP_ENABLED"]?.toBoolean() ?: false
val synchronizationEnabled = dotenv["SYNCHRONIZATION_ENABLED"]?.toBoolean() ?: true
val syncRootBlockCacheEnabled = dotenv["SYNC_ROOT_BLOCK_CACHE_ENABLED"]?.toBoolean() ?: true
val syncViewportSnapshotEnabled = dotenv["SYNC_VIEWPORT_SNAPSHOT_ENABLED"]?.toBoolean() ?: true
val syncSkipEmptyNpcPacketEnabled = dotenv["SYNC_SKIP_EMPTY_NPC_PACKET_ENABLED"]?.toBoolean() ?: false
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
val worldMaintenanceEnabled = dotenv["WORLD_MAINTENANCE_ENABLED"]?.toBoolean() ?: true
val worldMaintenanceVerboseMetricsEnabled = dotenv["WORLD_MAINTENANCE_VERBOSE_METRICS_ENABLED"]?.toBoolean() ?: true
val worldMaintenanceMetricsLogIntervalRuns = dotenv["WORLD_MAINTENANCE_METRICS_LOG_INTERVAL_RUNS"]?.toInt() ?: 5
val farmingSchedulerEnabled = dotenv["FARMING_SCHEDULER_ENABLED"]?.toBoolean() ?: true
val zoneUpdateBatchingEnabled = dotenv["ZONE_UPDATE_BATCHING_ENABLED"]?.toBoolean() ?: false
val queueTasksEnabled = dotenv["QUEUE_TASKS_ENABLED"]?.toBoolean() ?: false
val clientUiDeltaProcessorEnabled = dotenv["CLIENT_UI_DELTA_PROCESSOR_ENABLED"]?.toBoolean() ?: true
val databaseConnectionProxyEnabled = dotenv["DATABASE_CONNECTION_PROXY_ENABLED"]?.toBoolean() ?: false
val syncMetricsVerboseEnabled = dotenv["SYNC_METRICS_VERBOSE_ENABLED"]?.toBoolean() ?: true
val syncMetricsLogIntervalTicks = dotenv["SYNC_METRICS_LOG_INTERVAL_TICKS"]?.toInt() ?: 10
val runtimePhaseTimingEnabled = dotenv["RUNTIME_PHASE_TIMING_ENABLED"]?.toBoolean() ?: true
val runtimePhaseWarnMs = dotenv["RUNTIME_PHASE_WARN_MS"]?.toLong() ?: 25L
val runtimeCycleLogEnabled = dotenv["RUNTIME_CYCLE_LOG_ENABLED"]?.toBoolean() ?: true
val runtimeCycleLogIntervalTicks = dotenv["RUNTIME_CYCLE_LOG_INTERVAL_TICKS"]?.toInt() ?: 10

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1
