package net.dodian.uber.net.update

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import net.dodian.uber.net.update.resource.CombinedResourceProvider
import net.dodian.uber.net.update.resource.HyperTextResourceProvider
import net.dodian.uber.net.update.resource.ResourceProvider
import net.dodian.uber.net.update.resource.VirtualResourceProvider
import org.apollo.cache.IndexedFileSystem
import java.nio.charset.Charset
import java.util.*
import kotlin.io.path.Path

private const val SERVER_IDENTIFIER = "JAGeX/3.1"
private val CHARACTER_SET = Charsets.ISO_8859_1
private val WWW_DIRECTORY = Path("./data/www")

class HttpRequestWorker(
    dispatcher: UpdateDispatcher,
    fs: IndexedFileSystem
) : RequestWorker<HttpRequest, ResourceProvider>(
    dispatcher,
    CombinedResourceProvider(VirtualResourceProvider(fs), HyperTextResourceProvider(WWW_DIRECTORY))
) {

    override fun nextRequest(dispatcher: UpdateDispatcher) = dispatcher.nextHttpRequest

    override fun service(provider: ResourceProvider, channel: Channel, request: HttpRequest): Boolean {
        val path = request.uri()
        val buf = provider.get(path)

        var status = HttpResponseStatus.OK
        var mime = getMimeType(request.uri())

        if (buf == null) {
            status = HttpResponseStatus.NOT_FOUND
            mime = "text/html"
        }

        val wrapped = if (buf != null) {
            Unpooled.wrappedBuffer(buf)
        } else createErrorPage(status, "The page you requested could not be found.")

        val response = DefaultHttpResponse(request.protocolVersion(), status)

        response.headers().set("Date", Date())
        response.headers()["Server"] = SERVER_IDENTIFIER
        response.headers()["Content-type"] = mime + ", charset=" + CHARACTER_SET.name()
        response.headers()["Cache-control"] = "no-cache"
        response.headers()["Pragma"] = "no-cache"
        response.headers()["Expires"] = Date(0)
        response.headers()["Connection"] = "close"
        response.headers()["Content-length"] = wrapped.readableBytes()

        channel.write(response)
        channel.writeAndFlush(wrapped).addListener(ChannelFutureListener.CLOSE)

        return true
    }

    companion object {

        private fun getMimeType(name: String): String {
            var newName = name
            if (newName.endsWith("/"))
                newName += "index.html"

            return when {
                newName.endsWith(".html") || newName.endsWith(".html") -> "text/html"
                newName.endsWith(".jpg") || newName.endsWith(".jpeg") -> "image/jpeg"
                newName.endsWith(".png") -> "image/png"
                newName.endsWith(".gif") -> "image/gif"
                newName.endsWith(".css") -> "text/css"
                newName.endsWith(".js") -> "text/javascript"
                newName.endsWith(".txt") -> "text/plain"
                else -> "application/octet-stream"
            }
        }

        private fun createErrorPage(status: HttpResponseStatus, description: String): ByteBuf {
            val title = "${status.code()} ${status.reasonPhrase()}"
            val builder = StringBuilder("<!DOCTYPE html><html><head>")
            builder.append("<title>$title</title>")
            builder.append("</head><body>")

            builder.append("<h1>$title</h1>")
            builder.append("<p>$description</p>")
            builder.append("<hr /><address>$SERVER_IDENTIFIER</address> Server")
            builder.append("</body></html>")

            return Unpooled.copiedBuffer(builder.toString(), Charset.defaultCharset())
        }
    }
}