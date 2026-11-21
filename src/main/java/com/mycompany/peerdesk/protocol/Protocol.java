package com.mycompany.peerdesk.protocol;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Protocol defines the standardized message commands and packet format
 * used for communication between the PeerDesk Server and connected Clients.
 *
 * All messages follow a pipe-delimited format:
 * COMMAND|ARG1|ARG2|...
 */
public final class Protocol {

    private Protocol() {}

    // Protocol version
    public static final int PROTOCOL_VERSION = 1;

    // Network constants
    public static final int MAX_MESSAGE_LENGTH = 2000;
    public static final int MAX_NICK_LENGTH = 16;
    public static final int MIN_NICK_LENGTH = 2;
    public static final int CONNECTION_TIMEOUT_MS = 5000;
    public static final int DEFAULT_PORT = 8888;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Client -> Server Commands
    public static final String CMD_CONNECT        = "CONNECT";        // CONNECT|<nick>
    public static final String CMD_NAME           = "NAME";           // NAME|<newNick>
    public static final String CMD_CHAT           = "CHAT";           // CHAT|<message>
    public static final String CMD_EXIT           = "EXIT";           // EXIT
    public static final String CMD_TICKET_NEW     = "TICKET_NEW";     // TICKET_NEW|<category>|<subject> or TICKET_NEW|<category>|<subject>|<requester>
    public static final String CMD_TICKET_CLAIM   = "TICKET_CLAIM";   // TICKET_CLAIM|<ticketId>
    public static final String CMD_TICKET_RESOLVE = "TICKET_RESOLVE"; // TICKET_RESOLVE|<ticketId>
    public static final String CMD_TICKET_STATUS  = "TICKET_STATUS";  // TICKET_STATUS|<ticketId>|<status>
    public static final String CMD_TICKET_DELETE  = "TICKET_DELETE";  // TICKET_DELETE|<ticketId>
    public static final String CMD_TICKET_EDIT   = "TICKET_EDIT";    // TICKET_EDIT|<ticketId>|<category>|<subject>|<requester>
    public static final String CMD_REACT          = "REACT";          // REACT|<msgId>|<reactionType>
    public static final String CMD_PING           = "PING";           // PING
    public static final String CMD_PONG           = "PONG";           // PONG

    // Server -> Client Responses & Broadcasts
    public static final String RSP_AUTH_OK        = "AUTH_OK";        // AUTH_OK|<assignedNick>
    public static final String RSP_AUTH_ERR       = "AUTH_ERR";       // AUTH_ERR|<reason>
    public static final String RSP_NAME_OK        = "NAME_OK";        // NAME_OK|<newNick>
    public static final String RSP_NAME_ERR       = "NAME_ERR";       // NAME_ERR|<reason>
    public static final String BCAST_USER_LIST    = "USER_LIST";      // USER_LIST|<nick1>,<nick2>,...
    public static final String BCAST_CHAT         = "CHAT_MSG";       // CHAT_MSG|<msgId>|<time>|<sender>|<message>
    public static final String BCAST_SYS          = "SYS_MSG";        // SYS_MSG|<time>|<message>
    public static final String BCAST_TICKETS      = "TICKET_LIST";    // TICKET_LIST|<ticket1>@@<ticket2>...
    public static final String BCAST_REACT        = "MSG_REACT";      // MSG_REACT|<msgId>|<reactionType>|<count>
    public static final String RSP_SERVER_ERR     = "SERVER_ERR";     // SERVER_ERR|<errorMessage>

    public static final String TICKET_DELIMITER   = "@@";

    /**
     * Gets current local timestamp formatted as HH:mm:ss.
     */
    public static String getCurrentTimestamp() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    /**
     * Sanitizes chat messages to prevent unintended pipe delimiters from breaking parsing.
     */
    public static String sanitizeChat(String text) {
        if (text == null) return "";
        return text.replace("|", "｜").replace("\n", " ").replace("\r", "");
    }

    /**
     * Builds a pipe-delimited packet string.
     */
    public static String buildPacket(String command, String... args) {
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append("|").append(arg != null ? arg : "");
        }
        return sb.toString();
    }
}
