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

    @Test
    void testStudentIdValidNumbersAndSymbols() {
        assertNull(server.validateStudentId("2025-1124"));
        assertNull(server.validateStudentId("12345678")); // normalized to 1234-5678
        assertNull(server.validateStudentId("2024-0001"));
    }

    @Test
    void testStudentIdRejectsNon8Digits() {
        assertNotNull(server.validateStudentId("2024-01"));
        assertNotNull(server.validateStudentId("12345"));
        assertNotNull(server.validateStudentId("2025-11245"));
    }

    @Test
    void testStudentIdRejectsLetters() {
        assertNotNull(server.validateStudentId("2025-A124"));
        assertNotNull(server.validateStudentId("Student123"));
        assertNotNull(server.validateStudentId("abc"));
    }

    @Test
    void testStudentIdRejectsDelimiters() {
        assertNotNull(server.validateStudentId("2025|1124"));
        assertNotNull(server.validateStudentId("2025~1124"));
        assertNotNull(server.validateStudentId("2025@1124"));
        assertNotNull(server.validateStudentId("2025,1124"));
    }

    @Test
    void testStudentIdBlankFails() {
        assertNotNull(server.validateStudentId(""));
        assertNotNull(server.validateStudentId("   "));
        assertNotNull(server.validateStudentId(null));
    }

    @Test
    void testRegistrationWithSameNameDifferentStudentIdAllowed() {
        assertNull(server.validateRegistration("Alex", "2025-1124"));
        assertNull(server.validateRegistration("Alex", "2025-5678"));
    }

    @Test
    void testRegistrationNameMismatchFails() {
        // First registration registers Alex with 2025-9999
        assertNull(server.validateRegistration("Alex", "2025-9999"));
        // Second sign-in with same name & ID succeeds
        assertNull(server.validateRegistration("Alex", "2025-9999"));
        // Sign-in with DIFFERENT name for same student ID must fail!
        String error = server.validateRegistration("John", "2025-9999");
        assertNotNull(error);
        assertTrue(error.contains("Wrong Name or Student ID"), "Error should explain name mismatch: " + error);
    }
}
