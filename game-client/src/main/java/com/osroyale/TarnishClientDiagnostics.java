package com.osroyale;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;

/** Bounded, failure-only diagnostics. It observes the Tarnish protocol without changing it. */
@Slf4j
final class TarnishClientDiagnostics {
    private static final int CAPACITY = 32;
    private static final long STALL_NANOS = 5_000_000_000L;
    private static final Deque<String> EVENTS = new ArrayDeque<>(CAPACITY);
    private static final AtomicBoolean WATCHDOG_STARTED = new AtomicBoolean();
    private static volatile long lastHeartbeat = System.nanoTime();
    private static volatile boolean loggedIn;
    private static volatile String renderSubject = "none";
    private static volatile String lastFailureSignature = "";
    private static volatile int lastLoadingResult = Integer.MIN_VALUE;

    private TarnishClientDiagnostics() {}

    static void heartbeat(boolean isLoggedIn, int tick, int baseX, int baseY, Player local) {
        loggedIn = isLoggedIn;
        lastHeartbeat = System.nanoTime();
        if (tick % 100 == 0 && isLoggedIn) {
            add("heartbeat tick=" + tick + " base=" + baseX + ',' + baseY + " local=" + position(local));
        }
        startWatchdog();
    }

    static void packet(int opcode, int size, byte[] payload, int tick, int baseX, int baseY, Player local) {
        add("packet opcode=" + opcode + " size=" + size + " hash=" + hash(payload, size) +
                " tick=" + tick + " base=" + baseX + ',' + baseY + " local=" + position(local));
    }

    static void playerMask(int playerIndex, int mask, int fixedBits, int variableBytes, Player player) {
        add("player-mask index=" + playerIndex + " mask=0x" + Integer.toHexString(mask) +
                " fixedBits=" + fixedBits + " variableBytes=" + variableBytes +
                " subject=" + summary(player));
    }

    static void appearance(int playerIndex, int length, byte[] bytes, Player player) {
        add("appearance index=" + playerIndex + " length=" + length + " hash=" + hash(bytes, bytes.length) +
                " subject=" + summary(player) + " models=" + Arrays.toString(player.appearanceModels) +
                " anims=" + player.seqStandID + ',' + player.walkingAnimation + ',' + player.runAnimation);
    }

    static void region(int chunkX, int chunkY, int baseX, int baseY, int[] regions, int[] maps, int[] landscapes) {
        add("region chunk=" + chunkX + ',' + chunkY + " base=" + baseX + ',' + baseY +
                " regions=" + Arrays.toString(regions) + " maps=" + Arrays.toString(maps) +
                " landscapes=" + Arrays.toString(landscapes));
    }

    static void fileResponse(int index, int file, byte[] data, int loadingStage) {
        add("swift-response index=" + index + " file=" + file + " bytes=" + (data == null ? -1 : data.length) +
                " stage=" + loadingStage);
    }

    static void loadingResult(int result, int baseX, int baseY, Player local) {
        if (result == lastLoadingResult) return;
        lastLoadingResult = result;
        add("loading result=" + result + " base=" + baseX + ',' + baseY + " local=" + position(local));
    }

    static void beginRender(Player player) { renderSubject = summary(player); }
    static void endRender() { renderSubject = "none"; }

    static void failure(String stage, Throwable failure) {
        String signature = stage + ':' + failure.getClass().getName() + ':' + failure.getMessage();
        if (signature.equals(lastFailureSignature)) return;
        lastFailureSignature = signature;
        log.error("tarnish_client_failure stage={} renderSubject={} recent={} ", stage, renderSubject, snapshot(), failure);
    }

    static synchronized String snapshot() { return EVENTS.toString(); }

    private static synchronized void add(String event) {
        if (EVENTS.size() == CAPACITY) EVENTS.removeFirst();
        EVENTS.addLast(event);
    }

    private static String summary(Player player) {
        if (player == null) return "null";
        return "name=" + player.name + " pos=" + position(player) + " rights=" + player.privelage +
                " combat=" + player.combatLevel;
    }

    private static String position(Player player) {
        return player == null ? "unknown" : (player.x >> 7) + "," + (player.y >> 7);
    }

    private static String hash(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        if (bytes != null) crc.update(bytes, 0, Math.min(length, bytes.length));
        return String.format("%08x", crc.getValue());
    }

    private static void startWatchdog() {
        if (!WATCHDOG_STARTED.compareAndSet(false, true)) return;
        Thread watchdog = new Thread(() -> {
            while (true) {
                try { Thread.sleep(1_000L); } catch (InterruptedException ignored) { return; }
                if (!loggedIn || System.nanoTime() - lastHeartbeat < STALL_NANOS) continue;
                String signature = "stall:" + renderSubject;
                if (signature.equals(lastFailureSignature)) continue;
                lastFailureSignature = signature;
                StringBuilder stacks = new StringBuilder();
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    stacks.append('\n').append(entry.getKey().getName());
                    for (StackTraceElement element : entry.getValue()) stacks.append("\n  at ").append(element);
                }
                log.error("tarnish_client_stall renderSubject={} recent={} threads={}", renderSubject, snapshot(), stacks);
            }
        }, "tarnish-client-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }
}
