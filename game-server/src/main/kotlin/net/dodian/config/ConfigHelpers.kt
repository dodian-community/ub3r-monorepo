package net.dodian.config

import net.dodian.uber.config

val serverConfig get() = config.server
val databaseConfig get() = config.database
val groupsConfig get() = config.groups
val gameConfig get() = config.game
val webApiConfig get() = config.webApi

/**
 *  Server Shortcuts
 */
val serverName get() = serverConfig.name
val serverEnvironment get() = Environment.valueOf(serverConfig.environment)
val debugMode get() = serverConfig.debugMode
val gamePort get() = serverConfig.port

/**
 *  Game Shortcuts
 */
val forumFeaturesEnabled get() = gameConfig.forumFeaturesEnabled
val worldId get() = gameConfig.worldId
val connectionsPerIp get() = gameConfig.connectionsPerIp
val xpMultiplierGlobal get() = gameConfig.xpMultiplierGlobal
val clientVersion get() = gameConfig.clientVersion

/**
 *  Database Shortcuts
 */
val dbHost get() = databaseConfig.host
val dbPort get() = databaseConfig.port
val dbName get() = databaseConfig.name
val dbUser get() = databaseConfig.user
val dbPass get() = databaseConfig.pass
val dbTablePrefix get() = databaseConfig.tablePrefix
val initializeDatabase get() = databaseConfig.initialize