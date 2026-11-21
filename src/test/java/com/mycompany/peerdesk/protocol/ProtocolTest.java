package com.mycompany.peerdesk.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Protocol utility class.
 */
public class ProtocolTest {

    @Test
    void testBuildPacketSingleArg() {
        String result = Protocol.buildPacket("CMD", "arg1");
        assertEquals("CMD|arg1", result);
    }

    @Test
    void testBuildPacketMultipleArgs() {
        String result = Protocol.buildPacket("CONNECT", "Alice");
        assertEquals("CONNECT|Alice", result);
    }

    @Test
    void testBuildPacketNoArgs() {
        String result = Protocol.buildPacket("EXIT");
        assertEquals("EXIT", result);
    }

    @Test
    void testBuildPacketNullArg() {
        String result = Protocol.buildPacket("CMD", (String) null);
        assertEquals("CMD|", result);
    }

    @Test
    void testBuildPacketMultipleArgsFormat() {
        String result = Protocol.buildPacket("CHAT_MSG", "1001", "14:30:00", "Alice", "Hello world");
        assertEquals("CHAT_MSG|1001|14:30:00|Alice|Hello world", result);
    }

    @Test
    void testSanitizeChatRemovesPipes() {
        String result = Protocol.sanitizeChat("hello|world");
        assertFalse(result.contains("|"));
    }

    @Test
    void testSanitizeChatRemovesNewlines() {
        String result = Protocol.sanitizeChat("line1\nline2\rline3");
        assertFalse(result.contains("\n"));
        assertFalse(result.contains("\r"));
    }

    @Test
    void testSanitizeChatNull() {
        assertEquals("", Protocol.sanitizeChat(null));
    }

    @Test
    void testSanitizeChatPreservesNormalText() {
        assertEquals("Hello world!", Protocol.sanitizeChat("Hello world!"));
    }

    @Test
    void testGetCurrentTimestampFormat() {
        String ts = Protocol.getCurrentTimestamp();
        assertNotNull(ts);
        assertTrue(ts.matches("\\d{2}:\\d{2}:\\d{2}"), "Timestamp should match HH:mm:ss format, got: " + ts);
    }

    @Test
    void testProtocolVersionDefined() {
        assertTrue(Protocol.PROTOCOL_VERSION >= 1);
    }

    @Test
    void testNetworkConstants() {
        assertEquals(2000, Protocol.MAX_MESSAGE_LENGTH);
        assertEquals(16, Protocol.MAX_NICK_LENGTH);
        assertEquals(2, Protocol.MIN_NICK_LENGTH);
        assertEquals(5000, Protocol.CONNECTION_TIMEOUT_MS);
        assertEquals(8888, Protocol.DEFAULT_PORT);
    }

    @Test
    void testCommandConstants() {
        assertEquals("CONNECT", Protocol.CMD_CONNECT);
        assertEquals("CHAT", Protocol.CMD_CHAT);
        assertEquals("EXIT", Protocol.CMD_EXIT);
        assertEquals("PING", Protocol.CMD_PING);
        assertEquals("PONG", Protocol.CMD_PONG);
    }

    @Test
    void testTicketDelimiter() {
        assertEquals("@@", Protocol.TICKET_DELIMITER);
    }
}
