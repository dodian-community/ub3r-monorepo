package net.dodian.uber.game.netty.login;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dodian.utilities.ISAACCipher;
import net.dodian.uber.game.engine.config.DotEnvKt;
import net.dodian.uber.game.engine.metrics.OperationalTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bounded CPU worker for RSA decryption and Tarnish login-block validation. */
public final class LoginPreparationService {
    private static final Logger logger = LoggerFactory.getLogger(LoginPreparationService.class);
    private static final int WORKERS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    private static final int QUEUE_CAPACITY = 256;
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            WORKERS,
            WORKERS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int sequence;
                @Override public synchronized Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "login-auth-" + (++sequence));
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    private LoginPreparationService() {}

    public static boolean submit(LoginAttempt attempt, Consumer<Result> completion) {
        attempt.setStage(LoginAttempt.Stage.PREPARING);
        try {
            EXECUTOR.execute(() -> {
                Result result;
                try {
                    result = Result.success(parse(attempt));
                } catch (LoginRejected rejection) {
                    result = Result.failure(rejection.responseCode, rejection.getMessage());
                } catch (RuntimeException exception) {
                    logger.warn("Login preparation crashed remote={}", attempt.getRemoteIp(), exception);
                    result = Result.failure(13, "unexpected login preparation failure");
                }
                completion.accept(result);
            });
            OperationalTelemetry.incrementCounter("login.prepare.submitted", 1L);
            return true;
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            OperationalTelemetry.incrementCounter("login.prepare.rejected", 1L);
            return false;
        }
    }

    static ParsedLoginRequest parse(LoginAttempt attempt) {
        long started = System.nanoTime();
        Cursor outer = new Cursor(attempt.payload());
        require(outer.remaining() >= 3, 13, "truncated login header");
        require(outer.u8() == 255, 13, "invalid login magic");
        int version = outer.u8();
        require(version == DotEnvKt.getClientVersion(), 6, "unsupported client version " + version);
        outer.u8(); // low-memory flag
        require(outer.remaining() >= 9 * Integer.BYTES + 1, 13, "truncated CRC block");
        outer.skip(9 * Integer.BYTES);
        int rsaLength = outer.u8();
        require(rsaLength > 0 && outer.remaining() >= rsaLength, 13, "truncated RSA block");
        byte[] encryptedBytes = outer.bytes(rsaLength);
        require(outer.remaining() == 0, 13, "unexpected trailing login bytes");

        BigInteger encrypted = new BigInteger(1, encryptedBytes);
        byte[] decryptedBytes = encrypted.modPow(DotEnvKt.getRsaExponent(), DotEnvKt.getRsaModulus()).toByteArray();
        Cursor rsa = new Cursor(decryptedBytes);
        if (rsa.remaining() > 0 && rsa.peek() == 0) rsa.u8();
        require(rsa.remaining() >= 1 + 8 + 8 + 4 + 1, 13, "decrypted RSA block too short");
        require(rsa.u8() == 10, 13, "invalid RSA magic");
        long clientSeed = rsa.i64();
        long serverSeed = rsa.i64();
        require(serverSeed == attempt.getExpectedServerSeed(), 13, "RSA server seed mismatch");
        rsa.i32(); // legacy UID
        int loginMode = rsa.u8();
        require(loginMode == 0 || loginMode == 1, 13, "invalid login mode");
        String discordAuthCode = rsa.string(64, "discordAuthCode").trim();
        rsa.string(64, "MAC address");
        String username = rsa.string(12, "username", 18).trim();
        String password = rsa.string(20, "password", 19).trim();
        require(!username.isEmpty() && !password.isEmpty(), 13, "empty username or password");
        require(rsa.remaining() == 0, 13, "unexpected trailing RSA bytes");

        int[] seed = {(int) (clientSeed >>> 32), (int) clientSeed, (int) (serverSeed >>> 32), (int) serverSeed};
        ISAACCipher inbound = new ISAACCipher(seed.clone());
        for (int i = 0; i < seed.length; i++) seed[i] += 50;
        ISAACCipher outbound = new ISAACCipher(seed);
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        OperationalTelemetry.recordPhaseMillis("login.prepare", durationMs);
        return new ParsedLoginRequest(
                loginMode == 1 ? ParsedLoginRequest.Mode.CREATE_ACCOUNT : ParsedLoginRequest.Mode.LOGIN,
                username, password, discordAuthCode, inbound, outbound, durationMs);
    }

    public static void shutdown(Duration timeout) {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS)) EXECUTOR.shutdownNow();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            EXECUTOR.shutdownNow();
        }
    }

    static String workerThreadPrefix() { return "login-auth-"; }

    private static void require(boolean condition, int code, String message) {
        if (!condition) throw new LoginRejected(code, message);
    }

    private static final class LoginRejected extends RuntimeException {
        private final int responseCode;
        private LoginRejected(int responseCode, String message) { super(message); this.responseCode = responseCode; }
    }

    private static final class Cursor {
        private final ByteBuffer buffer;
        private Cursor(byte[] bytes) { buffer = ByteBuffer.wrap(bytes); }
        private int remaining() { return buffer.remaining(); }
        private byte peek() { return buffer.get(buffer.position()); }
        private int u8() { return buffer.get() & 0xFF; }
        private int i32() { return buffer.getInt(); }
        private long i64() { return buffer.getLong(); }
        private void skip(int length) { buffer.position(buffer.position() + length); }
        private byte[] bytes(int length) { byte[] value = new byte[length]; buffer.get(value); return value; }
        private String string(int maxLength, String field) {
            return string(maxLength, field, 13);
        }
        private String string(int maxLength, String field, int overflowCode) {
            StringBuilder value = new StringBuilder();
            boolean terminated = false;
            while (buffer.hasRemaining()) {
                int next = u8();
                if (next == 10) { terminated = true; break; }
                require(value.length() < maxLength, overflowCode, field + " exceeds maximum length");
                value.append((char) next);
            }
            require(terminated, 13, "unterminated " + field);
            return value.toString();
        }
    }

    public static final class Result {
        private final ParsedLoginRequest request;
        private final int responseCode;
        private final String reason;
        private Result(ParsedLoginRequest request, int responseCode, String reason) {
            this.request = request; this.responseCode = responseCode; this.reason = reason;
        }
        static Result success(ParsedLoginRequest request) { return new Result(request, 0, ""); }
        static Result failure(int code, String reason) { return new Result(null, code, reason); }
        public boolean isSuccess() { return request != null; }
        public ParsedLoginRequest getRequest() { return request; }
        public int getResponseCode() { return responseCode; }
        public String getReason() { return reason; }
    }
}
