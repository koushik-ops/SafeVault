package com.securevault.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class LocalDevKeyManagementServiceTest {

    private LocalDevKeyManagementService devKms;

    @BeforeEach
    void setUp() {
        devKms = new LocalDevKeyManagementService();
        // Set a valid 32-byte Base64 key
        byte[] testKek = new byte[32];
        for (int i = 0; i < 32; i++) testKek[i] = (byte) i;
        String base64Kek = Base64.getEncoder().encodeToString(testKek);

        ReflectionTestUtils.setField(devKms, "localKekBase64", base64Kek);
        devKms.init();
    }

    @Test
    @DisplayName("Should generate fresh DEK and wrap it successfully")
    void testGenerateDataKey() {
        KeyManagementService.DataKey dataKey = devKms.generateDataKey();

        assertNotNull(dataKey);
        assertNotNull(dataKey.plaintextDek());
        assertEquals(32, dataKey.plaintextDek().length, "DEK must be 256 bits (32 bytes)");
        assertNotNull(dataKey.wrappedDek());
        assertTrue(dataKey.wrappedDek().length > 32, "Wrapped DEK includes IV and GCM tag");
        assertEquals("local-dev-kek://default-local-key", dataKey.keyReference());

        // Unwrapping should yield exact same plaintext DEK
        byte[] unwrapped = devKms.decryptDataKey(dataKey.wrappedDek(), dataKey.keyReference());
        assertArrayEquals(dataKey.plaintextDek(), unwrapped);
    }

    @Test
    @DisplayName("Different calls should generate different DEKs")
    void testIndependentDeks() {
        KeyManagementService.DataKey key1 = devKms.generateDataKey();
        KeyManagementService.DataKey key2 = devKms.generateDataKey();

        assertFalse(java.util.Arrays.equals(key1.plaintextDek(), key2.plaintextDek()));
        assertFalse(java.util.Arrays.equals(key1.wrappedDek(), key2.wrappedDek()));
    }
}
