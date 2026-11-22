package com.mycompany.peerdesk.server;

import com.mycompany.peerdesk.model.HelperRating;
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
    private static final String STUDENTS_STORAGE_FILE = System.getProperty("user.home") + File.separator + "peerdesk_students.dat";
    private static final String RATINGS_STORAGE_FILE = System.getProperty("user.home") + File.separator + "peerdesk_ratings.dat";
    private final java.util.Map<String, String> registeredStudents = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<HelperRating> ratings = Collections.synchronizedList(new ArrayList<>());
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
        loadPersistedStudents();
        loadPersistedRatings();

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

    private void loadPersistedStudents() {
        if (!persistenceEnabled) return;
        File file = new File(STUDENTS_STORAGE_FILE);
        if (!file.exists()) return;

        registeredStudents.clear();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("~", 2);
                if (parts.length >= 2) {
                    registeredStudents.put(parts[0].trim(), parts[1].trim());
                }
            }
            log("Restored " + registeredStudents.size() + " registered student record(s).");
        } catch (IOException e) {
            log("Warning: Failed to load registered students: " + e.getMessage());
        }
    }

    private void saveStudentsToStorage() {
        if (!persistenceEnabled) return;
        File file = new File(STUDENTS_STORAGE_FILE);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (java.util.Map.Entry<String, String> entry : registeredStudents.entrySet()) {
                writer.write(entry.getKey() + "~" + entry.getValue());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            log("Warning: Failed to save students: " + e.getMessage());
        }
    }

    private void loadPersistedRatings() {
        if (!persistenceEnabled) return;
        File file = new File(RATINGS_STORAGE_FILE);
        if (!file.exists()) return;

        synchronized (ratings) {
            ratings.clear();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    HelperRating r = HelperRating.deserialize(line);
                    if (r != null) {
                        ratings.add(r);
                    }
                }
                log("Restored " + ratings.size() + " helper rating(s) from persistent storage.");
            } catch (IOException e) {
                log("Warning: Failed to load ratings from " + RATINGS_STORAGE_FILE + ": " + e.getMessage());
            }
        }
    }

    private void saveRatingsToStorage() {
        if (!persistenceEnabled) return;
        File file = new File(RATINGS_STORAGE_FILE);
        synchronized (ratings) {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (HelperRating r : ratings) {
                    writer.write(r.serialize());
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                log("Warning: Failed to save ratings to " + RATINGS_STORAGE_FILE + ": " + e.getMessage());
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
     * Normalizes a Student ID string (e.g. "20251124" -> "2025-1124").
     */
    public static String normalizeStudentId(String id) {
        if (id == null) return null;
        String clean = id.trim().replaceAll("\\s+", "");
        if (clean.matches("^\\d{8}$")) {
            return clean.substring(0, 4) + "-" + clean.substring(4);
        }
        return clean;
    }

    /**
     * Validates student registration: nickname format and studentId constraints.
     * Student ID must be 8 digits (e.g. 2025-1124), only numbers and symbols, no letters.
     * If already registered, the submitted name must match the registered name.
     */
    public synchronized String validateRegistration(String nick, String studentId) {
        String nickError = validateNicknameFormat(nick);
        if (nickError != null) return nickError;

        if (studentId != null) {
            String idError = validateStudentId(studentId);
            if (idError != null) return idError;

            String normalizedId = normalizeStudentId(studentId);
            String cleanNick = nick.trim();
            String registeredName = registeredStudents.get(normalizedId);
            if (registeredName != null) {
                if (!registeredName.equalsIgnoreCase(cleanNick)) {
                    return "Wrong Name or Student ID! The Student ID '" + normalizedId
                            + "' is registered to '" + registeredName + "'. Please check your credentials.";
                }
            } else {
                // First-time registration for this Student ID
                registeredStudents.put(normalizedId, cleanNick);
                saveStudentsToStorage();
            }
        } else {
            // Legacy single-identifier mode: nickname must be unique across active clients
            for (ClientHandler client : clients) {
                if (nick.trim().equalsIgnoreCase(client.getNickname())) {
                    return "Nickname '" + nick.trim() + "' is already in use by another peer.";
                }
            }
        }
        return null;
    }

    /**
     * Legacy validation method maintained for backward compatibility.
     */
    public synchronized String validateNickname(String nick) {
        return validateRegistration(nick, null);
    }

    /**
     * Validates nickname format: non-blank, 2-16 chars, no delimiter chars.
     */
    public synchronized String validateNicknameFormat(String nick) {
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
        return null;
    }

    /**
     * Validates Student ID: non-blank, exactly 8 digits (e.g. 2025-1124), NO letters, unique among online clients.
     */
    public synchronized String validateStudentId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "Student ID cannot be blank.";
        }
        String clean = normalizeStudentId(id);
        if (clean.contains("|") || clean.contains("~") || clean.contains("@") || clean.contains(",")) {
            return "Student ID contains illegal characters (| ~ @ ,).";
        }
        for (char c : clean.toCharArray()) {
            if (Character.isLetter(c)) {
                return "Student ID must contain only numbers and symbols, no letters.";
            }
        }
        if (!clean.matches("^\\d{4}-\\d{4}$")) {
            return "Invalid Student ID format. Must be 8 digits (e.g. 2025-1124).";
        }
        for (ClientHandler client : clients) {
            if (clean.equalsIgnoreCase(client.getStudentId())) {
                return "Student ID '" + clean + "' is already logged in on another session.";
            }
        }
        return null;
    }

    public void updateStudentName(String studentId, String newName) {
        String normalizedId = normalizeStudentId(studentId);
        if (normalizedId != null && newName != null && !newName.trim().isEmpty()) {
            registeredStudents.put(normalizedId, newName.trim());
            saveStudentsToStorage();
        }
    }

    public void registerClient(ClientHandler client) {
        clients.add(client);
        String name = client.getNickname();
        if (client.getStudentId() != null && !client.getStudentId().equalsIgnoreCase(name)) {
            name = name + " (" + client.getStudentId() + ")";
        }
        log("Client registered: " + name + " (Total active: " + clients.size() + ")");
        if (listener != null) {
            listener.onClientCountChanged(clients.size());
        }
    }

    public void unregisterClient(ClientHandler client) {
        clients.remove(client);
        String name = client.getNickname();
        if (client.getStudentId() != null && !client.getStudentId().equalsIgnoreCase(name)) {
            name = name + " (" + client.getStudentId() + ")";
        }
        log("Client unregistered: " + name + " (Total active: " + clients.size() + ")");
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
            ClientHandler c = clients.get(i);
            String nick = c.getNickname();
            String sid = c.getStudentId();
            if (nick != null) {
                if (sb.length() > 0) sb.append(",");
                if (sid != null && !sid.isEmpty() && !sid.equalsIgnoreCase(nick)) {
                    sb.append(nick).append(" (").append(sid).append(")");
                } else {
                    sb.append(nick);
                }
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
        updateTicketStatus(ticketId, newStatus, updaterNick, updaterNick);
    }

    public void updateTicketStatus(int ticketId, Ticket.Status newStatus, String updaterNick, String updaterStudentId) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    if (newStatus == Ticket.Status.RESOLVED) {
                        // Resolving is restricted to requester only
                        resolveTicket(ticketId, updaterNick, updaterStudentId);
                        return;
                    }
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
        resolveTicket(ticketId, solverNick, solverNick);
    }

    public void resolveTicket(int ticketId, String solverNick, String solverStudentId) {
        synchronized (tickets) {
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    // Check if solver is the original requester (by Student ID or name)
                    boolean isRequester = (solverStudentId != null && solverStudentId.equalsIgnoreCase(t.getPeerId()))
                            || solverNick.equalsIgnoreCase(t.getRequester())
                            || solverNick.equalsIgnoreCase(t.getPeerId())
                            || (t.getRequester() != null && t.getRequester().contains("(" + solverStudentId + ")"));

                    if (!isRequester) {
                        log("Warning: " + solverNick + " attempted to resolve ticket #" + ticketId + " without permission.");
                        for (ClientHandler client : clients) {
                            if (solverNick.equalsIgnoreCase(client.getNickname())) {
                                client.sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Only the original requester can mark this ticket as RESOLVED."));
                                break;
                            }
                        }
                        return;
                    }

                    t.setStatus(Ticket.Status.RESOLVED);
                    saveTicketsToStorage();
                    broadcastTicketList();
                    broadcastSystemMessage("Ticket #" + ticketId + " has been marked RESOLVED by " + solverNick);
                    return;
                }
            }
        }
    }

    /**
     * Submits a rating (1-5 stars) for a resolved ticket.
     * Enforces rules:
     * 1. Ticket must be in RESOLVED status.
     * 2. Only original requester can submit.
     * 3. Helper cannot rate themselves.
     * 4. No duplicate ratings for the same ticket.
     * 5. Single rating is credited to all assigned helpers on the ticket.
     */
    public String submitRating(int ticketId, int stars, String requesterNick, String requesterStudentId) {
        if (stars < 1 || stars > 5) {
            return "Rating must be between 1 and 5 stars.";
        }

        synchronized (tickets) {
            Ticket ticket = null;
            for (Ticket t : tickets) {
                if (t.getId() == ticketId) {
                    ticket = t;
                    break;
                }
            }

            if (ticket == null) {
                return "Ticket #" + ticketId + " does not exist.";
            }

            if (ticket.getStatus() != Ticket.Status.RESOLVED) {
                return "Only resolved tickets can be rated.";
            }

            // Verify caller is the original requester
            boolean isRequester = (requesterStudentId != null && requesterStudentId.equalsIgnoreCase(ticket.getPeerId()))
                    || requesterNick.equalsIgnoreCase(ticket.getRequester())
                    || requesterNick.equalsIgnoreCase(ticket.getPeerId())
                    || (ticket.getRequester() != null && ticket.getRequester().contains("(" + requesterStudentId + ")"));
            if (!isRequester) {
                return "Only the original requester can rate this ticket.";
            }

            // Check helpers assigned
            String helperField = ticket.getHelper();
            if (helperField == null || helperField.trim().isEmpty() || "None".equalsIgnoreCase(helperField.trim())) {
                return "No helper was assigned to this ticket to rate.";
            }

            List<String> helperList = new ArrayList<>();
            for (String h : helperField.split(",")) {
                String trimmed = h.trim();
                if (!trimmed.isEmpty() && !"None".equalsIgnoreCase(trimmed)) {
                    helperList.add(trimmed);
                }
            }

            if (helperList.isEmpty()) {
                return "No helper was assigned to this ticket to rate.";
            }

            // Prevent helper from rating themselves / their own assistance
            for (String h : helperList) {
                if (h.equalsIgnoreCase(requesterNick) || (requesterStudentId != null && h.equalsIgnoreCase(requesterStudentId))) {
                    return "You cannot rate yourself or your own assistance.";
                }
            }

            // Prevent duplicate ratings for this ticket
            synchronized (ratings) {
                for (HelperRating r : ratings) {
                    if (r.getTicketId() == ticketId) {
                        return "This ticket has already been rated.";
                    }
                }

                // Award rating to all helpers
                String timestamp = Protocol.getCurrentTimestamp();
                for (String h : helperList) {
                    ratings.add(new HelperRating(ticketId, h, ticket.getRequester(), stars, timestamp));
                }
                saveRatingsToStorage();
            }

            String helpersPhrase;
            if (helperList.size() == 1) {
                helpersPhrase = helperList.get(0);
            } else if (helperList.size() == 2) {
                helpersPhrase = helperList.get(0) + " and " + helperList.get(1);
            } else {
                helpersPhrase = helperList.get(0) + " and the other helpers";
            }
            log("Rating submitted for ticket #" + ticketId + " (" + stars + " stars) to [" + String.join(", ", helperList) + "] by " + requesterNick);
            broadcastSystemMessage(requesterNick + " rated " + helpersPhrase + " " + stars + " star" + (stars > 1 ? "s" : "") + " for ticket #" + ticketId + ".");
            return null; // success
        }
    }

    /**
     * Gets rating summary for a classmate.
     * Returns: [classmateNick, avgStr, totalCountStr, detailsStr]
     */
    public String[] getRatingsForClassmate(String classmateIdentifier) {
        if (classmateIdentifier == null) return new String[]{"", "0.0", "0", ""};
        String target = classmateIdentifier.trim();
        // Also if identifier has "Name (ID)", extract name and ID
        String targetName = target;
        String targetId = target;
        if (target.contains("(") && target.endsWith(")")) {
            int openIdx = target.indexOf('(');
            targetName = target.substring(0, openIdx).trim();
            targetId = target.substring(openIdx + 1, target.length() - 1).trim();
        }

        List<HelperRating> matched = new ArrayList<>();
        int sum = 0;
        synchronized (ratings) {
            for (HelperRating r : ratings) {
                String h = r.getHelperName();
                if (h.equalsIgnoreCase(target) || h.equalsIgnoreCase(targetName) || h.equalsIgnoreCase(targetId)) {
                    matched.add(r);
                    sum += r.getStars();
                }
            }
        }

        int count = matched.size();
        double avg = count > 0 ? (double) sum / count : 0.0;
        String avgStr = String.format(java.util.Locale.US, "%.1f", avg);
        String countStr = String.valueOf(count);

        StringBuilder details = new StringBuilder();
        for (HelperRating r : matched) {
            if (details.length() > 0) details.append(";");
            details.append(r.getRequesterName()).append("~").append(r.getStars()).append("~").append(r.getTimestamp()).append("~").append(r.getTicketId());
        }

        return new String[]{targetName, avgStr, countStr, details.toString()};
    }

    public boolean hasTicketBeenRated(int ticketId) {
        synchronized (ratings) {
            for (HelperRating r : ratings) {
                if (r.getTicketId() == ticketId) return true;
            }
        }
        return false;
    }

    public void deleteTicket(int ticketId, String requesterId, String requesterNick) {
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

            // Check if the caller is the original requester (by Student ID, name, or embedded ID)
            boolean isRequester = (requesterId != null && requesterId.equalsIgnoreCase(target.getPeerId()))
                    || requesterNick.equalsIgnoreCase(target.getRequester())
                    || requesterNick.equalsIgnoreCase(target.getPeerId())
                    || (target.getRequester() != null && requesterId != null && target.getRequester().contains("(" + requesterId + ")"));

            if (!isRequester) {
                log("Warning: " + requesterNick + " (" + requesterId + ") attempted to delete ticket #" + ticketId + " without permission.");
                // Send error back to the requesting client
                for (ClientHandler client : clients) {
                    if (requesterNick.equalsIgnoreCase(client.getNickname())
                            || (requesterId != null && requesterId.equalsIgnoreCase(client.getStudentId()))) {
                        client.sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Only the original requester can remove this ticket."));
                        break;
                    }
                }
                return;
            }
            tickets.removeIf(t -> t.getId() == ticketId);
        }
        saveTicketsToStorage();
        broadcastTicketList();
        broadcastSystemMessage("Ticket #" + ticketId + " was removed by " + requesterNick);
    }

    public void deleteTicket(int ticketId, String requesterNick) {
        deleteTicket(ticketId, requesterNick, requesterNick);
    }

    /**
     * Stops the server, closes all client connections, and releases ports.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        log("Stopping PeerDesk Server...");

        // 1. Immediately close the ServerSocket to unblock acceptLoop
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        // 2. Safely notify and close each connected client
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(Protocol.buildPacket(Protocol.RSP_SERVER_ERR, "Server is shutting down."));
            } catch (Exception ignored) {}
            client.close();
        }
        clients.clear();

        // 3. Gracefully shutdown thread pool
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 4. Save final state
        saveTicketsToStorage();
        saveChatToStorage();
        saveStudentsToStorage();
        saveRatingsToStorage();

        log("PeerDesk Server stopped.");
    }
}
