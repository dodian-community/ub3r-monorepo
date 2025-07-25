package net.dodian.uber.game.netty.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import net.dodian.utilities.Utils;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.PlayerInitializer;
import net.dodian.uber.game.netty.codec.ByteMessageEncoder;
import net.dodian.uber.game.netty.game.GamePacketDecoder;
import net.dodian.uber.game.netty.game.GamePacketEncoder;
import net.dodian.uber.game.netty.game.GamePacketHandler;
import net.dodian.uber.game.netty.util.ConnectionLoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Processes the full second-stage login payload and, on success, creates the
 * {@link Client} instance then swaps the pipeline to in-game handlers.
 */
public class LoginProcessorHandler extends SimpleChannelInboundHandler<LoginPayload> {

    private static final Logger logger = LoggerFactory.getLogger(LoginProcessorHandler.class);

    private static final int LOGIN_SUCCESS_CODE = 2;
    private static final int RSA_MAGIC          = 255;
    private static final int CLIENT_VERSION     = 317;
    private static final int RSA_PACKET_ID      = 10;

    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY  = AttributeKey.valueOf("inCipher");
    private static final AttributeKey<ISAACCipher> OUT_CIPHER_KEY = AttributeKey.valueOf("outCipher");

    // Dedicated thread pool for blocking login operations
    private static final ExecutorService LOGIN_EXECUTOR =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    private final PlayerHandler playerHandler;

    private long   clientSessionKey;
    private long   serverSessionKey;
    private String username;
    private String password;

    private int  reservedSlot = -1;
    private boolean loginFinished = false;

    public LoginProcessorHandler(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginPayload payloadHolder) {
        ByteBuf in = payloadHolder.getPayload();
        try {
            if (!parseLogin(ctx, in)) {
                return; // parseLogin already handled failure
            }
            processLogin(ctx);
        } finally {
            if (in.refCnt() > 0) in.release();
        }
    }

    /* --------------- Parsing --------------- */
    private boolean parseLogin(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 3) return false;

        int magic = buf.readUnsignedByte();
        if (magic != RSA_MAGIC) {
            logger.debug("[Netty] Bad RSA magic {}", magic);
            ctx.close();
            return false;
        }
        int version = buf.readUnsignedShort();
        if (version != CLIENT_VERSION) {
            logger.debug("[Netty] Unsupported client version {}", version);
            ctx.close();
            return false;
        }
        if (buf.readableBytes() < 1) return false;
        buf.readByte(); // lowMem flag
        if (buf.readableBytes() < 9 * 4 + 2) return false;
        buf.skipBytes(9 * 4); // CRC keys
        int rsaLength   = buf.readUnsignedByte();
        int rsaPacketId = buf.readUnsignedByte();
        if (rsaPacketId != RSA_PACKET_ID) {
            ctx.close();
            return false;
        }
        if (buf.readableBytes() < rsaLength - 1) return false;

        clientSessionKey = buf.readLong();
        serverSessionKey = buf.readLong();
        readString(buf); // optional server string, ignored
        username = readString(buf);
        password = readString(buf);

        logger.debug("[Netty] Login attempt {} from {}", username, ctx.channel().remoteAddress());
        return true;
    }

    private static String readString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 10) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    /* --------------- Login logic --------------- */
    private void processLogin(ChannelHandlerContext ctx) {
        PlayerHandler ph = playerHandler != null ? playerHandler : Server.playerHandler;

        if (PlayerHandler.isPlayerOn(username)) {
            sendAndClose(ctx, 5); // already online
            return;
        }

        reservedSlot = reserveSlot();
        if (reservedSlot == -1) {
            sendAndClose(ctx, 7); // world full
            return;
        }

        // Configure ISAAC
        int[] seed = new int[]{
                (int) (clientSessionKey >>> 32),
                (int) clientSessionKey,
                (int) (serverSessionKey >>> 32),
                (int) serverSessionKey
        };
        ISAACCipher inCipher = new ISAACCipher(seed);
        for (int i = 0; i < 4; i++) seed[i] += 50;
        ISAACCipher outCipher = new ISAACCipher(seed);
        ctx.channel().attr(IN_CIPHER_KEY).set(inCipher);
        ctx.channel().attr(OUT_CIPHER_KEY).set(outCipher);

        // Instantiate Client
        Client client;
        try {
            client = new Client(ctx.channel(), reservedSlot);
        } catch (Exception ex) {
            logger.error("[Netty] Failed to create Client: {}", ex.getMessage());
            releaseSlot(reservedSlot);
            sendAndClose(ctx, 13);
            return;
        }
        client.handler = ph;
        client.setPlayerName(Utils.capitalize(username.replace('_', ' ')));
        client.playerPass = password;
        client.longName  = Utils.playerNameToInt64(client.getPlayerName());
        try {
            InetSocketAddress isa = (InetSocketAddress) ctx.channel().remoteAddress();
            client.connectedFrom = isa.getAddress().getHostAddress();
        } catch (Exception ignored) {}

        //client.inStreamDecryption  = inCipher;
       // client.outStreamDecryption = outCipher;
        // Legacy Stream encryption setup - no longer needed with pure Netty
        // if (client.getOutputStream() != null) client.getOutputStream().packetEncryption = outCipher;
        // if (client.getInputStream()  != null) client.getInputStream().packetEncryption  = inCipher;

        final int slotCopy = reservedSlot;
        LOGIN_EXECUTOR.submit(() -> {
            int loadResult;
            try {
                loadResult = Server.loginManager.loadgame(client, username, password);
            } catch (Exception ex) {
                logger.warn("[Netty] loadgame exception for {}: {}", username, ex.getMessage(), ex);
                loadResult = 13; // generic error
            }
            final int lr = loadResult;
            // Ensure completion logic runs back on the Netty event loop thread
            ctx.channel().eventLoop().execute(() -> finishLogin(ctx, client, lr, slotCopy));
        });
        return;
    }

        /**
     * Completes login after the blocking account load has finished.
     * Runs on the Netty event loop thread.
     */
    private void finishLogin(ChannelHandlerContext ctx, Client client, int loadResult, int slot) {
        if (loadResult != 0) {
            releaseSlot(slot);
            sendAndClose(ctx, loadResult);
            return;
        }

        client.validLogin = true;
        client.playerRights = (client.playerGroup == 9 || client.playerGroup == 5) ? 1 :
                              ((client.playerGroup == 6 || client.playerGroup == 18) ? 2 : 0);
        client.premium = client.playerRights > 0 || client.premium;

        sendLoginSuccess(ctx, client.playerRights);

        PlayerHandler.players[slot] = client;
        PlayerHandler.playersOnline.put(Utils.playerNameToLong(client.getPlayerName()), client);

        // CRITICAL: Setup game pipeline BEFORE PlayerInitializer sends packets
        // Remove handshake & login-specific decoders first
        if (ctx.pipeline().get(net.dodian.uber.game.netty.login.LoginPayloadDecoder.class) != null) {
            ctx.pipeline().remove(net.dodian.uber.game.netty.login.LoginPayloadDecoder.class);
        }
        if (ctx.pipeline().get(net.dodian.uber.game.netty.login.LoginHandshakeHandler.class) != null) {
            ctx.pipeline().remove(net.dodian.uber.game.netty.login.LoginHandshakeHandler.class);
        }
        // Swap pipeline to game mode
        if (ctx.pipeline().get(ConnectionLoggingHandler.class) != null) {
            ctx.pipeline().remove(ConnectionLoggingHandler.class);
        }
        ctx.pipeline().addLast(new GamePacketDecoder());
        ctx.pipeline().addLast(new ByteMessageEncoder());
        // ctx.pipeline().addLast(new GamePacketEncoder()); // Removed - using pure ByteMessage/Netty
        ctx.pipeline().addLast(new GamePacketHandler(client));
        ctx.pipeline().remove(this);

        // Store reference for disconnect cleanup
        ctx.channel().attr(AttributeKey.valueOf("activeClient")).set(client);

        // NOW initialize player - packets will go through proper game pipeline
        try {
            new PlayerInitializer().initializePlayer(client);
            client.initialized = true;
        } catch (Exception ex) {
            logger.warn("[Netty] PlayerInitializer error for {}: {}", client.getPlayerName(), ex.getMessage());
        }
        client.isActive = true;
        if (client.getUpdateFlags() != null) {
            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }
        // client.flushOutStream(); // No longer needed with pure Netty

        client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));

        loginFinished = true;
        logger.info("[Netty] Login finished for {} slot {} (async)", client.getPlayerName(), slot);
    }

    /* Slot helpers */
    private int reserveSlot() {
        synchronized (PlayerHandler.SLOT_LOCK) {
            for (int i = 1; i <= Constants.maxPlayers; i++) {
                if (!PlayerHandler.usedSlots.get(i)) {
                    PlayerHandler.usedSlots.set(i);
                    return i;
                }
            }
        }
        return -1;
    }

    private void releaseSlot(int slot) {
        if (slot <= 0) return;
        synchronized (PlayerHandler.SLOT_LOCK) {
            PlayerHandler.usedSlots.clear(slot);
            PlayerHandler.players[slot] = null;
        }
    }

    private void sendLoginSuccess(ChannelHandlerContext ctx, int rights) {
        ByteBuf resp = ctx.alloc().buffer(3);
        resp.writeByte(LOGIN_SUCCESS_CODE);
        resp.writeByte(rights);
        resp.writeByte(0);
        ctx.writeAndFlush(resp);
    }

    private void sendAndClose(ChannelHandlerContext ctx, int code) {
        ByteBuf resp = ctx.alloc().buffer(3);
        resp.writeByte(code);
        resp.writeByte(0);
        resp.writeByte(0);
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (!loginFinished && reservedSlot > 0) {
            releaseSlot(reservedSlot);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] Login processing error for {}", ctx.channel().remoteAddress(), cause);
        if (!loginFinished && reservedSlot > 0) {
            releaseSlot(reservedSlot);
        }
        ctx.close();
    }
}
