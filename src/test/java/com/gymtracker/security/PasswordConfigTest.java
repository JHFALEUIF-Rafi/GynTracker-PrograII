package com.gymtracker.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for the password encoding configuration.
 */
class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

    @Test
    void producesABCryptEncoder() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void encodedPasswordIsNeverStoredInPlainText() {
        String encoded = passwordEncoder.encode("plain-text-password");

        assertThat(encoded).isNotEqualTo("plain-text-password");
    }

    @Test
    void matchesReturnsTrueForCorrectRawPassword() {
        String encoded = passwordEncoder.encode("correct-password");

        assertThat(passwordEncoder.matches("correct-password", encoded)).isTrue();
    }

    @Test
    void matchesReturnsFalseForIncorrectRawPassword() {
        String encoded = passwordEncoder.encode("correct-password");

        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }

    @Test
    void encodingIsSaltedAndProducesDifferentHashesForSameInput() {
        String first = passwordEncoder.encode("same-password");
        String second = passwordEncoder.encode("same-password");

        assertThat(first).isNotEqualTo(second);
    }
}
