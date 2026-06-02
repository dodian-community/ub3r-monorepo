package com.runescape.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Base64;

public final class WebBridgeSocket extends Socket {

    private final int connectionId;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private volatile boolean closed;

    public WebBridgeSocket(String bridgeUrl) throws IOException {
        if (bridgeUrl == null || bridgeUrl.isEmpty()) {
            throw new IOException("Missing WebSocket bridge URL");
        }
        int id = CheerpJWs.open(bridgeUrl);
        if (id < 0) {
            throw new IOException("Failed to open WebSocket bridge: " + bridgeUrl);
        }
        this.connectionId = id;
        this.inputStream = new BridgeInputStream();
        this.outputStream = new BridgeOutputStream();
        this.closed = false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ensureOpen();
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        ensureOpen();
        return outputStream;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            CheerpJWs.close(connectionId);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isConnected() {
        return !closed;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public int getSoTimeout() {
        return 0;
    }

    @Override
    public void setSoTimeout(int timeout) {
        // WebSocket bridge uses event-driven reads; timeout is handled by caller logic.
    }

    @Override
    public void setTcpNoDelay(boolean on) {
        // No-op: WebSocket transport does not expose TCP_NODELAY.
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        throw new UnsupportedOperationException("connect is not supported for WebBridgeSocket");
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        throw new UnsupportedOperationException("connect is not supported for WebBridgeSocket");
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Socket is closed");
        }
    }

    private final class BridgeInputStream extends InputStream {
        private static final int MAX_CHUNK = 65536;

        private byte[] buffer = new byte[0];
        private int index = 0;
        private boolean eof = false;

        @Override
        public int read() throws IOException {
            if (!ensureData(1)) {
                return -1;
            }
            return buffer[index++] & 0xFF;
        }

        @Override
        public int read(byte[] destination, int off, int len) throws IOException {
            if (destination == null) {
                throw new NullPointerException("destination");
            }
            if (off < 0 || len < 0 || len > destination.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            if (!ensureData(len)) {
                return -1;
            }
            int available = buffer.length - index;
            int count = Math.min(len, available);
            System.arraycopy(buffer, index, destination, off, count);
            index += count;
            return count;
        }

        @Override
        public int available() throws IOException {
            ensureOpen();
            int buffered = buffer.length - index;
            int remote = Math.max(0, CheerpJWs.available(connectionId));
            return buffered + remote;
        }

        private boolean ensureData(int preferredBytes) throws IOException {
            ensureOpen();
            if (index < buffer.length) {
                return true;
            }
            if (eof) {
                return false;
            }

            int readSize = Math.max(1, Math.min(preferredBytes, MAX_CHUNK));
            String base64 = CheerpJWs.readBase64(connectionId, readSize);
            if (base64 == null || base64.isEmpty()) {
                eof = true;
                return false;
            }

            try {
                buffer = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid base64 from WebSocket bridge", exception);
            }

            index = 0;
            if (buffer.length == 0) {
                eof = true;
                return false;
            }
            return true;
        }
    }

    private final class BridgeOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            byte[] payload = new byte[] {(byte) b};
            write(payload, 0, 1);
        }

        @Override
        public void write(byte[] payload, int off, int len) throws IOException {
            ensureOpen();
            if (payload == null) {
                throw new NullPointerException("payload");
            }
            if (off < 0 || len < 0 || len > payload.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return;
            }

            byte[] copy = new byte[len];
            System.arraycopy(payload, off, copy, 0, len);
            String base64 = Base64.getEncoder().encodeToString(copy);
            CheerpJWs.writeBase64(connectionId, base64);
        }

        @Override
        public void flush() throws IOException {
            ensureOpen();
        }

        @Override
        public void close() {
            WebBridgeSocket.this.close();
        }
    }
}
