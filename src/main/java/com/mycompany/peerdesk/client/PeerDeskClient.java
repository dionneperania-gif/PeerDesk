package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.protocol.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Handles the client-side TCP socket connection to PeerDeskServer.
 * Performs all network operations on dedicated background threads to ensure
 * the Swing GUI remains completely responsive.
 */
public class PeerDeskClient {

    private static final Logger LOGGER = Logger.getLogger(PeerDeskClient.class.getName());

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private volatile boolean connected = false;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private String currentNickname;
    private final Object writeLock = new Object();

    private PeerDeskClientListener listener;
    private boolean useEdt = true;

    public void setUseEdt(boolean useEdt) {
        this.useEdt = useEdt;
    }

    public void setListener(PeerDeskClientListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getCurrentNickname() {
        return currentNickname;
    }

    /**
     * Connects to the server asynchronously to prevent freezing the GUI.
     */
    public void connectAsync(String host, int port, String requestedNick) {
        if (!connecting.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                socket = new Socket();
                // Use protocol-defined connection timeout
                socket.connect(new InetSocketAddress(host, port), Protocol.CONNECTION_TIMEOUT_MS);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                // Send initial handshake
                sendRaw(Protocol.buildPacket(Protocol.CMD_CONNECT, requestedNick));

                // Wait for initial handshake response synchronously on this background thread
                String response;
                while ((response = in.readLine()) != null) {
                    String[] parts = response.split("\\|", 2);
                    if (Protocol.RSP_AUTH_OK.equals(parts[0])) {
                        connected = true;
                        this.currentNickname = parts.length > 1 ? parts[1] : requestedNick;
                        dispatch(() -> {
                            if (listener != null) listener.onConnected(currentNickname);
                        });
                        break;
                    } else if (Protocol.RSP_AUTH_ERR.equals(parts[0])) {
                        String reason = parts.length > 1 ? parts[1] : "Authentication failed.";
                        closeResources();
                        dispatch(() -> {
                            if (listener != null) listener.onAuthFailed(reason);
                        });
                        return;
                    }
                }

                if (!connected) {
                    throw new IOException("Server closed connection during handshake.");
                }

                LOGGER.info("Connected to server " + host + ":" + port + " as " + currentNickname);

                // Start background reader loop
                listenerThread = new Thread(this::listenLoop, "PeerDeskClient-Reader");
                listenerThread.setDaemon(true);
                listenerThread.start();

            } catch (IOException ex) {
                closeResources();
                dispatch(() -> {
                    if (listener != null) listener.onAuthFailed("Connection error: " + ex.getMessage());
                });
            } finally {
                connecting.set(false);
            }
        }, "PeerDeskClient-ConnectThread").start();
    }

    /**
     * Continuous background loop reading packets from server.
     */
    private void listenLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    handleIncomingPacket(line);
                }
            }
        } catch (IOException e) {
            // Disconnection occurred
        } finally {
            if (connected) {
                disconnect("Connection lost from server.");
            }
        }
    }

    /**
     * Parses incoming protocol packets and forwards them to GUI listeners via EDT.
     */
    private void handleIncomingPacket(String packet) {
        String[] parts = packet.split("\\|", -1);
        String command = parts[0];

        switch (command) {
            case Protocol.BCAST_CHAT:
                if (parts.length >= 5) {
                    try {
                        int msgId = Integer.parseInt(parts[1]);
                        String time = parts[2];
                        String sender = parts[3];
                        String text = parts[4];
                        dispatch(() -> {
                            if (listener != null) listener.onChatMessage(msgId, time, sender, text);
                        });
                    } catch (NumberFormatException e) {
                        String time = parts[1];
                        String sender = parts[2];
                        String text = parts[3];
                        dispatch(() -> {
                            if (listener != null) listener.onChatMessage(0, time, sender, text);
                        });
                    }
                } else if (parts.length >= 4) {
                    String time = parts[1];
                    String sender = parts[2];
                    String text = parts[3];
                    dispatch(() -> {
                        if (listener != null) listener.onChatMessage(0, time, sender, text);
                    });
                }
                break;

            case Protocol.BCAST_REACT:
                if (parts.length >= 4) {
                    try {
                        int msgId = Integer.parseInt(parts[1]);
                        String reaction = parts[2];
                        int count = Integer.parseInt(parts[3]);
                        dispatch(() -> {
                            if (listener != null) listener.onMessageReactionUpdated(msgId, reaction, count);
                        });
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case Protocol.BCAST_SYS:
                if (parts.length >= 3) {
                    String time = parts[1];
                    String msg = parts[2];
                    dispatch(() -> {
                        if (listener != null) listener.onSystemMessage(time, msg);
                    });
                }
                break;

            case Protocol.BCAST_USER_LIST:
                List<String> users = new ArrayList<>();
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    users.addAll(Arrays.asList(parts[1].split(",")));
                }
                dispatch(() -> {
                    if (listener != null) listener.onUserListUpdated(users);
                });
                break;

            case Protocol.BCAST_TICKETS:
                List<Ticket> tickets = new ArrayList<>();
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    String[] rawTickets = parts[1].split(Protocol.TICKET_DELIMITER);
                    for (String raw : rawTickets) {
                        Ticket t = Ticket.deserialize(raw);
                        if (t != null) {
                            tickets.add(t);
                        }
                    }
                }
                dispatch(() -> {
                    if (listener != null) listener.onTicketListUpdated(tickets);
                });
                break;

            case Protocol.RSP_NAME_OK:
                if (parts.length > 1) {
                    this.currentNickname = parts[1];
                    dispatch(() -> {
                        if (listener != null) listener.onNameChangeSuccess(currentNickname);
                    });
                }
                break;

            case Protocol.RSP_NAME_ERR:
                String err = parts.length > 1 ? parts[1] : "Could not change nickname.";
                dispatch(() -> {
                    if (listener != null) listener.onNameChangeFailed(err);
                });
                break;

            case Protocol.RSP_SERVER_ERR:
                String srvErr = parts.length > 1 ? parts[1] : "Unknown server error.";
                dispatch(() -> {
                    if (listener != null) listener.onServerMessage(srvErr, true);
                });
                break;

            default:
                break;
        }
    }

    /**
     * Sends a chat message or handles client-side commands.
     */
    public void sendChat(String rawInput) {
        if (!connected || rawInput == null) return;
        String text = rawInput.trim();
        if (text.isEmpty()) return;

        // Client-side message length enforcement
        if (!text.startsWith("/") && text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            dispatch(() -> {
                if (listener != null) listener.onServerMessage(
                        "Message too long (max " + Protocol.MAX_MESSAGE_LENGTH + " characters).", true);
            });
            return;
        }

        // Command handling
        if (text.startsWith("/")) {
            handleSlashCommand(text);
        } else {
            sendRaw(Protocol.buildPacket(Protocol.CMD_CHAT, text));
        }
    }

    private void handleSlashCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/name":
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    sendRaw(Protocol.buildPacket(Protocol.CMD_NAME, parts[1].trim()));
                } else {
                    dispatch(() -> {
                        if (listener != null) listener.onServerMessage("Usage: /name <newname>", false);
                    });
                }
                break;

            case "/exit":
                disconnect("You disconnected.");
                break;

            case "/help":
                dispatch(() -> {
                    if (listener != null) {
                        String help = "Available commands:\n" +
                                      "- /name <newname> : Change your active nickname\n" +
                                      "- /exit           : Safely exit and disconnect\n" +
                                      "- /help           : Display this command cheat sheet";
                        listener.onServerMessage(help, false);
                    }
                });
                break;

            default:
                dispatch(() -> {
                    if (listener != null) {
                        listener.onServerMessage("Unknown command '" + cmd + "'. Type /help for assistance.", false);
                    }
                });
                break;
        }
    }

    // --- Ticket Action Methods ---

    public void createTicket(String category, String subject) {
        createTicket(category, subject, null);
    }

    public void createTicket(String category, String subject, String customRequester) {
        if (!connected) return;
        if (customRequester != null && !customRequester.trim().isEmpty()) {
            sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_NEW, category, subject, customRequester.trim()));
        } else {
            sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_NEW, category, subject));
        }
    }

    public void updateTicketStatus(int ticketId, String status) {
        if (!connected) return;
        sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_STATUS, String.valueOf(ticketId), status));
    }

    public void claimTicket(int ticketId) {
        if (!connected) return;
        sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_CLAIM, String.valueOf(ticketId)));
    }

    public void resolveTicket(int ticketId) {
        if (!connected) return;
        sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_RESOLVE, String.valueOf(ticketId)));
    }

    public void editTicket(int ticketId, String category, String subject, String customRequester) {
        if (!connected) return;
        if (customRequester != null && !customRequester.trim().isEmpty()) {
            sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_EDIT, String.valueOf(ticketId), category, subject, customRequester.trim()));
        } else {
            sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_EDIT, String.valueOf(ticketId), category, subject));
        }
    }

    public void deleteTicket(int ticketId) {
        if (!connected) return;
        sendRaw(Protocol.buildPacket(Protocol.CMD_TICKET_DELETE, String.valueOf(ticketId)));
    }

    public void sendReaction(int messageId, String reactionType) {
        if (!connected) return;
        sendRaw(Protocol.buildPacket(Protocol.CMD_REACT, String.valueOf(messageId), reactionType));
    }

    /**
     * Synchronized packet sending.
     */
    private void sendRaw(String packet) {
        synchronized (writeLock) {
            if (out != null) {
                out.println(packet);
                out.flush();
            }
        }
    }

    /**
     * Gracefully disconnects from the server and cleans up resources.
     */
    public void disconnect(String reason) {
        if (!connected && socket == null) return;
        connected = false;

        // Try to inform server before closing
        try {
            sendRaw(Protocol.buildPacket(Protocol.CMD_EXIT));
        } catch (Exception ignored) {}

        closeResources();

        dispatch(() -> {
            if (listener != null) {
                listener.onDisconnected(reason);
            }
        });
    }

    private void closeResources() {
        connected = false;
        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }

    private void dispatch(Runnable runnable) {
        if (!useEdt || SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
