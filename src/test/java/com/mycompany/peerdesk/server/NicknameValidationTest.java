package com.mycompany.peerdesk.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for nickname validation in PeerDeskServer.
 */
public class NicknameValidationTest {

    private PeerDeskServer server;

    @BeforeEach
    void setUp() {
        server = new PeerDeskServer(0, false); // Port 0, in-memory mode
    }

    @Test
    void testNullNickname() {
        assertNotNull(server.validateNickname(null));
    }

    @Test
    void testBlankNickname() {
        assertNotNull(server.validateNickname(""));
        assertNotNull(server.validateNickname("   "));
    }

    @Test
    void testTooShortNickname() {
        assertNotNull(server.validateNickname("A"));
    }

    @Test
    void testTooLongNickname() {
        assertNotNull(server.validateNickname("ThisNameIsFarTooLong"));
    }

    @Test
    void testNicknameWithPipe() {
        assertNotNull(server.validateNickname("bad|name"));
    }

    @Test
    void testNicknameWithTilde() {
        assertNotNull(server.validateNickname("bad~name"));
    }

    @Test
    void testNicknameWithAt() {
        assertNotNull(server.validateNickname("bad@name"));
    }

    @Test
    void testNicknameWithComma() {
        assertNotNull(server.validateNickname("bad,name"));
    }

    @Test
    void testValidNickname() {
        assertNull(server.validateNickname("Alice"));
    }

    @Test
    void testMinLengthNickname() {
        assertNull(server.validateNickname("Al"));
    }

    @Test
    void testExactMaxLengthNickname() {
        // 16 characters exactly - should pass
        assertNull(server.validateNickname("AbcdefghijklmnOp"));
    }

    @Test
    void testOneOverMaxLength() {
        // 17 characters - should fail
        assertNotNull(server.validateNickname("AbcdefghijklmnOpQ"));
    }

    @Test
    void testNicknameWithSpacesAroundIsAccepted() {
        // Leading/trailing whitespace should be trimmed by validateNickname
        // The clean variable trims, so " Al " should work
        assertNull(server.validateNickname(" Alice "));
    }

    @Test
    void testValidNicknameWithNumbers() {
        assertNull(server.validateNickname("Peer123"));
    }

    @Test
    void testValidNicknameWithUnderscore() {
        assertNull(server.validateNickname("my_name"));
    }
}
