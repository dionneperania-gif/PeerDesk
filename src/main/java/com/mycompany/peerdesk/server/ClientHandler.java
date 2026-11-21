package com.mycompany.peerdesk.server;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.protocol.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Handles communication with a single connected client over a TCP socket.
 * Runs on its own thread, parsing incoming packets and delegating actions
 * to the PeerDeskServer.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final PeerDeskServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private volatile boolean running = true;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    public ClientHandler(Socket socket, PeerDeskServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Thread-safe message transmission to prevent interleaved writes.
     */
    public void sendMessage(String packet) {
        if (closed.get()) return;
        synchronized (writeLock) {
            if (out != null && !socket.isClosed()) {
                out.println(packet);
                out.flush();
            }
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // First step: Wait for initial handshake CONNECT|<nick>
            String initialLine = in.readLine();
            if (initialLine == null) {
                return;
            }

            if (!handleInitialHandshake(initialLine)) {
                return; // Handshake failed, exit thread and close socket
            }

            // Main message processing loop
            String line;
            while (running && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                processPacket(line);
            }

        } catch (IOException e) {
            if (running) {
                server.log("Connection error with " + (nickname != null ? nickname : socket.getRemoteSocketAddress()) + ": " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    /**
     * Validates and completes initial client registration.
     */
    private boolean handleInitialHandshake(String line) {
        String[] parts = line.split("\\|", 2);
        if (!Protocol.CMD_CONNECT.equals(parts[0]) || parts.length < 2) {
            sendMessage(Protocol.buildPacket(Protocol.RSP_AUTH_ERR, "Invalid handshake protocol."));
            return false;
        }

        String requestedNick = parts[1].trim();
        String validationError = server.validateNickname(requestedNick);
        if (validationError != null) {
            sendMessage(Protocol.buildPacket(Protocol.RSP_AUTH_ERR, validationError));
            return false;
        }

        this.nickname = requestedNick;
        sendMessage(Protocol.buildPacket(Protocol.RSP_AUTH_OK, this.nickname));
        server.registerClient(this);

        // Announce user joined
        server.broadcastSystemMessage(this.nickname + " joined PeerDesk!");
        server.broadcastUserList();
        server.syncTicketsToClient(this);
        server.syncChatHistoryToClient(this);
        return true;
    }

    /**
     * Parses and processes incoming protocol commands.
     */
    private void processPacket(String line) {
        String[] parts = line.split("\\|", -1);
        String command = parts[0];

        switch (command) {
            case Protocol.CMD_CHAT:
                if (parts.length > 1) {
                    String chatText = parts[1].trim();
                    if (chatText.length() > Protocol.MAX_MESSAGE_LENGTH) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR,
                                "Message too long (max " + Protocol.MAX_MESSAGE_LENGTH + " characters)."));
                    } else if (!chatText.isEmpty()) {
                        server.broadcastChatMessage(this.nickname, chatText);
                    }
                }
                break;

            case Protocol.CMD_PING:
                sendMessage(Protocol.buildPacket(Protocol.CMD_PONG));
                break;

            case Protocol.CMD_NAME:
                if (parts.length > 1) {
                    String newNick = parts[1].trim();
                    handleNameChange(newNick);
                } else {
                    sendMessage(Protocol.buildPacket(Protocol.RSP_NAME_ERR, "Nickname cannot be blank."));
                }
                break;

            case Protocol.CMD_EXIT:
                running = false;
                break;

            case Protocol.CMD_TICKET_NEW:
                if (parts.length >= 3) {
                    String category = parts[1].trim();
                    String subject = parts[2].trim();
                    String customRequester = (parts.length >= 4 && !parts[3].trim().isEmpty()) ? parts[3].trim() : this.nickname;
                    if (!category.isEmpty() && !subject.isEmpty()) {
                        server.createTicket(this.nickname, customRequester, category, subject);
                    } else {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Ticket category and subject cannot be blank."));
                    }
                }
                break;

            case Protocol.CMD_TICKET_CLAIM:
                if (parts.length >= 2) {
                    try {
                        int ticketId = Integer.parseInt(parts[1]);
                        server.claimTicket(ticketId, this.nickname);
                    } catch (NumberFormatException e) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Invalid ticket ID format."));
                    }
                }
                break;

            case Protocol.CMD_TICKET_RESOLVE:
                if (parts.length >= 2) {
                    try {
                        int ticketId = Integer.parseInt(parts[1]);
                        server.resolveTicket(ticketId, this.nickname);
                    } catch (NumberFormatException e) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Invalid ticket ID format."));
                    }
                }
                break;

            case Protocol.CMD_TICKET_STATUS:
                if (parts.length >= 3) {
                    try {
                        int ticketId = Integer.parseInt(parts[1]);
                        String statusStr = parts[2].trim().toUpperCase();
                        Ticket.Status newStatus = Ticket.Status.valueOf(statusStr);
                        server.updateTicketStatus(ticketId, newStatus, this.nickname);
                    } catch (IllegalArgumentException e) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Invalid ticket ID or status format."));
                    }
                }
                break;

            case Protocol.CMD_TICKET_DELETE:
                if (parts.length >= 2) {
                    try {
                        int ticketId = Integer.parseInt(parts[1]);
                        server.deleteTicket(ticketId, this.nickname);
                    } catch (NumberFormatException e) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Invalid ticket ID format."));
                    }
                }
                break;

            case Protocol.CMD_TICKET_EDIT:
                if (parts.length >= 4) {
                    try {
                        int ticketId = Integer.parseInt(parts[1]);
                        String category = parts[2].trim();
                        String subject = parts[3].trim();
                        String customRequester = (parts.length >= 5 && !parts[4].trim().isEmpty()) ? parts[4].trim() : this.nickname;
                        if (!category.isEmpty() && !subject.isEmpty()) {
                            server.editTicket(ticketId, category, subject, customRequester, this.nickname);
                        } else {
                            sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Ticket category and subject cannot be blank."));
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Invalid ticket ID format."));
                    }
                }
                break;

            case Protocol.CMD_REACT:
                if (parts.length >= 3) {
                    try {
                        int msgId = Integer.parseInt(parts[1]);
                        String reaction = parts[2].trim();
                        server.handleReaction(msgId, reaction, this.nickname);
                    } catch (NumberFormatException ignored) {}
                }
                break;

            default:
                sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Unknown command: " + command));
                break;
        }
    }

    private void handleNameChange(String newNick) {
        String validationError = server.validateNickname(newNick);
        if (validationError != null) {
            sendMessage(Protocol.buildPacket(Protocol.RSP_NAME_ERR, validationError));
            return;
        }

        String oldNick = this.nickname;
        this.nickname = newNick;
        sendMessage(Protocol.buildPacket(Protocol.RSP_NAME_OK, newNick));
        server.broadcastSystemMessage(oldNick + " renamed themselves to " + newNick);
        server.broadcastUserList();
        server.syncTicketsToClient(this);
    }

    /**
     * Cleans up client socket, streams, and unregisters from server.
     */
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        running = false;
        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}

        if (nickname != null) {
            server.unregisterClient(this);
            if (server.isRunning()) {
                server.broadcastSystemMessage(nickname + " left PeerDesk.");
                server.broadcastUserList();
            }
        }
    }
}
