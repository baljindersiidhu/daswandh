package com.daswandh.backend.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HashingTest {

    @Test
    void sha256MatchesTheKnownTestVector() {
        assertThat(Hashing.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sha1MatchesTheKnownTestVector() {
        assertThat(Hashing.sha1Hex("abc")).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
    }
}