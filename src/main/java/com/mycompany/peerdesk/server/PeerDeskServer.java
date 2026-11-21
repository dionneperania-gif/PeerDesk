package com.mycompany.peerdesk.server;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.protocol.Protocol;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Core TCP Server for PeerDesk.
 * Manages client connections, nickname validation, message broadcasting,
 * and synchronized peer help ticket state.
 */
public class PeerDeskServer {

    private static final Logger LOGGER = Logger.getLogger(PeerDeskServer.class.getName());

    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final List<Ticket> tickets = Collections.synchronizedList(new ArrayList<>());
    private final List<String> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger ticketCounter = new AtomicInteger(100);
    private final AtomicInteger messageCounter = new AtomicInteger(1001);
    private final java.util.Map<Integer, java.util.Map<String, Integer>> reactions = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String STORAGE_FILE = System.getProperty("user.home") + File.separator + "peerdesk_tickets.dat";
    private static final String CHAT_STORAGE_FILE = System.getProperty("user.home") + File.separator + "peerdesk_chat_history.dat";
    private final boolean persistenceEnabled;
    private ServerEventListener listener;

    public PeerDeskServer(int port) {
        this(port, true);
    }

    public PeerDeskServer(int port, boolean persistenceEnabled) {
        this.port = port;
        this.persistenceEnabled = persistenceEnabled;
    }

    public void setListener(ServerEventListener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public List<ClientHandler> getConnectedClients() {
        return Collections.unmodifiableList(clients);
    }

    public void log(String message) {
        String timestamp = Protocol.getCurrentTimestamp();
        System.out.println("[" + timestamp + "] [SERVER] " + message);
        LOGGER.info(message);
        if (listener != null) {
            listener.onLog(timestamp, message);
        }
    }

    /**
     * Starts listening for client socket connections.
     */
    public synchronized void start() throws IOException {
        if (running) return;

        serverSocket = new ServerSocket(port);
        threadPool = Executors.newCachedThreadPool();
        running = true;
        log("PeerDesk Server listening on port " + port);

        // Load previously persisted tickets from disk (empty by default on clean start)
        loadPersistedTickets();
        loadPersistedChat();

        // Accept loop in background thread
        new Thread(this::acceptLoop, "Server-Accept-Thread").start();
    }

    /**
     * Loads tickets previously saved to disk.
     * Does NOT generate any sample or dummy tickets.
     */
    private void loadPersistedTickets() {
        if (!persistenceEnabled) return;
        File file = new File(STORAGE_FILE);
        if (!file.exists()) {
            return;
        }

        synchronized (tickets) {
            tickets.clear();
            int maxId = 99;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    Ticket t = Ticket.deserialize(line);
                    if (t != null) {
                        tickets.add(t);
                        if (t.getId() > maxId) {
                            maxId = t.getId();
                        }
                    }
                }
                ticketCounter.set(maxId + 1);
                log("Restored " + tickets.size() + " ticket(s) from persistent storage.");
            } catch (IOException e) {
                log("Warning: Failed to load tickets from " + STORAGE_FILE + ": " + e.getMessage());
            }
        }
    }

    /**
     * Persists all current tickets to disk.
     */
    private void saveTicketsToStorage() {
        if (!persistenceEnabled) return;
        File file = new File(STORAGE_FILE);
        synchronized (tickets) {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (Ticket t : tickets) {
                    writer.write(t.serialize());
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                log("Warning: Failed to save tickets to " + STORAGE_FILE + ": " + e.getMessage());
            }
        }
    }

    private void loadPersistedChat() {
        if (!persistenceEnabled) return;
        File file = new File(CHAT_STORAGE_FILE);
        if (!file.exists()) return;

        synchronized (chatHistory) {
            chatHistory.clear();
            int maxMsgId = 1000;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    chatHistory.add(line);
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2) {
                        try {
                            int mid = Integer.parseInt(parts[1]);
                            if (mid > maxMsgId) maxMsgId = mid;
                        } catch (NumberFormatException ignored) {}
                    }
                }
                messageCounter.set(maxMsgId + 1);
                log("Restored " + chatHistory.size() + " chat message(s) from persistent storage.");
            } catch (IOException e) {
                log("Warning: Failed to load chat history: " + e.getMessage());
            }
        }
    }

    private void saveChatToStorage() {
        if (!persistenceEnabled) return;
        File file = new File(CHAT_STORAGE_FILE);
        synchronized (chatHistory) {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (String packet : chatHistory) {
                    writer.write(packet);
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                log("Warning: Failed to save chat history: " + e.getMessage());
            }
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                log("New client connected from: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.submit(handler);
            } catch (IOException e) {
                if (running) {
                    log("Accept loop exception: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Validates nickname: non-blank, 2-16 chars, no delimiter chars, unique across active users.
     * Returns null if valid, or an error string if invalid.
     */
    public synchronized String validateNickname(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return "Nickname cannot be blank.";
        }
        String clean = nick.trim();
        if (clean.length() < Protocol.MIN_NICK_LENGTH || clean.length() > Protocol.MAX_NICK_LENGTH) {
            return "Nickname must be between " + Protocol.MIN_NICK_LENGTH + " and " + Protocol.MAX_NICK_LENGTH + " characters.";
        }
        if (clean.contains("|") || clean.contains("~") || clean.contains("@") || clean.contains(",")) {
            return "Nickname contains illegal characters (| ~ @ ,).";
        }
        for (ClientHandler client : clients) {
            if (clean.equalsIgnoreCase(client.getNickname())) {
                return "Nickname '" + clean + "' is already in use by another peer.";
            }
        }
        return null;
    }

    public void registerClient(ClientHandler client) {
        clients.add(client);
        log("Client registered: " + client.getNickname() + " (Total active: " + clients.size() + ")");
        if (listener != null) {
            listener.onClientCountChanged(clients.size());
        }
    }

    public void unregisterClient(ClientHandler client) {
        clients.remove(client);
        log("Client unregistered: " + client.getNickname() + " (Total active: " + clients.size() + ")");
        if (listener != null) {
            listener.onClientCountChanged(clients.size());
        }
    }

    /**
     * Broadcasts a chat message with a unique message ID to all connected clients
     * and saves it to chat history for late-joining peers.
     */
    public void broadcastChatMessage(String sender, String message) {
        if (message.length() > Protocol.MAX_MESSAGE_LENGTH) {
            message = message.substring(0, Protocol.MAX_MESSAGE_LENGTH);
        }
        int msgId = messageCounter.getAndIncrement();
        String timestamp = Protocol.getCurrentTimestamp();
        String safeText = Protocol.sanitizeChat(message);
        String packet = Protocol.buildPacket(Protocol.BCAST_CHAT, String.valueOf(msgId), timestamp, sender, safeText);
        chatHistory.add(packet);
        saveChatToStorage();
        broadcastRaw(packet);
        log("Chat #" + msgId + " [" + sender + "]: " + safeText);
    }

    /**
     * Synchronizes recent chat message history and their reactions to a newly joined client.
     */
    public void syncChatHistoryToClient(ClientHandler client) {
        synchronized (chatHistory) {
            for (String chatPacket : chatHistory) {
                client.sendMessage(chatPacket);
            }
        }
        // Also sync any reactions recorded on those messages
        for (java.util.Map.Entry<Integer, java.util.Map<String, Integer>> entry : reactions.entrySet()) {
            int msgId = entry.getKey();
            for (java.util.Map.Entry<String, Integer> rEntry : entry.getValue().entrySet()) {
                String reactPacket = Protocol.buildPacket(Protocol.BCAST_REACT, String.valueOf(msgId), rEntry.getKey(), String.valueOf(rEntry.getValue()));
                client.sendMessage(reactPacket);
            }
        }
    }

    /**
     * Records a message reaction and broadcasts the updated reaction count to all clients.
     */
    public void handleReaction(int msgId, String reactionType, String senderNick) {
        reactions.putIfAbsent(msgId, new java.util.concurrent.ConcurrentHashMap<>());
        java.util.Map<String, Integer> msgReactions = reactions.get(msgId);
        int newCount = msgReactions.compute(reactionType, (k, v) -> (v == null) ? 1 : v + 1);

        String packet = Protocol.buildPacket(Protocol.BCAST_REACT, String.valueOf(msgId), reactionType, String.valueOf(newCount));
        broadcastRaw(packet);
        log("Reaction on #" + msgId + " by " + senderNick + ": [" + reactionType + "] total: " + newCount);
    }

    /**
     * Broadcasts a system announcement to all connected clients and persists it in chat history.
     */
    public void broadcastSystemMessage(String message) {
        String timestamp = Protocol.getCurrentTimestamp();
        String packet = Protocol.buildPacket(Protocol.BCAST_SYS, timestamp, message);
        chatHistory.add(packet);
        saveChatToStorage();
        broadcastRaw(packet);
        log("System: " + message);
    }

    /**
     * Sends the current online user list to all connected clients.
     */
    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            String nick = clients.get(i).getNickname();
            if (nick != null) {
                if (sb.length() > 0) sb.append(",");
                sb.append(nick);
            }
        }
        String packet = Protocol.buildPacket(Protocol.BCAST_USER_LIST, sb.toString());
        broadcastRaw(packet);
    }

    /**
     * Sends the complete shared ticket board to all currently connected clients.
     */
    public void broadcastTicketList() {
        String packet = buildTicketListPacket();
        broadcastRaw(packet);
    }

    /**
     * Synchronizes all existing tickets to a client upon connect / reconnect.
     */
    public void syncTicketsToClient(ClientHandler client) {
        client.sendMessage(buildTicketListPacket());
    }

    private String buildTicketListPacket() {
        StringBuilder sb = new StringBuilder();
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (sb.length() > 0) {
                    sb.append(Protocol.TICKET_DELIMITER);
                }
                sb.append(t.serialize());
            }
        }
        return Protocol.buildPacket(Protocol.BCAST_TICKETS, sb.toString());
    }

    private void broadcastRaw(String packet) {
        for (ClientHandler client : clients) {
            client.sendMessage(packet);
        }
    }

    // --- Original Theme Feature 1 & 2: Ticket Management Workflows ---

    public void createTicket(String peerId, String requester, String category, String subject) {
        int id = ticketCounter.getAndIncrement();
        Ticket ticket = new Ticket(id, peerId, requester, category, subject, "None", Ticket.Status.OPEN, Protocol.getCurrentTimestamp());
        tickets.add(ticket);
        saveTicketsToStorage();
        broadcastTicketList();
        broadcastSystemMessage(requester + " submitted ticket #" + id + ": [" + category + "] " + subject);
    }

    public void editTicket(int ticketId, String newCategory, String newSubject, String newRequester, String editorNick) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    if (newCategory != null && !newCategory.trim().isEmpty()) {
                        t.setCategory(newCategory.trim());
                    }
                    if (newSubject != null && !newSubject.trim().isEmpty()) {
                        t.setSubject(newSubject.trim());
                    }
                    if (newRequester != null && !newRequester.trim().isEmpty()) {
                        t.setRequester(newRequester.trim());
                    }
                    saveTicketsToStorage();
                    broadcastTicketList();
                    broadcastSystemMessage(editorNick + " updated ticket #" + ticketId + " details.");
                    return;
                }
            }
        }
    }

    public void claimTicket(int ticketId, String helperNick) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    String current = t.getHelper();
                    List<String> list = new ArrayList<>();
                    if (current != null && !current.trim().isEmpty() && !"None".equalsIgnoreCase(current.trim())) {
                        for (String part : current.split(",")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                list.add(trimmed);
                            }
                        }
                    }

                    boolean removed = false;
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).equalsIgnoreCase(helperNick)) {
                            list.remove(i);
                            removed = true;
                            break;
                        }
                    }

                    if (removed) {
                        if (list.isEmpty()) {
                            t.setHelper("None");
                            t.setStatus(Ticket.Status.OPEN);
                        } else {
                            t.setHelper(String.join(", ", list));
                        }
                        saveTicketsToStorage();
                        broadcastTicketList();
                        broadcastSystemMessage(helperNick + " stopped assisting ticket #" + ticketId);
                    } else {
                        list.add(helperNick);
                        t.setHelper(String.join(", ", list));
                        t.setStatus(Ticket.Status.IN_PROGRESS);
                        saveTicketsToStorage();
                        broadcastTicketList();
                        broadcastSystemMessage(helperNick + " is now assisting with ticket #" + ticketId + " (" + t.getRequester() + ")");
                    }
                    return;
                }
            }
        }
    }

    public void updateTicketStatus(int ticketId, Ticket.Status newStatus, String updaterNick) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    t.setStatus(newStatus);
                    if (newStatus == Ticket.Status.OPEN) {
                        t.setHelper("None");
                    } else if (newStatus == Ticket.Status.IN_PROGRESS && "None".equalsIgnoreCase(t.getHelper())) {
                        t.setHelper(updaterNick);
                    }
                    saveTicketsToStorage();
                    broadcastTicketList();
                    broadcastSystemMessage("Ticket #" + ticketId + " status updated to " + newStatus.name() + " by " + updaterNick);
                    return;
                }
            }
        }
    }

    public void resolveTicket(int ticketId, String solverNick) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    t.setStatus(Ticket.Status.RESOLVED);
                    saveTicketsToStorage();
                    broadcastTicketList();
                    broadcastSystemMessage("Ticket #" + ticketId + " has been marked RESOLVED by " + solverNick);
                    return;
                }
            }
        }
    }

    public void deleteTicket(int ticketId, String requesterNick) {
        synchronized (tickets) {
            Ticket target = null;
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    target = t;
                    break;
                }
            }
            if (target == null) {
                return;
            }
            if (!requesterNick.equalsIgnoreCase(target.getRequester())
                    && !requesterNick.equalsIgnoreCase(target.getPeerId())) {
                log("Warning: " + requesterNick + " attempted to delete ticket #" + ticketId + " without permission.");
                return;
            }
            tickets.removeIf(t -> t.getId() == ticketId);
        }
        saveTicketsToStorage();
        broadcastTicketList();
        broadcastSystemMessage("Ticket #" + ticketId + " was removed by " + requesterNick);
    }

    /**
     * Stops the server, closes all client connections, and releases ports.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        log("Stopping PeerDesk Server...");

        for (ClientHandler client : clients) {
            client.sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Server is shutting down."));
            client.close();
        }
        clients.clear();

        if (threadPool != null) {
            threadPool.shutdownNow();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        log("PeerDesk Server stopped.");
    }
}
