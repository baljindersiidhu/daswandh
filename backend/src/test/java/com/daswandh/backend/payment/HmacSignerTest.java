package com.daswandh.backend.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    @Test
    void producesTheKnownHmacSha256TestVector() {
        // Well-known HMAC-SHA256 test vector (see Wikipedia's HMAC article)
        String result = HmacSigner.hmacSha256Hex("The quick brown fox jumps over the lazy dog", "key");

        assertThat(result).isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void differentDataGivesDifferentSignature() {
        assertThat(HmacSigner.hmacSha256Hex("order_1|pay_1", "secret"))
                .isNotEqualTo(HmacSigner.hmacSha256Hex("order_1|pay_2", "secret"));
    }

    @Test
    void constantTimeEqualsComparesCorrectly() {
        assertThat(HmacSigner.constantTimeEquals("abc123", "abc123")).isTrue();
        assertThat(HmacSigner.constantTimeEquals("abc123", "abc124")).isFalse();
        assertThat(HmacSigner.constantTimeEquals("abc", "abcdef")).isFalse();
    }
}