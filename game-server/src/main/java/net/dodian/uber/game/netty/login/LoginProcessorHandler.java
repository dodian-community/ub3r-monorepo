package net.dodian.uber.game.netty.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import net.dodian.utilities.Utils;
import net.dodian.uber.game.engine.config.DotEnvKt;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.model.entity.player.PlayerInitializer;
import net.dodian.uber.game.engine.metrics.OperationalTelemetry;
import net.dodian.uber.game.netty.codec.ByteMessageEncoder;
import net.dodian.uber.game.netty.game.GamePacketDecoder;
import net.dodian.uber.game.netty.game.GamePacketHandler;
import net.dodian.uber.game.netty.util.ConnectionLoggingHandler;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.engine.loop.GameThreadIngress;
import net.dodian.uber.game.events.player.PlayerLoginEvent;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.account.login.AccountLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final ConcurrentHashMap<Long, Object> LOADING_ACCOUNTS = new ConcurrentHashMap<>();
    private static final LoginAttemptLimiter LOGIN_ATTEMPTS = new LoginAttemptLimiter();

    private static final int LOGIN_SUCCESS_CODE = 2;
    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY  = AttributeKey.valueOf("inCipher");
    private static final AttributeKey<ISAACCipher> OUT_CIPHER_KEY = AttributeKey.valueOf("outCipher");

    private volatile LoginAttempt attempt;

    public LoginProcessorHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginPayload payloadHolder) {
        ByteBuf in = payloadHolder.payload();
        try {
            String remoteIp = remoteIp(ctx);
            LoginAttemptLimiter.Decision preflight = LOGIN_ATTEMPTS.preflight(remoteIp, System.currentTimeMillis());
            if (!preflight.isAllowed()) {
                logger.debug("[Netty] Login throttled from {}", remoteIp);
                sendAndClose(ctx, preflight.getResponseCode());
                return;
            }
            if (attempt != null) {
                logger.warn("Login connection sent more than one payload remote={}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            Long expectedSeed = ctx.channel().attr(LoginHandshakeHandler.SERVER_SEED_KEY).get();
            if (expectedSeed == null) {
                logger.warn("Login payload arrived without a server seed remote={}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            byte[] payload = new byte[in.readableBytes()];
            in.readBytes(payload);
            LoginAttempt submitted = new LoginAttempt(
                    ctx.channel(), remoteIp, expectedSeed, payload, payloadHolder.reconnecting());
            attempt = submitted;
            boolean accepted = LoginPreparationService.submit(submitted, result -> {
                try {
                    ctx.channel().eventLoop().execute(() -> preparedLogin(ctx, submitted, result));
                } catch (java.util.concurrent.RejectedExecutionException rejected) {
                    submitted.setStage(LoginAttempt.Stage.FAILED);
                    releaseAttempt(submitted);
                }
            });
            if (!accepted) {
                submitted.setStage(LoginAttempt.Stage.FAILED);
                logger.warn("Login preparation queue full remote={}; rejecting attempt", ctx.channel().remoteAddress());
                sendAndClose(ctx, 13);
            }
        } finally {
            if (in.refCnt() > 0) in.release();
        }
    }

    private void preparedLogin(
            ChannelHandlerContext ctx,
            LoginAttempt current,
            LoginPreparationService.Result result
    ) {
        if (attempt != current || !ctx.channel().isActive()) {
            releaseAttempt(current);
            return;
        }
        if (!result.isSuccess()) {
            current.setStage(LoginAttempt.Stage.FAILED);
            OperationalTelemetry.incrementCounter("login.prepare.failure", 1L);
            logger.warn("Login preparation failed remote={} code={} reason={}",
                    ctx.channel().remoteAddress(), result.getResponseCode(), result.getReason());
            sendAndClose(ctx, result.getResponseCode());
            return;
        }
        processLogin(ctx, current, result.getRequest());
    }

    /* --------------- Login logic --------------- */
    private void processLogin(ChannelHandlerContext ctx, LoginAttempt current, ParsedLoginRequest parsed) {
        String username = parsed.getUsername();
        String password = parsed.getPassword();
        LoginAttemptLimiter.Decision blocked = LOGIN_ATTEMPTS.checkBlocked(
                current.getRemoteIp(), username, parsed.getDiscordAuthCode(), System.currentTimeMillis());
        if (!blocked.isAllowed()) {
            OperationalTelemetry.incrementCounter("login.auth.blocked", 1L);
            sendAndClose(ctx, blocked.getResponseCode());
            return;
        }
        long longName = Utils.playerNameToLong(Utils.capitalize(username.replace('_', ' ')));
        current.longName = longName;
        if (LOADING_ACCOUNTS.putIfAbsent(longName, current) != null) {
            sendAndClose(ctx, 5); // login in progress or already online
            return;
        }
        if (PlayerRegistry.isPlayerOn(username)) {
            releaseToken(current);
            sendAndClose(ctx, 5); // already online
            return;
        }

        final long slotReserveStart = System.nanoTime();
        int reservedSlot = reserveSlot();
        current.setReservedSlot(reservedSlot);
        if (reservedSlot == -1) {
            long failures = LOGIN_SLOT_FAILURES.incrementAndGet();
            logger.warn("Login slot reservation failed for {} failures={}", username, failures);
            releaseToken(current);
            sendAndClose(ctx, 7); // world full
            return;
        }
        final long slotReserveDurationMs = (System.nanoTime() - slotReserveStart) / 1_000_000L;

        ctx.channel().attr(IN_CIPHER_KEY).set(parsed.getInboundCipher());
        ctx.channel().attr(OUT_CIPHER_KEY).set(parsed.getOutboundCipher());

        // Instantiate Client
        Client client;
        try {
            client = new Client(ctx.channel(), reservedSlot);
        } catch (Exception ex) {
            logger.error("[Netty] Failed to create Client: {}", ex.getMessage());
            releaseSlot(current);
            releaseToken(current);
            sendAndClose(ctx, 13);
            return;
        }
        client.setPlayerName(Utils.capitalize(username.replace('_', ' ')));
        client.playerPass = password;
        client.longName  = longName;
        current.client = client;
        try {
            InetSocketAddress isa = (InetSocketAddress) ctx.channel().remoteAddress();
            client.connectedFrom = isa.getAddress().getHostAddress();
        } catch (Exception ignored) {}

        current.setStage(LoginAttempt.Stage.ACCOUNT_LOADING);
        final int slotCopy = reservedSlot;
        AccountPersistenceService.submitLoginLoad(
                client, username, password, parsed.getDiscordAuthCode(), parsed.isAccountCreationRequested(), loadResult ->
                ctx.channel().eventLoop().execute(() -> finishLogin(
                        ctx, current, parsed, client, loadResult, slotCopy, slotReserveDurationMs)));
    }

        /**
     * Completes login after the blocking account load has finished.
     * Runs on the Netty event loop thread.
     */
    private void finishLogin(
            ChannelHandlerContext ctx,
            LoginAttempt current,
            ParsedLoginRequest parsed,
            Client client,
            AccountPersistenceService.LoginLoadResult loadResult,
            int slot,
            long slotReserveDurationMs
    ) {
        if (loadResult.getCode() != 0) {
            recordAuthenticationFailure(parsed, current.getRemoteIp(), loadResult.getCode());
            long failures = LOGIN_LOAD_FAILURES.incrementAndGet();
            OperationalTelemetry.incrementCounter("login.load.failure", 1L);
            if (isExpectedAuthenticationRejection(loadResult.getCode())) {
                logger.debug(
                        "Login rejected for {} reason=\"{}\" code={} load={}ms pendingRetries={}",
                        client.getPlayerName(),
                        describeReason(loadResult.getCode()),
                        loadResult.getCode(),
                        loadResult.getDurationMs(),
                        loadResult.getPendingRetries()
                );
            } else {
                logger.warn(
                        "Login load failed for {} reason=\"{}\" code={} load={}ms pendingRetries={} failures={}",
                        client.getPlayerName(),
                        describeReason(loadResult.getCode()),
                        loadResult.getCode(),
                        loadResult.getDurationMs(),
                        loadResult.getPendingRetries(),
                        failures
                );
            }
            releaseSlot(current);
            releaseToken(current);
            sendAndClose(ctx, loadResult.getCode());
            return;
        }
        AccountLoginService.PreparedLogin hydration = loadResult.getHydration();
        if (hydration == null) {
            logger.warn("Successful login load for {} had no hydration snapshot", client.getPlayerName());
            releaseSlot(current);
            releaseToken(current);
            sendAndClose(ctx, 13);
            return;
        }
        long logoutCooldownMs = net.dodian.uber.game.persistence.account.LogoutReLoginGuard.remainingMillis(hydration.getDbId());
        if (logoutCooldownMs > 0L) {
            OperationalTelemetry.incrementCounter("login.logout_cooldown", 1L);
            logger.info("Login delayed by logout cooldown player={} dbId={} remainingMs={}", client.getPlayerName(), hydration.getDbId(), logoutCooldownMs);
            releaseSlot(current);
            releaseToken(current);
            sendAndClose(ctx, 5);
            return;
        }

        LOGIN_ATTEMPTS.recordSuccess(parsed.getUsername());
        int loginRights = rightsForGroup(hydration.getPlayerGroup());
        sendLoginSuccess(ctx, loginRights);

        // CRITICAL: Setup game pipeline BEFORE PlayerInitializer sends packets
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
        ISAACCipher outCipher = ctx.channel().attr(OUT_CIPHER_KEY).get();
        ctx.pipeline().addLast(new ByteMessageEncoder(outCipher));
        // ctx.pipeline().addLast(new GamePacketEncoder()); // Removed - using pure ByteMessage/Netty
        ctx.pipeline().addLast(new GamePacketHandler(client));
        ctx.pipeline().remove(this);

        // Store reference for disconnect cleanup
        ctx.channel().attr(AttributeKey.valueOf("activeClient")).set(client);

        // Finish registration + initialization on the game thread. This avoids cross-thread mutation
        // of PlayerRegistry players[]/playersOnline and reduces login-related sync spikes.
        final io.netty.channel.Channel channel = ctx.channel();
        final int slotCopy = slot;
        final long finalizerQueuedAtNanos = System.nanoTime();
        current.setStage(LoginAttempt.Stage.FINALIZING);
        boolean finalizerAccepted = GameThreadIngress.submitCritical("login-finalize", () -> {
            net.dodian.uber.game.engine.loop.GameThreadContext.validateGameThread("player-registry.login-register");
            long finalizerStartedAtNanos = System.nanoTime();
            long queueWaitMs = (finalizerStartedAtNanos - finalizerQueuedAtNanos) / 1_000_000L;
            if (!channel.isActive() || client.disconnected) {
                long failures = LOGIN_CHANNEL_CLOSES_BEFORE_FINALIZE.incrementAndGet();
                OperationalTelemetry.incrementCounter("login.finalize.channel_closed", 1L);
                // Channel died before the game thread could register the player; release the reserved slot.
                releaseSlot(current);
                releaseToken(current);
                logger.warn(
                        "Login channel closed before game-thread finalization for {} queueWait={}ms failures={}",
                        client.getPlayerName(),
                        queueWaitMs,
                        failures
                );
                return;
            }

            try {
                AccountLoginService.hydrateGame(client, hydration);
                client.validLogin = true;
                client.playerRights = loginRights;
                client.premium = client.playerRights > 0 || client.premium;
            } catch (Exception ex) {
                long failures = LOGIN_INITIALIZER_FAILURES.incrementAndGet();
                OperationalTelemetry.incrementCounter("login.hydration.failure", 1L);
                releaseSlot(current);
                releaseToken(current);
                logger.warn(
                        "[GameThread] Account hydration failed for {} queueWait={}ms failures={}",
                        client.getPlayerName(), queueWaitMs, failures, ex);
                channel.close();
                return;
            }

            Client previous = PlayerRegistry.playersOnline.putIfAbsent(client.longName, client);
            if (previous != null) {
                boolean previousStale = previous.disconnected || !previous.isActive
                        || previous.channel == null || !previous.channel.isActive();
                if (!previousStale) {
                    releaseSlot(current);
                    releaseToken(current);
                    logger.warn(
                            "Duplicate login prevented for {} queueWait={}ms — another session already active",
                            client.getPlayerName(),
                            queueWaitMs
                    );
                    channel.close();
                    return;
                }
                // previous is stale; replace it
                PlayerRegistry.playersOnline.remove(client.longName, previous);
                PlayerRegistry.playersOnline.put(client.longName, client);
            }
            net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[slotCopy] = client;
            current.markSlotReleased(); // ownership has transferred to PlayerRegistry
            releaseToken(current);

            long initializerDurationMs = 0L;
            try {
                long initializerStartNanos = System.nanoTime();
                PlayerInitializer initializer = new PlayerInitializer();
                initializer.initializeCriticalLoginState(client);
                initializerDurationMs = (System.nanoTime() - initializerStartNanos) / 1_000_000L;
                client.initialized = true;

                if (!channel.isActive() || client.disconnected) {
                    current.setStage(LoginAttempt.Stage.FAILED);
                    PlayerRegistry.removePlayer(client);
                    return;
                }
                client.isActive = true;
                if (client.getUpdateFlags() != null) {
                    client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                }
                client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));
                // Publish to player synchronization only after hydration, appearance and
                // initial placement are all complete.
                client.setSynchronizationReady(true);
                GameEventBus.post(new PlayerLoginEvent(client));

                long totalMs = (System.nanoTime() - current.getAcceptedAtNanos()) / 1_000_000L;
                current.setStage(LoginAttempt.Stage.COMPLETE);
                OperationalTelemetry.recordPhaseMillis("login.account_load", loadResult.getDurationMs());
                OperationalTelemetry.recordPhaseMillis("login.finalize_queue", queueWaitMs);
                OperationalTelemetry.recordPhaseMillis("login.initialize", initializerDurationMs);
                OperationalTelemetry.recordPhaseMillis("login.total", totalMs);
                logger.info(
                        "Login finished player={} slot={} x={} y={} z={} remote={} parse={}ms load={}ms " +
                                "queueWait={}ms init={}ms total={}ms pendingRetries={} slotReserve={}ms",
                        client.getPlayerName(), slotCopy,
                        client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ(),
                        current.getRemoteIp(), parsed.getParseDurationMs(), loadResult.getDurationMs(),
                        queueWaitMs, initializerDurationMs, totalMs, loadResult.getPendingRetries(), slotReserveDurationMs
                );

                final PlayerInitializer postInitializer = initializer;
                GameThreadIngress.submitDeferred("login-post-init", () -> {
                    if (!client.disconnected) {
                        postInitializer.initializeDeferredPostLoginState(client);
                    }
                });
            } catch (Exception ex) {
                long failures = LOGIN_INITIALIZER_FAILURES.incrementAndGet();
                OperationalTelemetry.incrementCounter("login.finalize.failure", 1L);
                logger.warn(
                        "[GameThread] PlayerInitializer error for {} failures={}",
                        client.getPlayerName(),
                        failures,
                        ex
                );
                PlayerRegistry.removePlayer(client);
                channel.close();
            }

        });

        if (!finalizerAccepted) {
            OperationalTelemetry.incrementCounter("login.finalize.rejected", 1L);
            logger.warn("Login finalization queue full for {}; closing session", client.getPlayerName());
            releaseSlot(current);
            releaseToken(current);
            channel.close();
            return;
        }

    }

    private static String remoteIp(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            return address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int rightsForGroup(int playerGroup) {
        if (net.dodian.uber.game.engine.config.DotEnvKt.getRankAdminGroupIds().contains(playerGroup)) return 2;
        if (net.dodian.uber.game.engine.config.DotEnvKt.getRankModGroupIds().contains(playerGroup)) return 1;
        return 0;
    }

    /* Slot helpers */
    private int reserveSlot() {
        synchronized (PlayerRegistry.slotLock) {
            int maxPlayers = DotEnvKt.getGameMaxPlayers();
            for (int i = 1; i <= maxPlayers; i++) {
                if (!PlayerRegistry.usedSlots.get(i)) {
                    PlayerRegistry.usedSlots.set(i);
                    return i;
                }
            }
        }
        return -1;
    }

    private static void releaseSlot(LoginAttempt current) {
        if (!current.markSlotReleased()) return;
        int slot = current.getReservedSlot();
        if (slot <= 0) return;
        synchronized (PlayerRegistry.slotLock) {
            if (PlayerRegistry.players[slot] == null || PlayerRegistry.players[slot] == current.client) {
                PlayerRegistry.usedSlots.clear(slot);
                PlayerRegistry.players[slot] = null;
            }
        }
    }

    private static void releaseToken(LoginAttempt current) {
        if (current.markTokenReleased() && current.longName != 0L) {
            LOADING_ACCOUNTS.remove(current.longName, current);
        }
    }

    private static void releaseAttempt(LoginAttempt current) {
        if (current == null) return;
        releaseToken(current);
        releaseSlot(current);
    }

    private void sendLoginSuccess(ChannelHandlerContext ctx, int rights) {
        ctx.writeAndFlush(successResponse(ctx.alloc(), rights));
    }

    static ByteBuf successResponse(io.netty.buffer.ByteBufAllocator allocator, int rights) {
        ByteBuf resp = allocator.buffer(3, 3);
        resp.writeByte(LOGIN_SUCCESS_CODE);
        resp.writeByte(rights);
        resp.writeByte(0); // client flagged state
        return resp;
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
        LoginAttempt current = attempt;
        if (current != null && current.getStage() != LoginAttempt.Stage.COMPLETE) releaseAttempt(current);
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] Login processing error for {}", ctx.channel().remoteAddress(), cause);
        LoginAttempt current = attempt;
        if (current != null && current.getStage() != LoginAttempt.Stage.COMPLETE) releaseAttempt(current);
        ctx.close();
    }

    public static String describeReason(int code) {
        switch (code) {
            case 3:
                return "Incorrect password";
            case 4:
                return "Account disabled/banned";
            case 5:
                return "Account already online or loading";
            case 6:
                return "Game updated";
            case 7:
                return "World full";
            case 8:
                return "Login server offline";
            case 9:
                return "Login limit exceeded";
            case 10:
                return "Bad session ID";
            case 11:
                return "Session rejected by login server";
            case 13:
                return "Could not complete login / internal error";
            case 14:
                return "Server is currently updating";
            case 16:
                return "Too many login attempts from your IP";
            case 34:
                return "Authentication blocked for five minutes";
            case 35:
                return "Authentication blocked for 24 hours";
            case 22:
                return "MAC/IP/UID Banned";
            case 23:
                return "Username too short (minimum 3 characters)";
            case 26:
                return "No account created / Discord verification required";
            case 29:
                return "Invalid or expired Discord authorization code";
            case 30:
                return "Username capitalization does not match registered account";
            case 31:
                return "Discord account limit reached";
            case 32:
                return "Discord email not verified";
            case 33:
                return "Username already exists; choose another username";
            default:
                return "Unknown error (" + code + ")";
        }
    }

    private static void recordAuthenticationFailure(ParsedLoginRequest parsed, String remoteIp, int code) {
        long now = System.currentTimeMillis();
        if (code == 3 || code == 26) {
            LOGIN_ATTEMPTS.recordCredentialFailure(remoteIp, parsed.getUsername(), now);
            OperationalTelemetry.incrementCounter("login.auth.failure", 1L);
        } else if (code == 29 && parsed.isAccountCreationRequested()) {
            LOGIN_ATTEMPTS.recordCreationAuthFailure(
                    remoteIp, parsed.getUsername(), parsed.getDiscordAuthCode(), now);
            OperationalTelemetry.incrementCounter("login.creation_auth.failure", 1L);
        }
    }

    private static boolean isExpectedAuthenticationRejection(int code) {
        return code == 3 || code == 4 || code == 5 || code == 23 || code == 26
                || code == 29 || code == 30 || code == 31 || code == 32 || code == 33;
    }
}
