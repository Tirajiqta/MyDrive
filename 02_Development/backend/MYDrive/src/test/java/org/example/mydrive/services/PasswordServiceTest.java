package org.example.mydrive.services;

import org.example.mydrive.dto.ChangePasswordRequest;
import org.example.mydrive.dto.ForgotPasswordRequest;
import org.example.mydrive.dto.ResetPasswordRequest;
import org.example.mydrive.entities.PasswordResetTokenEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.PasswordResetTokenRepository;
import org.example.mydrive.repositories.UserRepository;
import org.example.mydrive.utils.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private PasswordService passwordService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPassword("oldHash");
    }

    // ----- forgotPassword -----

    @Test
    void forgotPassword_persistsTokenAndSendsEmail_whenUserExists() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordService.forgotPassword(new ForgotPasswordRequest("User@Example.com"));

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetTokenEntity saved = tokenCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage msg = mailCaptor.getValue();
        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).contains("Password Reset");
        // emailed raw token must hash to the stored hash
        String rawToken = extractTokenFromMail(msg.getText());
        assertThat(TokenUtil.sha256Hex(rawToken)).isEqualTo(saved.getTokenHash());
    }

    @Test
    void forgotPassword_doesNothing_whenUserUnknown() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        passwordService.forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(mailSender);
    }

    // ----- resetPassword -----

    @Test
    void resetPassword_updatesPasswordAndMarksTokenUsed() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(600));
        when(passwordResetTokenRepository.findByTokenHash(TokenUtil.sha256Hex("raw"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        passwordService.resetPassword(new ResetPasswordRequest("raw", "newPass"));

        assertThat(user.getPassword()).isEqualTo("newHash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPassword_withUnknownToken_throwsBadRequest() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("bad", "x")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void resetPassword_withAlreadyUsedToken_throwsBadRequest() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(600));
        token.setUsedAt(Instant.now().minusSeconds(5));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("raw", "x")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("already used"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_withExpiredToken_throwsBadRequest() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(user);
        token.setExpiresAt(Instant.now().minusSeconds(5));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("raw", "x")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("expired"));
    }

    // ----- changePassword -----

    @Test
    void changePassword_updatesPassword_whenCurrentMatches() {
        JwtAuthenticationToken auth = authFor("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("brandNew")).thenReturn("brandNewHash");

        passwordService.changePassword(new ChangePasswordRequest("current", "brandNew"), auth);

        assertThat(user.getPassword()).isEqualTo("brandNewHash");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_whenCurrentPasswordWrong_throwsBadRequest() {
        JwtAuthenticationToken auth = authFor("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        assertThatThrownBy(() -> passwordService.changePassword(new ChangePasswordRequest("wrong", "brandNew"), auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenUserMissing_throwsUnauthorized() {
        JwtAuthenticationToken auth = authFor("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.changePassword(new ChangePasswordRequest("a", "b"), auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static JwtAuthenticationToken authFor(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(email)
                .claim("uid", 1L)
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    private static String extractTokenFromMail(String text) {
        // body is: "...:\n\n<token>\n\nExpires..."
        String[] parts = text.split("\n\n");
        return parts[1].trim();
    }
}
