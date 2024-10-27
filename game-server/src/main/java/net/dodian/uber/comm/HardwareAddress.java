package net.dodian.uber.comm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class HardwareAddress {

    public static String getMacAddress(InetAddress addr) throws UnknownHostException,
            SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(addr);
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        StringBuilder macAddressBuilder = new StringBuilder();

        for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
            String macAddressHexByte = String.format("%02X",
                    macAddressBytes[macAddressByteIndex]);

            macAddressBuilder.append(macAddressHexByte);

            if (macAddressByteIndex != macAddressBytes.length - 1) {
                macAddressBuilder.append(":");
            }
        }

        return macAddressBuilder.toString();
    }
}