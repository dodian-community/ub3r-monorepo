package net.dodian.uber.game.netty.listener;

import net.dodian.uber.game.engine.systems.net.PacketRegistrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages packet listener registration.
 */
public final class PacketListenerManager {

    private static final Logger logger = LoggerFactory.getLogger(PacketListenerManager.class);
    
    private static final PacketRepository repository = PacketRepository.getInstance();
    
    private static volatile PacketRegistrationReport registrationReport;

    public static synchronized PacketRegistrationReport initialize() {
        if (registrationReport != null) {
            return registrationReport;
        }
        try {
            logger.debug("Discovering packet listeners...");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = PacketListenerManager.class.getClassLoader();
            }
            PacketListenerDiscovery.Result discovery = PacketListenerDiscovery.discover(classLoader);
            for (PacketListenerDiscovery.Binding binding : discovery.bindings()) {
                repository.register(binding.opcode(), binding.listener());
            }

            PacketRegistrationReport report = buildRegistrationReport(discovery);
            validateCriticalOpcodesOrThrow(report);
            repository.lock();
            registrationReport = report;
            // Truncated to 12 chars to match every other fingerprint printed at startup
            // (ContentModuleIndex's "Plugin index frozen" line) - the full hash is still in
            // report.getFingerprint() for anything that needs to compare it exactly.
            String fingerprint = report.getFingerprint();
            logger.info("packet_listeners_ready handlers={} bindings={} duplicates={} fingerprint={}",
                    report.getDiscoveredHandlerCount(), report.getRegisteredCount(),
                    report.getDuplicateOverwriteCount(),
                    fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint);
            return report;
        } catch (Throwable e) {
            logger.error("Failed to register packet listeners", e);
            throw new IllegalStateException("Packet listener initialization failed", e);
        }
    }

    private PacketListenerManager() {}

    public static void register(int opcode, PacketListener listener) {
        repository.register(opcode, listener);
    }

    public static PacketListener get(int opcode) {
        if (registrationReport == null || !repository.isLocked()) {
            throw new IllegalStateException("Packet listeners were not initialized during server startup");
        }
        return repository.get(opcode);
    }

    public static boolean isInitialized() {
        return registrationReport != null && repository.isLocked();
    }

    /**
     * Gets the packet repository instance.
     * Useful for advanced operations.
     */
    public static PacketRepository getRepository() {
        return repository;
    }

    private static PacketRegistrationReport buildRegistrationReport(PacketListenerDiscovery.Result discovery) {
        List<Integer> missingCriticalOpcodes = new ArrayList<>();
        for (int opcode : PacketRegistrationReport.CRITICAL_OPCODES) {
            if (!repository.has(opcode)) {
                missingCriticalOpcodes.add(opcode);
            }
        }
        return new PacketRegistrationReport(
            repository.getRegisteredCount(),
            missingCriticalOpcodes,
            repository.getDuplicateOverwriteCount(),
            discovery.handlerCount(),
            discovery.owners(),
            discovery.fingerprint()
        );
    }

    private static void validateCriticalOpcodesOrThrow(PacketRegistrationReport report) {
        if (report.getHasMissingCriticalOpcodes()) {
            throw new IllegalStateException("Missing critical packet listeners: " + report.getMissingCriticalOpcodes());
        }
    }
}
