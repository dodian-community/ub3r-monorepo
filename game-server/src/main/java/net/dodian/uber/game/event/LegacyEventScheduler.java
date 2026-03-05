package net.dodian.uber.game.event;

import net.dodian.uber.game.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.dodian.utilities.DotEnvKt.getRuntimePhaseWarnMs;

@Deprecated
public class LegacyEventScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LegacyEventScheduler.class);

    /**
     * The world instance.
     */
    private static LegacyEventScheduler instance = null;

    /**
     * A list of connected players.
     */
    // private EntityList<Player> players;
    public static LegacyEventScheduler getInstance() {
        if (instance == null) {
            instance = new LegacyEventScheduler();
        }
        return instance;
    }

    private LegacyEventScheduler() {
        pendingAdds = new ConcurrentLinkedQueue<>();
        scheduled = new PriorityQueue<>((a, b) -> {
            int dueCompare = Long.compare(a.dueAtMs, b.dueAtMs);
            return dueCompare != 0 ? dueCompare : Long.compare(a.id, b.id);
        });
    }

    /**
     * A list of pending events.
     */
    private final Queue<LegacyEvent> pendingAdds;
    private final PriorityQueue<ScheduledEvent> scheduled;
    private long nextId = 0L;

    public void run() {
        while (!Server.shutdownServer) {
            processEvents();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

    }

    /**
     * Registers an event.
     *
     * @param event
     */
    @Deprecated
    public void registerEvent(LegacyEvent event) {
        if (event == null) {
            return;
        }
        pendingAdds.add(event);
    }

    /**
     * Processes any pending events.
     */
    public void processEvents() {
        final long startNs = System.nanoTime();
        final long nowMs = System.currentTimeMillis();

        // Drain any newly registered events.
        for (;;) {
            LegacyEvent event = pendingAdds.poll();
            if (event == null) {
                break;
            }
            if (event.isStopped()) {
                continue;
            }
            schedule(event, nowMs);
        }

        int duePolledCount = 0;
        int ranCount = 0;
        long slowest1Ns = 0L;
        long slowest2Ns = 0L;
        long slowest3Ns = 0L;
        String slowest1 = null;
        String slowest2 = null;
        String slowest3 = null;

        for (;;) {
            ScheduledEvent next = scheduled.peek();
            if (next == null || next.dueAtMs > nowMs) {
                break;
            }
            scheduled.poll();
            duePolledCount++;

            LegacyEvent event = next.event;
            if (event == null || event.isStopped()) {
                continue;
            }

            // Preserve legacy semantics: an event becomes ready when (now - lastRun) > tick.
            // If registration lag or clock drift would make it not ready yet, requeue shortly.
            if (!event.isReady()) {
                scheduled.add(new ScheduledEvent(next.id, nowMs + 1L, event));
                continue;
            }

            long eventStartNs = System.nanoTime();
            try {
                event.run();
            } catch (Exception exception) {
                logger.error("Legacy event failed: {}", event.getClass().getSimpleName(), exception);
                try {
                    event.stop();
                } catch (Exception ignored) {
                    // ignore secondary failures
                }
                continue;
            } finally {
                ranCount++;
                long eventNs = System.nanoTime() - eventStartNs;
                String simpleName = event.getClass().getSimpleName();
                String name = (simpleName == null || simpleName.isEmpty()) ? event.getClass().getName() : simpleName;
                name = name + "(tick=" + event.getTick() + ")";
                if (eventNs > slowest1Ns) {
                    slowest3Ns = slowest2Ns;
                    slowest3 = slowest2;
                    slowest2Ns = slowest1Ns;
                    slowest2 = slowest1;
                    slowest1Ns = eventNs;
                    slowest1 = name;
                } else if (eventNs > slowest2Ns) {
                    slowest3Ns = slowest2Ns;
                    slowest3 = slowest2;
                    slowest2Ns = eventNs;
                    slowest2 = name;
                } else if (eventNs > slowest3Ns) {
                    slowest3Ns = eventNs;
                    slowest3 = name;
                }
            }

            if (!event.isStopped()) {
                schedule(event, System.currentTimeMillis());
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (elapsedMs >= getRuntimePhaseWarnMs()) {
            logger.warn(
                    "LegacyEventScheduler slow: total={}ms duePolled={} ran={} queueSize={} slowest=[{}:{}ms, {}:{}ms, {}:{}ms]",
                    elapsedMs,
                    duePolledCount,
                    ranCount,
                    scheduled.size(),
                    slowest1,
                    slowest1Ns / 1_000_000L,
                    slowest2,
                    slowest2Ns / 1_000_000L,
                    slowest3,
                    slowest3Ns / 1_000_000L
            );
        }
    }

    private void schedule(LegacyEvent event, long nowMs) {
        int tick = event.getTick();
        long tickMs = Math.max(0L, tick);
        long dueAt = event.isReady() ? (nowMs + 1L) : (nowMs + tickMs + 1L);
        scheduled.add(new ScheduledEvent(++nextId, dueAt, event));
    }

    private static final class ScheduledEvent {
        final long id;
        final long dueAtMs;
        final LegacyEvent event;

        ScheduledEvent(long id, long dueAtMs, LegacyEvent event) {
            this.id = id;
            this.dueAtMs = dueAtMs;
            this.event = event;
        }
    }
}
