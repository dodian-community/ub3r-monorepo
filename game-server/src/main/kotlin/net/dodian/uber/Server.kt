package net.dodian.uber

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dodian.config.InstanceConfig
import net.dodian.config.webApiConfig
import net.dodian.extensions.toFile
import net.dodian.uber.game.Server
import net.dodian.uber.game.commands.devCommands
import net.dodian.uber.game.commands.playerCommands
import net.dodian.uber.game.commands.staffCommands
import net.dodian.uber.game.libraries.commands.CommandsLibrary
import net.dodian.uber.game.libraries.commands.interfaces.ICommandsLibrary
import net.dodian.uber.web.configuration.module

val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory())
    .findAndRegisterModules()
    .registerKotlinModule()
    .enable(SerializationFeature.INDENT_OUTPUT)

lateinit var config: InstanceConfig
lateinit var commandsLibrary: ICommandsLibrary

suspend fun main(args: Array<String>) {
    config = yamlMapper.readValue<InstanceConfig>("./config.yml".toFile())

    commandsLibrary = CommandsLibrary()

    playerCommands()
    staffCommands()
    devCommands()

    withContext(Dispatchers.Default) {
        Server.startDodian()
    }

    withContext(Dispatchers.Default) {
        embeddedServer(
            Netty,
            port = webApiConfig.port,
            host = webApiConfig.hostname,
            module = Application::module
        ).start(wait = true)
    }
}