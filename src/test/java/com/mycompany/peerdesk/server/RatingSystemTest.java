package com.mycompany.peerdesk.server;

import com.mycompany.peerdesk.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Star Rating system in PeerDeskServer.
 */
public class RatingSystemTest {

    private PeerDeskServer server;

    @BeforeEach
    void setUp() {
        server = new PeerDeskServer(0, false); // in-memory mode
    }

    @Test
    void testRatingNonExistentTicket() {
        String err = server.submitRating(999, 5, "Alice", "2025-1111");
        assertNotNull(err);
        assertTrue(err.contains("does not exist"));
    }

    @Test
    void testRatingUnresolvedTicket() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        // ticket ID 100
        String err = server.submitRating(100, 5, "Alice", "2025-1111");
        assertNotNull(err);
        assertTrue(err.contains("Only resolved tickets"));
    }

    @Test
    void testRatingTicketWithoutHelper() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        server.resolveTicket(100, "Alice");
        String err = server.submitRating(100, 5, "Alice", "2025-1111");
        assertNotNull(err);
        assertTrue(err.contains("No helper was assigned"));
    }

    @Test
    void testNonRequesterCannotRate() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        server.claimTicket(100, "Bob");
        server.resolveTicket(100, "Alice");

        String err = server.submitRating(100, 5, "Charlie", "2025-2222");
        assertNotNull(err);
        assertTrue(err.contains("Only the original requester"));
    }

    @Test
    void testHelperCannotRateThemselves() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        server.claimTicket(100, "Alice");
        server.resolveTicket(100, "Alice");

        String err = server.submitRating(100, 5, "Alice", "2025-1111");
        assertNotNull(err);
        assertTrue(err.contains("You cannot rate yourself"));
    }

    @Test
    void testSuccessfulSingleHelperRating() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        server.claimTicket(100, "Bob");
        server.resolveTicket(100, "Alice");

        String err = server.submitRating(100, 5, "Alice", "2025-1111");
        assertNull(err); // Success

        assertTrue(server.hasTicketBeenRated(100));

        // Check Bob's profile
        String[] info = server.getRatingsForClassmate("Bob");
        assertEquals("Bob", info[0]);
        assertEquals("5.0", info[1]);
        assertEquals("1", info[2]);
        assertTrue(info[3].contains("Alice~5~"));
    }

    @Test
    void testPreventDuplicateRatingOnSameTicket() {
        server.createTicket("2025-1111", "Alice", "Java", "Bug");
        server.claimTicket(100, "Bob");
        server.resolveTicket(100, "Alice");

        String err1 = server.submitRating(100, 5, "Alice", "2025-1111");
        assertNull(err1);

        String err2 = server.submitRating(100, 4, "Alice", "2025-1111");
        assertNotNull(err2);
        assertTrue(err2.contains("already been rated"));
    }

    @Test
    void testMultipleHelpersRatingCreditedToAll() {
        // Example from user request: Dion and Angela helped Myka and Myka gives the ticket 5 stars
        server.createTicket("2025-1111", "Myka", "Math", "Calculus");
        server.claimTicket(100, "Dion");
        server.claimTicket(100, "Angela");
        server.resolveTicket(100, "Myka");

        // Myka rates once with 5 stars
        String err = server.submitRating(100, 5, "Myka", "2025-1111");
        assertNull(err);

        // Dion receives 5-star rating
        String[] dionInfo = server.getRatingsForClassmate("Dion");
        assertEquals("5.0", dionInfo[1]);
        assertEquals("1", dionInfo[2]);

        // Angela receives 5-star rating
        String[] angelaInfo = server.getRatingsForClassmate("Angela");
        assertEquals("5.0", angelaInfo[1]);
        assertEquals("1", angelaInfo[2]);

        // Average calculation test with second ticket for Dion
        server.createTicket("2025-2222", "Chloe", "Java", "OOP");
        server.claimTicket(101, "Dion");
        server.resolveTicket(101, "Chloe");
        String err2 = server.submitRating(101, 3, "Chloe", "2025-2222");
        assertNull(err2);

        // Dion now has (5 + 3) / 2 = 4.0 average with 2 ratings
        dionInfo = server.getRatingsForClassmate("Dion");
        assertEquals("4.0", dionInfo[1]);
        assertEquals("2", dionInfo[2]);

        // Angela still has 1 rating
        angelaInfo = server.getRatingsForClassmate("Angela");
        assertEquals("5.0", angelaInfo[1]);
        assertEquals("1", angelaInfo[2]);
    }

    @Test
    void testHelperCannotResolveTicketOnBehalfOfRequester() {
        server.createTicket("2025-1111", "Alice (2025-1111)", "Java", "Bug");
        server.claimTicket(100, "Bob");

        // Helper Bob attempts to mark ticket resolved
        server.resolveTicket(100, "Bob", "2025-9999");

        // Ticket must remain IN_PROGRESS because Bob is not the requester
        Ticket t = null;
        for (Ticket ticket : server.getConnectedClients().isEmpty() ? getTicketFromList(100) : null) {
            t = ticket;
        }
        assertEquals(Ticket.Status.IN_PROGRESS, serverGetTicket(100).getStatus());

        // Now original requester Alice resolves it
        server.resolveTicket(100, "Alice", "2025-1111");
        assertEquals(Ticket.Status.RESOLVED, serverGetTicket(100).getStatus());
    }

    private Ticket serverGetTicket(int ticketId) {
        for (Ticket t : getTicketFromList(ticketId)) {
            if (t.getId() == ticketId) return t;
        }
        return null;
    }

    private java.util.List<Ticket> getTicketFromList(int ticketId) {
        try {
            java.lang.reflect.Field f = PeerDeskServer.class.getDeclaredField("tickets");
            f.setAccessible(true);
            return (java.util.List<Ticket>) f.get(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
