package org.example.mydrive.services;

import org.example.mydrive.dto.*;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.entities.RefreshTokenEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.entities.UserSubscriptionEntity;
import org.example.mydrive.repositories.*;
import org.example.mydrive.utils.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private UserSubscriptionRepository userSubscriptionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private LanguageRepository languageRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPassword("hashed");
        user.setCurrentStorageUsed(0L);
    }

    // ----- login -----

    @Test
    void login_withEmail_authenticatesLooksUpByEmailAndReturnsTokens() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(1L, "john@example.com")).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokensResponse res = authService.login(new UserLoginRequest("John@Example.com", "pw"));

        assertThat(res.accessToken()).isEqualTo("access-token");
        assertThat(res.refreshToken()).isNotBlank();
        assertThat(res.user().email()).isEqualTo("john@example.com");
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository, never()).findByUsername(anyString());
        // lastLogin set during login
        assertThat(user.getLastLogin()).isNotNull();
    }

    @Test
    void login_withUsername_looksUpByUsername() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.login(new UserLoginRequest("John", "pw"));

        verify(userRepository).findByUsername("john");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_persistsHashedRefreshToken_notRawToken() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokensResponse res = authService.login(new UserLoginRequest("john@example.com", "pw"));

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshTokenEntity stored = captor.getValue();
        assertThat(stored.getTokenHash()).isEqualTo(TokenUtil.sha256Hex(res.refreshToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(res.refreshToken());
        assertThat(stored.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void login_withBadCredentials_throwsUnauthorized() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new UserLoginRequest("john@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_whenUserMissingAfterAuth_throwsUnauthorized() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new UserLoginRequest("john@example.com", "pw")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ----- refresh -----

    @Test
    void refresh_withValidToken_rotatesAndReturnsNewTokens() {
        String raw = "raw-refresh";
        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setUser(user);
        stored.setTokenHash(TokenUtil.sha256Hex(raw));
        stored.setExpiresAt(Instant.now().plusSeconds(1000));
        when(refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex(raw))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("new-access");

        AuthTokensResponse res = authService.refresh(new RefreshRequest(raw));

        assertThat(res.accessToken()).isEqualTo("new-access");
        assertThat(res.refreshToken()).isNotEqualTo(raw);
        // old token revoked
        assertThat(stored.getRevokedAt()).isNotNull();
        // saved twice: revoke old + persist new
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    void refresh_withUnknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("nope")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_withRevokedToken_throwsUnauthorized() {
        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(1000));
        stored.setRevokedAt(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("x")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("revoked"));
    }

    @Test
    void refresh_withExpiredToken_throwsUnauthorized() {
        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("x")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("expired"));
    }

    // ----- logout -----

    @Test
    void logout_revokesActiveToken() {
        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setUser(user);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout(new LogoutRequest("raw"));

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_isNoOp_whenAlreadyRevoked() {
        RefreshTokenEntity stored = new RefreshTokenEntity();
        Instant revokedAt = Instant.now().minusSeconds(5);
        stored.setRevokedAt(revokedAt);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout(new LogoutRequest("raw"));

        assertThat(stored.getRevokedAt()).isEqualTo(revokedAt);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_isNoOp_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout(new LogoutRequest("raw"));

        verify(refreshTokenRepository, never()).save(any());
    }

    // ----- register -----

    @Test
    void register_createsUserSubscriptionAndReturnsTokens() {
        PlanEntity free = PlanEntity.builder()
                .id(10L).internalName("FREE_PLAN").storageLimitGB(5).isActive(true)
                .build();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(planRepository.findByInternalNameAndIsActiveTrue("FREE_PLAN")).thenReturn(Optional.of(free));
        when(passwordEncoder.encode("password123")).thenReturn("ENC");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            if (u.getId() == null) u.setId(99L);
            return u;
        });
        when(userSubscriptionRepository.save(any(UserSubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokensResponse res = authService.register(
                new UserRegisterRequest("NewBie", "New@Example.com", "password123"));

        assertThat(res.accessToken()).isEqualTo("access");
        assertThat(res.user().email()).isEqualTo("new@example.com");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getUsername()).isEqualTo("newbie");
        assertThat(saved.getPassword()).isEqualTo("ENC");
        assertThat(saved.getCurrentStorageUsed()).isZero();

        ArgumentCaptor<UserSubscriptionEntity> subCaptor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(subCaptor.getValue().getPlanEntity()).isEqualTo(free);
    }

    @Test
    void register_withExistingEmail_throwsConflict() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new UserRegisterRequest("user", "dup@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withExistingUsername_throwsConflict() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new UserRegisterRequest("Taken", "fresh@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void register_whenNoActiveFreePlan_throwsInternalServerError() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(planRepository.findByInternalNameAndIsActiveTrue("FREE_PLAN")).thenReturn(Optional.empty());
        when(planRepository.findFirstByTypeAndIsActiveTrue(PlanEntity.PlanType.FREE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(
                new UserRegisterRequest("user", "u@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // ----- getUserResponseById / updatePreferredLanguage -----

    @Test
    void getUserResponseById_returnsResponse_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse res = authService.getUserResponseById(1L);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(1L);
    }

    @Test
    void getUserResponseById_returnsNull_whenMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(authService.getUserResponseById(2L)).isNull();
    }

    @Test
    void updatePreferredLanguage_whenUserMissing_throwsNotFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updatePreferredLanguage(5L, "en"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updatePreferredLanguage_whenLanguageUnknown_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(languageRepository.findByCode("xx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updatePreferredLanguage(1L, "xx"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
