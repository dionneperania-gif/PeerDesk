package com.mycompany.peerdesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Ticket model class.
 */
public class TicketTest {

    @Test
    void testSerializeDeserializeRoundTrip() {
        Ticket original = new Ticket(101, "Alice", "Alice", "Java / Sockets", "NPE in main", "Bob", Ticket.Status.IN_PROGRESS, "14:30:00");
        String serialized = original.serialize();
        Ticket deserialized = Ticket.deserialize(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getRequester(), deserialized.getRequester());
        assertEquals(original.getCategory(), deserialized.getCategory());
        assertEquals(original.getSubject(), deserialized.getSubject());
        assertEquals(original.getHelper(), deserialized.getHelper());
        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    void testDeserializeInvalidReturnsNull() {
        assertNull(Ticket.deserialize("garbage"));
        assertNull(Ticket.deserialize(""));
        assertNull(Ticket.deserialize("1~2~3"));
    }

    @Test
    void testNullHelperDefaultsToNone() {
        Ticket t = new Ticket(1, "Alice", "Java", "Bug", null, Ticket.Status.OPEN, "10:00:00");
        assertEquals("None", t.getHelper());
    }

    @Test
    void testSetHelperNullDefaultsToNone() {
        Ticket t = new Ticket(1, "Alice", "Java", "Bug", "Bob", Ticket.Status.OPEN, "10:00:00");
        t.setHelper(null);
        assertEquals("None", t.getHelper());
    }

    @Test
    void testSetHelperBlankDefaultsToNone() {
        Ticket t = new Ticket(1, "Alice", "Java", "Bug", "Bob", Ticket.Status.OPEN, "10:00:00");
        t.setHelper("   ");
        assertEquals("None", t.getHelper());
    }

    @Test
    void testSerializeSanitizesTilde() {
        Ticket t = new Ticket(1, "Alice", "Alice", "Java", "Bug with ~ char", "None", Ticket.Status.OPEN, "10:00:00");
        String serialized = t.serialize();
        Ticket deserialized = Ticket.deserialize(serialized);
        assertNotNull(deserialized);
        // Tilde should be replaced during sanitization
        assertFalse(deserialized.getSubject().contains("~"));
    }

    @Test
    void testStatusValues() {
        assertEquals(3, Ticket.Status.values().length);
        assertNotNull(Ticket.Status.valueOf("OPEN"));
        assertNotNull(Ticket.Status.valueOf("IN_PROGRESS"));
        assertNotNull(Ticket.Status.valueOf("RESOLVED"));
    }

    @Test
    void testToString() {
        Ticket t = new Ticket(42, "Alice", "Java", "NPE", "None", Ticket.Status.OPEN, "10:00:00");
        String s = t.toString();
        assertTrue(s.contains("42"));
        assertTrue(s.contains("OPEN"));
        assertTrue(s.contains("NPE"));
    }

    @Test
    void testBackwardCompatibility7Parts() {
        // 7-part format: id~req~cat~sub~hlp~status~ts
        String raw = "50~Alice~Java~Bug~None~OPEN~12:00:00";
        Ticket t = Ticket.deserialize(raw);
        assertNotNull(t);
        assertEquals(50, t.getId());
        assertEquals("Alice", t.getRequester());
        assertEquals("Java", t.getCategory());
        assertEquals("Bug", t.getSubject());
        assertEquals("OPEN", t.getStatus().name());
    }

    @Test
    void test8PartFormat() {
        // 8-part format: id~peer~req~cat~sub~hlp~status~ts
        String raw = "60~PeerAlice~Alice~Java~Bug~Bob~IN_PROGRESS~13:00:00";
        Ticket t = Ticket.deserialize(raw);
        assertNotNull(t);
        assertEquals(60, t.getId());
        assertEquals("PeerAlice", t.getPeerId());
        assertEquals("Alice", t.getRequester());
        assertEquals("Bob", t.getHelper());
    }
}
