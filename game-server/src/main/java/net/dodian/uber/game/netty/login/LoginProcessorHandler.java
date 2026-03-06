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
import net.dodian.uber.game.runtime.loop.GameThreadTaskQueue;
import net.dodian.uber.game.runtime.loop.GameThreadIngress;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processes the full second-stage login payload and, on success, creates the
 * {@link Client} instance then swaps the pipeline to in-game handlers.
 */
public class LoginProcessorHandler extends SimpleChannelInboundHandler<LoginPayload> {

    private static final Logger logger = LoggerFactory.getLogger(LoginProcessorHandler.class);
    private static final AtomicLong LOGIN_SLOT_FAILURES = new AtomicLong();
    private static final AtomicLong LOGIN_LOAD_FAILURES = new AtomicLong();
    private static final AtomicLong LOGIN_CHANNEL_CLOSES_BEFORE_FINALIZE = new AtomicLong();
    private static final AtomicLong LOGIN_INITIALIZER_FAILURES = new AtomicLong();

    private static final int LOGIN_SUCCESS_CODE = 2;
    private static final int RSA_MAGIC          = 255;
    private static final int CLIENT_VERSION     = 317;
    private static final int RSA_PACKET_ID      = 10;

    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY  = AttributeKey.valueOf("inCipher");
    private static final AttributeKey<ISAACCipher> OUT_CIPHER_KEY = AttributeKey.valueOf("outCipher");

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
        final long acceptedAtNanos = System.nanoTime();
        PlayerHandler ph = playerHandler != null ? playerHandler : Server.playerHandler;

        if (PlayerHandler.isPlayerOn(username)) {
            sendAndClose(ctx, 5); // already online
            return;
        }

        final long slotReserveStart = System.nanoTime();
        reservedSlot = reserveSlot();
        if (reservedSlot == -1) {
            long failures = LOGIN_SLOT_FAILURES.incrementAndGet();
            logger.warn("Login slot reservation failed for {} failures={}", username, failures);
            sendAndClose(ctx, 7); // world full
            return;
        }
        final long slotReserveDurationMs = (System.nanoTime() - slotReserveStart) / 1_000_000L;

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
        // Canonical name hash used across online maps/friends.
        client.longName  = Utils.playerNameToLong(client.getPlayerName());
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
        AccountPersistenceService.submitLoginLoad(client, username, password, loadResult ->
                ctx.channel().eventLoop().execute(() -> finishLogin(ctx, client, loadResult, slotCopy, acceptedAtNanos, slotReserveDurationMs)));
        return;
    }

        /**
     * Completes login after the blocking account load has finished.
     * Runs on the Netty event loop thread.
     */
    private void finishLogin(
            ChannelHandlerContext ctx,
            Client client,
            AccountPersistenceService.LoginLoadResult loadResult,
            int slot,
            long acceptedAtNanos,
            long slotReserveDurationMs
    ) {
        if (loadResult.getCode() != 0) {
            long failures = LOGIN_LOAD_FAILURES.incrementAndGet();
            logger.warn(
                    "Login load failed for {} code={} load={}ms pendingRetries={} failures={}",
                    client.getPlayerName(),
                    loadResult.getCode(),
                    loadResult.getDurationMs(),
                    loadResult.getPendingRetries(),
                    failures
            );
            releaseSlot(slot);
            sendAndClose(ctx, loadResult.getCode());
            return;
        }

        client.validLogin = true;
        client.playerRights = (client.playerGroup == 9 || client.playerGroup == 5) ? 1 :
                              ((client.playerGroup == 6 || client.playerGroup == 18 || client.playerGroup == 10) ? 2 : 0);
        client.premium = client.playerRights > 0 || client.premium;

        sendLoginSuccess(ctx, client.playerRights);

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

        // Finish registration + initialization on the game thread. This avoids cross-thread mutation
        // of PlayerHandler players[]/playersOnline and reduces login-related sync spikes.
        final io.netty.channel.Channel channel = ctx.channel();
        final int slotCopy = slot;
        final long finalizerQueuedAtNanos = System.nanoTime();
        GameThreadIngress.submitCritical("login-finalize", () -> {
            long finalizerStartedAtNanos = System.nanoTime();
            long queueWaitMs = (finalizerStartedAtNanos - finalizerQueuedAtNanos) / 1_000_000L;
            if (!channel.isActive() || client.disconnected) {
                long failures = LOGIN_CHANNEL_CLOSES_BEFORE_FINALIZE.incrementAndGet();
                // Channel died before the game thread could register the player; release the reserved slot.
                synchronized (PlayerHandler.SLOT_LOCK) {
                    PlayerHandler.usedSlots.clear(slotCopy);
                    PlayerHandler.players[slotCopy] = null;
                }
                logger.warn(
                        "Login channel closed before game-thread finalization for {} queueWait={}ms failures={}",
                        client.getPlayerName(),
                        queueWaitMs,
                        failures
                );
                return;
            }

            PlayerHandler.players[slotCopy] = client;
            PlayerHandler.playersOnline.put(client.longName, client);

            long initializerDurationMs = 0L;
            try {
                long initializerStartNanos = System.nanoTime();
                PlayerInitializer initializer = new PlayerInitializer();
                initializer.initializeCriticalLoginState(client);
                initializerDurationMs = (System.nanoTime() - initializerStartNanos) / 1_000_000L;
                client.initialized = true;

                client.isActive = true;
                if (client.getUpdateFlags() != null) {
                    client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                }
                client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));

                final PlayerInitializer postInitializer = initializer;
                GameThreadIngress.submitDeferred("login-post-init", () -> {
                    if (!client.disconnected) {
                        postInitializer.initializeDeferredPostLoginState(client);
                    }
                });
            } catch (Exception ex) {
                long failures = LOGIN_INITIALIZER_FAILURES.incrementAndGet();
                logger.warn(
                        "[GameThread] PlayerInitializer error for {} failures={}",
                        client.getPlayerName(),
                        failures,
                        ex
                );
            }

        });

        loginFinished = true;
        logger.info(
                "[Netty] Login finished for {} slot {} (async) load={}ms pendingRetries={}",
                client.getPlayerName(),
                slot,
                loadResult.getDurationMs(),
                loadResult.getPendingRetries()
        );
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
        ByteBuf resp = ctx.alloc().buffer(2);
        resp.writeByte(LOGIN_SUCCESS_CODE);
        resp.writeByte(rights);
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
