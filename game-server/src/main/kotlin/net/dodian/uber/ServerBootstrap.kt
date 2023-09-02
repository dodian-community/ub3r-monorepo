package net.dodian.uber

import com.github.michaelbull.logging.InlineLogger
import net.dodian.server.scripting.KotlinScriptPlugin
import net.dodian.server.scripting.ScriptPluginLoader
import net.dodian.uber.services.LoginService
import net.dodian.uber.services.RsaService
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.net.startChannel
import net.dodian.uber.services.GameService

private val logger = InlineLogger()

val context = ServerContext()

fun main() {
    val plugins = ScriptPluginLoader.load(KotlinScriptPlugin::class.java)
    logger.info { "Loaded ${plugins.size} plugin script${if (plugins.size == 1) "" else "s"}." }

    context.registerHandler(PlayerManager())
    context.registerPlugins(*plugins.toTypedArray())

    context.registerServices(
        LoginService(),
        RsaService(),
        GameService()
    )

    startChannel()
}