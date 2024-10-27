package net.dodian.uber.comm;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;

/**
 * A list of all connections <code>InetAddress</code>
 *
 * @author Logan G
 */
public final class ConnectionList {
    public static int MAX_CONNECTIONS_PER_IP = 2;
    private static ConnectionList instance = null;
    private Map<InetAddress, Integer> connectionMap = null;
    public Client c;
    public Player p;

    /**
     * Private constructor for this class
     */
    private ConnectionList() {
        connectionMap = new Hashtable<InetAddress, Integer>();
    }

    /**
     * @return the instance of this class
     */
    public static ConnectionList getInstance() {
        if (instance == null) {
            instance = new ConnectionList();
        }
        return instance;
    }

    /**
     * Filters the connection
     *
     * @param address the <code>InetAddress</code> of the connection
     */
    public void addConnection(final InetAddress address) {
        if (filter(address)) {
            if (connectionMap.containsKey(address)) {
                connectionMap.put(address, connectionMap.get(address) + 1);
            } else {
                connectionMap.put(address, 1);
            }
        }
    }

    /**
     * Removes a <code>InetAddress</code> from the map
     *
     * @param address the <code>InetAddress</code> removed
     */
    public void remove(final InetAddress address) {
        if (connectionMap.containsKey(address)) {
            if (connectionMap.get(address) > 1) {
                connectionMap.put(address, connectionMap.get(address) - 1);
            } else {
                connectionMap.remove(address);
            }
        }
    }

    public boolean filter(final InetAddress address) {
        // TODO: If we want multi-log exceptions, we need to re-implement the feature a better way.

        if (connectionMap.containsKey(address)) {
            return connectionMap.get(address) < MAX_CONNECTIONS_PER_IP;
        }

        return true;
    }

    public static boolean isSameMac(final String macAddress) throws UnknownHostException, SocketException {
        return HardwareAddress.getMacAddress(InetAddress.getByName(macAddress)).equals(macAddress);
    }

    /**
     * @return the connectionMap
     */
    public Map<InetAddress, Integer> getConnectionMap() {
        return connectionMap;
    }
}