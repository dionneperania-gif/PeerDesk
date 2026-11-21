package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import java.util.List;

/**
 * Callback interface for receiving network events from PeerDeskClient.
 * All callbacks are safely dispatched to the Swing Event Dispatch Thread (EDT).
 */
public interface PeerDeskClientListener {
    void onConnected(String assignedNick);
    void onAuthFailed(String reason);
    void onNameChangeSuccess(String newNick);
    void onNameChangeFailed(String reason);
    void onChatMessage(int messageId, String timestamp, String sender, String message);
    void onSystemMessage(String timestamp, String message);
    void onUserListUpdated(List<String> users);
    void onTicketListUpdated(List<Ticket> tickets);
    void onMessageReactionUpdated(int messageId, String reactionType, int count);
    void onServerMessage(String message, boolean isError);
    void onDisconnected(String reason);
}
