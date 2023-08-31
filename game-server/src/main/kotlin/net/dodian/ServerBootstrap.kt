package net.dodian

import com.github.michaelbull.logging.InlineLogger
import net.dodian.server.scripting.KotlinScriptPlugin
import net.dodian.server.scripting.ScriptPluginLoader
import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.services.impl.RsaService
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.net.startChannel

private val logger = InlineLogger()

val context = ServerContext()

fun main() {
    val plugins = ScriptPluginLoader.load(KotlinScriptPlugin::class.java)
    logger.info { "Loaded ${plugins.size} plugin script${if (plugins.size == 1) "" else "s"}." }

    context.registerHandler(PlayerManager())
    context.registerPlugins(*plugins.toTypedArray())

    context.registerServices(
        GameService(),
        LoginService(),
        RsaService()
    )

    startChannel()
}