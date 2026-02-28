package net.dodian.uber.game.netty.bootstrap;

import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NettyGameServerTest {

    @Test
    public void defaultsToDisabledWhenUnset() {
        assertEquals(ResourceLeakDetector.Level.DISABLED, NettyGameServer.resolveLeakDetectionLevel(null));
        assertEquals(ResourceLeakDetector.Level.DISABLED, NettyGameServer.resolveLeakDetectionLevel(""));
        assertEquals(ResourceLeakDetector.Level.DISABLED, NettyGameServer.resolveLeakDetectionLevel("disabled"));
    }

    @Test
    public void supportsExplicitLeakDetectionOverrides() {
        assertEquals(ResourceLeakDetector.Level.SIMPLE, NettyGameServer.resolveLeakDetectionLevel("simple"));
        assertEquals(ResourceLeakDetector.Level.ADVANCED, NettyGameServer.resolveLeakDetectionLevel("advanced"));
        assertEquals(ResourceLeakDetector.Level.PARANOID, NettyGameServer.resolveLeakDetectionLevel("paranoid"));
    }

    @Test
    public void rejectsUnknownLeakDetectionOverride() {
        assertThrows(IllegalArgumentException.class, () -> NettyGameServer.resolveLeakDetectionLevel("nope"));
    }
}
