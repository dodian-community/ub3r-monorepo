package com.runescape.web;

public final class CheerpJWs {

    private CheerpJWs() {
    }

    public static native int open(String url);

    public static native int readByte(int id);

    public static native String readBase64(int id, int maxBytes);

    public static native int available(int id);

    public static native void writeBase64(int id, String base64);

    public static native void close(int id);

    public static native String getLocalStorage(String key);

    public static native void setLocalStorage(String key, String value);
}
