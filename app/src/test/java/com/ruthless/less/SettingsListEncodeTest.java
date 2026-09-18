package com.ruthless.less;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Lightweight smoke tests that do not require an emulator.
 */
public class SettingsListEncodeTest {

    @Test
    public void encodeDecodeRoundTrip() {
        // Mirror SettingsRepository encode/decode rules without Android deps.
        String encoded = encode(java.util.Arrays.asList("a.b", "c.d", "e.f"));
        java.util.List<String> decoded = decode(encoded);
        assertEquals(3, decoded.size());
        assertEquals("a.b", decoded.get(0));
        assertEquals("e.f", decoded.get(2));
    }

    @Test
    public void emptyStaysEmpty() {
        assertTrue(decode("").isEmpty());
        assertTrue(decode(null).isEmpty());
    }

    private static String encode(java.util.List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packages.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(packages.get(i));
        }
        return sb.toString();
    }

    private static java.util.List<String> decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        String[] parts = raw.split("\n");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }
}
