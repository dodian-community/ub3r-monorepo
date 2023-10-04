package net.dodian.utilities.cache.services

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jagex.runescape.Image24
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dodian.utilities.cache.cacheService
import net.dodian.utilities.cache.types.iftype.IfType
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs

fun Application.configureSerialization() {
    install(CORS) {
        anyHost()
    }

    install(ContentNegotiation) {
        jackson {
            findAndRegisterModules()
            registerKotlinModule()
            configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false)
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
}

data class IfTypeWithChildren(
    val parent: IfType,
    val children: List<IfTypeWithChildren>
)

fun Application.configureRoutes() {
    routing {
        get("/ping") {
            call.respondText { "pong!" }
        }

        route("/api") {
            get("/image/{name}/{id}.webp") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond("'${call.parameters["id"]}' is not a valid if ID.")

                val name = call.parameters["name"]
                    ?: return@get call.respond("No name was provided.")

                val image = Image24(cacheService.mediaArchive, name, id)

                val imageBytes = ByteArrayOutputStream()
                ImageIO.write(image.toBufferedImage(), "png", imageBytes)

                call.response.header(HttpHeaders.ContentType, "image/webp")
                call.respondBytes(imageBytes.toByteArray())
            }

            route("/types") {
                route("/obj") {
                    get {
                        paginated(cacheService.objTypes)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) call.respond("'${call.parameters["id"]}' is not a valid if ID.")

                        val objTypes = cacheService.objTypes

                        val type = objTypes.singleOrNull { it.id == id }
                            ?: return@get call.respond("No IfType found for ID '$id'.")

                        call.respond(type)
                    }
                }

                route("/ifs") {

                    get {
                        paginated(cacheService.ifTypes.filter { it.id == it.parentId || it.parentId == -1 })
                    }

                    get("/page/{page}/limit/{limit}") {
                        val page = call.parameters["page"]?.toIntOrNull() ?: 1
                        val limit = call.parameters["limit"]?.toIntOrNull() ?: 25

                        paginated(
                            cacheService.ifTypes.filter { it.id == it.parentId || it.parentId == -1 },
                            page = page,
                            limit = limit
                        )
                    }

                    get("/transparency/{minimum}") {
                        val minimum = call.parameters["minimum"]?.toIntOrNull() ?: 0

                        call.respond(cacheService.ifTypes.filter { it.transparency >= minimum })
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) call.respond("'${call.parameters["id"]}' is not a valid if ID.")

                        val ifTypes = cacheService.ifTypes

                        val type = ifTypes.singleOrNull { it.id == id }
                            ?: return@get call.respond("No IfType found for ID '$id'.")

                        val children = type.childrenFrom(ifTypes)


                        //val children = ifTypes.filter { type.childId.contains(it.id) }.map { child ->
                        //    IfTypeWithChildren(
                        //        parent = child,
                        //        children = ifTypes.filter { child.childId.contains(it.id) }
                        //            .map { IfTypeWithChildren(parent = it, children = emptyList()) })
                        //}
                        //
                        //val type2 = type.apply {
                        //    x = 0
                        //    y = 0
                        //}

                        call.respond(
                            IfTypeWithChildren(
                                parent = type,
                                children = children
                            )
                        )
                    }
                }
            }
        }
    }
}

fun IfType.childrenFrom(ifTypes: List<IfType>): List<IfTypeWithChildren> {
    val main = this

    return ifTypes.filter { this.childId.contains(it.id) }
        .map {
            it.apply {
                val index = main.childId.indexOf(it.id)
                if (index < 0) return@apply

                x = main.childX[index]
                y = main.childY[index]
            }
        }.map {
            IfTypeWithChildren(
                parent = it,
                children = it.childrenFrom(ifTypes)
            )
        }
}

suspend fun PipelineContext<Unit, ApplicationCall>.paginated(types: List<Any>, page: Int = 1, limit: Int = 25) {
    val items = types.subList((page - 1) * limit, page * limit)
    val responseBody = ResponseData(
        page = page,
        pages = abs(types.size / limit),
        total = types.size,
        onPage = items.size,
        result = items
    )

    call.respond(responseBody)
}

data class ResponseData(
    val page: Int,
    val pages: Int,
    val total: Int,
    val onPage: Int,
    val result: Any
)

fun Application.module() {
    configureSerialization()
    configureRoutes()
}

class WebApiService {

    suspend fun start(port: Int = 8080, hostname: String = "127.0.0.1") = withContext(Dispatchers.Default) {
        embeddedServer(
            Netty,
            port = port,
            host = hostname,
            module = Application::module
        ).start(wait = true)
    }
}