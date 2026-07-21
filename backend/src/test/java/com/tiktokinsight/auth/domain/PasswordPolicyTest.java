package com.tiktokinsight.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void enforcesLengthLetterAndDigitRules() {
        assertThat(PasswordPolicy.isValid("Password1")).isTrue();
        assertThat(PasswordPolicy.isValid("lettersOnly")).isFalse();
        assertThat(PasswordPolicy.isValid("12345678")).isFalse();
        assertThat(PasswordPolicy.isValid("A1short")).isFalse();
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }
}
