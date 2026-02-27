package net.dodian.stress;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public final class StressTestController {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<StressBotSession> sessions = new CopyOnWriteArrayList<>();
    private final List<Long> connectSamples = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger attempted = new AtomicInteger();
    private final AtomicInteger succeeded = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger disconnectedAfterLogin = new AtomicInteger();
    private final LongAdder totalConnectMs = new LongAdder();
    private final AtomicLong maxConnectMs = new AtomicLong();

    private volatile StressTestConfig config;
    private volatile Consumer<String> logSink = message -> {};
    private volatile ExecutorService spawnExecutor;
    private volatile ExecutorService botExecutor;
    private volatile long startedAtMs = 0L;

    public void start(StressTestConfig config, Consumer<String> logSink) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Stress test is already running.");
        }

        this.config = config;
        this.logSink = logSink;
        this.startedAtMs = System.currentTimeMillis();
        resetStats();

        spawnExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "stress-spawn"));
        botExecutor = Executors.newCachedThreadPool(r -> new Thread(r, "stress-bot"));

        log("Starting stress test: host=" + config.getHost() +
                ":" + config.getPort() +
                ", bots=" + config.getBotCount() +
                ", rate=" + config.getConnectRatePerSecond() + "/s");

        spawnExecutor.submit(this::spawnLoop);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        log("Stopping stress test...");
        for (StressBotSession session : sessions) {
            session.requestStop();
        }
        sessions.clear();

        shutdownExecutor(spawnExecutor);
        shutdownExecutor(botExecutor);

        log("Stress test stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public StressStatsSnapshot snapshot() {
        int successCount = succeeded.get();
        long averageConnect = successCount == 0 ? 0L : totalConnectMs.sum() / successCount;
        long p95 = computeP95();
        long max = maxConnectMs.get();
        long uptime = startedAtMs == 0L ? 0L : Math.max(0L, System.currentTimeMillis() - startedAtMs);

        StressTestConfig currentConfig = config;
        int target = currentConfig == null ? 0 : currentConfig.getBotCount();

        return new StressStatsSnapshot(
                running.get(),
                target,
                attempted.get(),
                successCount,
                failed.get(),
                active.get(),
                disconnectedAfterLogin.get(),
                averageConnect,
                p95,
                max,
                uptime
        );
    }

    private void spawnLoop() {
        StressTestConfig currentConfig = config;
        if (currentConfig == null) {
            return;
        }

        double rate = Math.max(0.1D, currentConfig.getConnectRatePerSecond());
        long intervalNanos = Math.max(1L, (long) (1_000_000_000D / rate));
        long nextAt = System.nanoTime();

        StressBotSession.Listener listener = new StressBotSession.Listener() {
            @Override
            public void onLoginSuccess(String username, long connectMs, int rights) {
                succeeded.incrementAndGet();
                active.incrementAndGet();
                disconnectedAfterLogin.compareAndSet(0, disconnectedAfterLogin.get());
                totalConnectMs.add(connectMs);
                connectSamples.add(connectMs);
                maxConnectMs.accumulateAndGet(connectMs, Math::max);
                log("Connected " + username + " in " + connectMs + "ms (rights " + rights + ")");
            }

            @Override
            public void onSessionEnded(String username, boolean loggedIn, String reason) {
                if (loggedIn) {
                    active.updateAndGet(value -> Math.max(0, value - 1));
                    disconnectedAfterLogin.incrementAndGet();
                    log("Disconnected " + username + " (" + reason + ")");
                } else {
                    failed.incrementAndGet();
                    log("Failed " + username + " (" + reason + ")");
                }
            }
        };

        for (int i = 0; i < currentConfig.getBotCount() && running.get(); i++) {
            int userNumber = currentConfig.getStartIndex() + i;
            String username = currentConfig.getUsernamePrefix() + userNumber;
            attempted.incrementAndGet();

            StressBotSession session = new StressBotSession(currentConfig, username, listener, running::get);
            sessions.add(session);
            botExecutor.submit(session);

            nextAt += intervalNanos;
            sleepUntil(nextAt);
        }

        log("Spawn queue completed.");
    }

    private void sleepUntil(long targetNanos) {
        while (running.get()) {
            long remaining = targetNanos - System.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(remaining);
            int nanos = (int) (remaining - TimeUnit.MILLISECONDS.toNanos(millis));
            try {
                Thread.sleep(millis, nanos);
                return;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void resetStats() {
        attempted.set(0);
        succeeded.set(0);
        failed.set(0);
        active.set(0);
        disconnectedAfterLogin.set(0);
        totalConnectMs.reset();
        maxConnectMs.set(0L);
        connectSamples.clear();
    }

    private long computeP95() {
        List<Long> copy;
        synchronized (connectSamples) {
            if (connectSamples.isEmpty()) {
                return 0L;
            }
            copy = new ArrayList<>(connectSamples);
        }
        copy.sort(Long::compareTo);
        int index = (int) Math.ceil(copy.size() * 0.95D) - 1;
        index = Math.max(0, Math.min(index, copy.size() - 1));
        return copy.get(index);
    }

    private void log(String message) {
        String now = LocalTime.now().format(LOG_TIME);
        logSink.accept("[" + now + "] " + message);
    }
}
