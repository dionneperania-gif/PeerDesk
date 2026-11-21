package com.mycompany.peerdesk.model;

/**
 * Represents a peer help ticket in the PeerDesk system.
 * Contains ticket metadata, requester, assigned helper, status, and timestamps.
 */
public class Ticket {
    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED
    }

    private int id;
    private String peerId;
    private String requester;
    private String category;
    private String subject;
    private String helper;
    private Status status;
    private String timestamp;

    public Ticket(int id, String peerId, String requester, String category, String subject, String helper, Status status, String timestamp) {
        this.id = id;
        this.peerId = (peerId == null || peerId.trim().isEmpty()) ? requester : peerId.trim();
        this.requester = requester;
        this.category = category;
        this.subject = subject;
        this.helper = (helper == null || helper.trim().isEmpty()) ? "None" : helper.trim();
        this.status = status;
        this.timestamp = timestamp;
    }

    public Ticket(int id, String requester, String category, String subject, String helper, Status status, String timestamp) {
        this(id, requester, requester, category, subject, helper, status, timestamp);
    }

    public int getId() {
        return id;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHelper() {
        return helper;
    }

    public void setHelper(String helper) {
        this.helper = (helper == null || helper.trim().isEmpty()) ? "None" : helper.trim();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Serializes this ticket to a single string line for network protocol transmission.
     * Escapes separator characters if needed.
     */
    public String serialize() {
        String safePeer = sanitize(peerId);
        String safeReq = sanitize(requester);
        String safeCat = sanitize(category);
        String safeSub = sanitize(subject);
        String safeHlp = sanitize(helper);
        return id + "~" + safePeer + "~" + safeReq + "~" + safeCat + "~" + safeSub + "~" + safeHlp + "~" + status.name() + "~" + timestamp;
    }

    /**
     * Deserializes a ticket from the network representation.
     */
    public static Ticket deserialize(String raw) {
        try {
            String[] parts = raw.split("~", 8);
            if (parts.length >= 8) {
                int id = Integer.parseInt(parts[0]);
                String peer = parts[1];
                String req = parts[2];
                String cat = parts[3];
                String sub = parts[4];
                String hlp = parts[5];
                Status stat = Status.valueOf(parts[6]);
                String ts = parts[7];
                return new Ticket(id, peer, req, cat, sub, hlp, stat, ts);
            } else if (parts.length == 7) {
                // Backward compatibility for 7-part tickets
                int id = Integer.parseInt(parts[0]);
                String req = parts[1];
                String cat = parts[2];
                String sub = parts[3];
                String hlp = parts[4];
                Status stat = Status.valueOf(parts[5]);
                String ts = parts[6];
                return new Ticket(id, req, req, cat, sub, hlp, stat, ts);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitize(String val) {
        if (val == null) return "";
        return val.replace("~", "-").replace("\n", " ").replace("\r", "");
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s (Req: %s, Helper: %s)", id, status, subject, requester, helper);
    }
}
