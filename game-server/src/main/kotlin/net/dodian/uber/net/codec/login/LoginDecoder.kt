package net.dodian.uber.net.codec.login

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import net.dodian.uber.extensions.readString
import net.dodian.uber.game.session.createPlayerCredentials
import net.dodian.uber.net.util.StatefulFrameDecoder
import net.dodian.services.impl.RsaService
import net.dodian.utilities.security.IsaacRandom
import net.dodian.utilities.security.IsaacRandomPair
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.SecureRandom

private val logger = InlineLogger()
private val random: SecureRandom = SecureRandom()

class LoginDecoder(
    private val rsaService: RsaService = RsaService()
) : StatefulFrameDecoder<LoginDecoderState>(LoginDecoderState.LOGIN_HANDSHAKE) {
    private var reconnecting: Boolean = false
    private var usernameHash: Int = -1
    private var serverSeed: Long = -1
    private var loginLength: Int = -1

    init {
        rsaService.init()
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>, state: LoginDecoderState) {
        logger.debug { "LoginDecoder: state=$state" }

        when (state) {
            LoginDecoderState.LOGIN_HANDSHAKE -> decodeHandshake(ctx, input)
            LoginDecoderState.LOGIN_HEADER -> decodeHeader(ctx, input)
            LoginDecoderState.LOGIN_PAYLOAD -> decodePayload(ctx, input, out)
        }
    }

    private fun decodeHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf) {
        if (!buffer.isReadable)
            return

        usernameHash = buffer.readUnsignedByte().toInt()
        serverSeed = random.nextLong()

        val response = ctx.alloc().buffer(17)
        response.writeByte(STATUS_EXCHANGE_DATA)
        response.writeLong(0L)
        response.writeLong(serverSeed)
        ctx.channel().write(response)

        state = LoginDecoderState.LOGIN_HEADER
    }

    private fun decodeHeader(ctx: ChannelHandlerContext, buffer: ByteBuf) {
        if (buffer.readableBytes() < 2)
            return

        val type = buffer.readUnsignedByte().toInt()

        if (type != TYPE_STANDARD && type != TYPE_RECONNECTING) {
            logger.warn { "Failed to decode login header." }
            writeResponseCode(ctx, STATUS_LOGIN_SERVER_REJECTED_SESSION)
            return
        }

        reconnecting = type == TYPE_RECONNECTING
        loginLength = buffer.readUnsignedByte().toInt()

        state = LoginDecoderState.LOGIN_PAYLOAD
    }

    private fun decodePayload(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        if (buffer.readableBytes() < loginLength || loginLength == -1)
            return

        val payload = buffer.readBytes(loginLength)
        val version = 255 - payload.readUnsignedByte()
        val release = payload.readUnsignedShort()

        val memoryStatus = payload.readUnsignedByte().toInt()
        if (memoryStatus != 0 && memoryStatus != 1) {
            logger.warn { "Login memoryStatus ($memoryStatus) not 0 or 1." }
            writeResponseCode(ctx, STATUS_LOGIN_SERVER_REJECTED_SESSION)
            return
        }

        val lowMemory = memoryStatus == 1

        // TODO: Implement CRC check?
        val crcs = IntArray(9)
        for (i in crcs.indices) {
            payload.readInt()
        }

        val length = payload.readUnsignedByte().toInt()
        if (length != loginLength - 41) {
            logger.warn { "Login packet unexpected length ($length)" }
            writeResponseCode(ctx, STATUS_LOGIN_SERVER_REJECTED_SESSION)
            return
        }

        var secure = payload.readBytes(length)

        var value = BigInteger(secure.array())
        value = value.modPow(rsaService.exponent, rsaService.modulus)
        secure = Unpooled.wrappedBuffer(value.toByteArray())

        val id = secure.readUnsignedByte().toInt()
        if (id != 10) {
            logger.warn { "Unable to read id from secure payload." }
            writeResponseCode(ctx, STATUS_LOGIN_SERVER_REJECTED_SESSION)
            return
        }

        val clientSeed = secure.readLong()
        val reportedSeed = secure.readLong()
        if (reportedSeed != serverSeed) {
            logger.warn { "Reported seed differed from server seed." }
            writeResponseCode(ctx, STATUS_LOGIN_SERVER_REJECTED_SESSION)
            return
        }

        val uid = secure.readInt()
        val username = secure.readString()
        val password = secure.readString()
        val remoteAddress = ctx.channel().remoteAddress() as InetSocketAddress
        val remoteHost = remoteAddress.hostName

        if (password.length < 6 || password.length > 20 || username.isEmpty() || username.length > 12) {
            logger.warn { "Username ('$username') or password did not pass validation." }
            writeResponseCode(ctx, STATUS_INVALID_CREDENTIALS)
            return
        }

        val seed = IntArray(4)
        seed[0] = (clientSeed shr 32).toInt()
        seed[1] = clientSeed.toInt()
        seed[2] = (serverSeed shr 32).toInt()
        seed[3] = serverSeed.toInt()

        val decodingRandom = IsaacRandom(seed)
        for (i in seed.indices)
            seed[i] += 50

        val encodingRandom = IsaacRandom(seed)

        val credentials = createPlayerCredentials(username, password, usernameHash, uid, remoteHost)
        logger.info { credentials }
        val randomPair = IsaacRandomPair(encodingRandom, decodingRandom)

        out.add(LoginRequest(credentials, randomPair, lowMemory, reconnecting, release, crcs.toList(), version))
    }

    private fun writeResponseCode(ctx: ChannelHandlerContext, response: Int) {
        val buffer = ctx.alloc().buffer(Byte.SIZE_BYTES)
        buffer.writeByte(response)
        ctx.writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE)
    }
}