package com.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Base62Encoder unit tests")
class Base62EncoderTest {

    private Base62Encoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }

    @Test
    @DisplayName("encode(0) returns '0'")
    void encode_zero() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @ParameterizedTest(name = "encode({0}) == {1}")
    @CsvSource({
            "1,  1",
            "61, z",
            "62, 10",
            "3843, zz",
            "238327, zzz"
    })
    @DisplayName("encode produces known Base62 values")
    void encode_knownValues(long input, String expected) {
        assertThat(encoder.encode(input)).isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("encode throws on negative input")
    void encode_negative_throws() {
        assertThatThrownBy(() -> encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("decode('0') returns 0")
    void decode_zero() {
        assertThat(encoder.decode("0")).isEqualTo(0L);
    }

    @ParameterizedTest(name = "decode({1}) == {0}")
    @CsvSource({
            "1,  1",
            "61, z",
            "62, 10",
            "3843, zz"
    })
    @DisplayName("decode reverses encode")
    void decode_knownValues(long expected, String input) {
        assertThat(encoder.decode(input.trim())).isEqualTo(expected);
    }

    @Test
    @DisplayName("decode throws on invalid character")
    void decode_invalidChar_throws() {
        assertThatThrownBy(() -> encoder.decode("a!b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base62 character");
    }

    @Test
    @DisplayName("decode throws on blank input")
    void decode_blank_throws() {
        assertThatThrownBy(() -> encoder.decode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "round-trip for {0}")
    @ValueSource(longs = {0, 1, 100, 999, 1_000_000, Long.MAX_VALUE / 2})
    @DisplayName("encode then decode returns original value")
    void roundTrip(long value) {
        assertThat(encoder.decode(encoder.encode(value))).isEqualTo(value);
    }

    @Test
    @DisplayName("generateRandom returns correct length")
    void generateRandom_correctLength() {
        for (int len : new int[]{5, 7, 10, 20}) {
            assertThat(encoder.generateRandom(len)).hasSize(len);
        }
    }

    @Test
    @DisplayName("generateRandom throws on non-positive length")
    void generateRandom_nonPositive_throws() {
        assertThatThrownBy(() -> encoder.generateRandom(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encoder.generateRandom(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateRandom produces only valid Base62 characters")
    void generateRandom_validChars() {
        for (int i = 0; i < 50; i++) {
            String code = encoder.generateRandom(7);
            assertThat(encoder.isValidBase62(code)).isTrue();
        }
    }

    @Test
    @DisplayName("generateRandom produces unique codes (probabilistic)")
    void generateRandom_uniqueness() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(encoder.generateRandom(7));
        }
        assertThat(codes.size()).isGreaterThan(995);
    }


    @Test
    @DisplayName("isValidBase62 returns true for valid codes")
    void isValid_valid() {
        assertThat(encoder.isValidBase62("abc123")).isTrue();
        assertThat(encoder.isValidBase62("ABC")).isTrue();
        assertThat(encoder.isValidBase62("0")).isTrue();
    }

    @Test
    @DisplayName("isValidBase62 returns false for invalid codes")
    void isValid_invalid() {
        assertThat(encoder.isValidBase62(null)).isFalse();
        assertThat(encoder.isValidBase62("")).isFalse();
        assertThat(encoder.isValidBase62("abc!")).isFalse();
        assertThat(encoder.isValidBase62("my-code")).isFalse();
    }
}
