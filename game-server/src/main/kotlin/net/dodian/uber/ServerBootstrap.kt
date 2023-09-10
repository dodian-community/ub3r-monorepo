package net.dodian.uber

import com.github.michaelbull.logging.InlineLogger
import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import net.dodian.server.scripting.KotlinScriptPlugin
import net.dodian.server.scripting.ScriptPluginLoader
import net.dodian.uber.game.coroutines.GameCoroutineScope
import net.dodian.uber.game.dispatcher.io.IOCoroutineScope
import net.dodian.uber.game.dispatcher.main.MainCoroutineScope
import net.dodian.uber.game.job.GameBootTaskScheduler
import net.dodian.uber.game.process.MainGameProcess
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.game.sync.task.PlayerSynchronizationTask
import net.dodian.uber.net.startChannel
import net.dodian.uber.services.CoroutineService
import net.dodian.uber.services.GameService
import net.dodian.uber.services.LoginService
import net.dodian.uber.services.RsaService
import java.util.concurrent.Executors

private val logger = InlineLogger()

val context = ServerContext()

fun main() {
    val plugins = ScriptPluginLoader.load(KotlinScriptPlugin::class.java)
    logger.info { "Loaded ${plugins.size} plugin script${if (plugins.size == 1) "" else "s"}." }

    val playerManager = PlayerManager(context.players)

    context.registerHandler(playerManager)
    context.registerPlugins(*plugins.toTypedArray())

    context.registerServices(
        LoginService(),
        RsaService(),
        GameService()
    )

    val factory = ThreadFactoryBuilder().setDaemon(false).setNameFormat("GameExecutor").build()
    val executor = Executors.newSingleThreadExecutor(factory)

    val coroutineService = CoroutineService(
        process = MainGameProcess(
            GameCoroutineScope(),
            context.eventBus,
            context.clients,
            context.players,
            context.clock,
            playerManager,
            PlayerSynchronizationTask()
        ),
        bootTasks = GameBootTaskScheduler(IOCoroutineScope(Dispatchers.IO)),
        coroutineScope = MainCoroutineScope(executor.asCoroutineDispatcher())
    )

    context.registerService(coroutineService)
    coroutineService.startUp()

    startChannel()
}