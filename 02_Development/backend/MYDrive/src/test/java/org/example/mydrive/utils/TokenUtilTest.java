package org.example.mydrive.utils;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilTest {

    @Test
    void newOpaqueToken_isUrlSafeAndUnpadded() {
        String token = TokenUtil.newOpaqueToken();

        assertThat(token).isNotBlank();
        // URL-safe Base64 alphabet without padding
        assertThat(token).matches("[A-Za-z0-9_-]+");
        assertThat(token).doesNotContain("=");
    }

    @Test
    void newOpaqueToken_producesUniqueValues() {
        String a = TokenUtil.newOpaqueToken();
        String b = TokenUtil.newOpaqueToken();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void sha256Hex_isDeterministic() {
        assertThat(TokenUtil.sha256Hex("hello"))
                .isEqualTo(TokenUtil.sha256Hex("hello"));
    }

    @Test
    void sha256Hex_producesLowercase64CharHex() {
        String hash = TokenUtil.sha256Hex("anything");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256Hex_matchesKnownVector() throws Exception {
        // Known SHA-256 of the empty string
        String expected = bytesToHex(MessageDigest.getInstance("SHA-256")
                .digest("".getBytes(StandardCharsets.UTF_8)));

        assertThat(TokenUtil.sha256Hex("")).isEqualTo(expected);
        assertThat(TokenUtil.sha256Hex(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void sha256Hex_differsForDifferentInput() {
        assertThat(TokenUtil.sha256Hex("a")).isNotEqualTo(TokenUtil.sha256Hex("b"));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
