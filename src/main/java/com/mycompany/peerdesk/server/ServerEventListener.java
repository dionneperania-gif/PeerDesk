package com.mycompany.peerdesk.server;

/**
 * Listener interface for server activity events (logs, client joins/leaves).
 * Allows the ServerGUI to update in real time.
 */
public interface ServerEventListener {
    void onLog(String timestamp, String message);
    void onClientCountChanged(int activeCount);
}
