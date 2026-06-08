package org.example.mydrive.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "first.last@sub.domain.org",
            "a+tag@gmail.com",
            "user_name@host-name.io",
            "x@y.z"
    })
    void isEmail_returnsTrue_forValidAddresses(String value) {
        assertThat(GeneralUtils.isEmail(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainusername",
            "no-at-sign.com",
            "missing@domain@twice.com",
            "spaces in@email.com",
            "@nolocalpart.com",
            "trailing@"
    })
    void isEmail_returnsFalse_forInvalidAddresses(String value) {
        assertThat(GeneralUtils.isEmail(value)).isFalse();
    }

    @Test
    void getIdFromToken_extractsUidClaimAsLong() {
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        when(auth.getTokenAttributes()).thenReturn(Map.of("uid", 42));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(GeneralUtils.getIdFromToken()).isEqualTo(42L);
    }

    @Test
    void getIdFromToken_handlesLongClaimValue() {
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        when(auth.getTokenAttributes()).thenReturn(Map.of("uid", 9_000_000_000L));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(GeneralUtils.getIdFromToken()).isEqualTo(9_000_000_000L);
    }
}
