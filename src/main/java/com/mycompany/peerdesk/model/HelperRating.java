package com.mycompany.peerdesk.model;

/**
 * Represents a star rating given by a ticket requester to a helper for resolving a ticket.
 */
public class HelperRating {
    private final int ticketId;
    private final String helperName;
    private final String requesterName;
    private final int stars; // 1 to 5
    private final String timestamp;

    public HelperRating(int ticketId, String helperName, String requesterName, int stars, String timestamp) {
        this.ticketId = ticketId;
        this.helperName = helperName;
        this.requesterName = requesterName;
        this.stars = stars;
        this.timestamp = timestamp;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getHelperName() {
        return helperName;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public int getStars() {
        return stars;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Serializes rating to single string format: ticketId~helperName~requesterName~stars~timestamp
     */
    public String serialize() {
        return ticketId + "~" + sanitize(helperName) + "~" + sanitize(requesterName) + "~" + stars + "~" + sanitize(timestamp);
    }

    public static HelperRating deserialize(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("~", 5);
        if (parts.length < 5) return null;
        try {
            int tid = Integer.parseInt(parts[0].trim());
            String hName = parts[1].trim();
            String rName = parts[2].trim();
            int s = Integer.parseInt(parts[3].trim());
            String ts = parts[4].trim();
            return new HelperRating(tid, hName, rName, s, ts);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitize(String val) {
        if (val == null) return "";
        return val.replace("~", "-").replace("\n", " ").replace("\r", "");
    }
}
