package org.example.mydrive.services;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.*;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.entities.RefreshTokenEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.entities.UserSubscriptionEntity;
import org.example.mydrive.repositories.*;
import org.example.mydrive.utils.GeneralUtils;
import org.example.mydrive.utils.TokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PlanRepository planRepository;
    private final LanguageRepository languageRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final long refreshTtlSeconds = 60L * 60 * 24 * 30; // 30 days


    public AuthTokensResponse login(UserLoginRequest request) {
        String identifier = request.usernameOrEmail().trim();
        boolean isEmail = GeneralUtils.isEmail(identifier);
        identifier = identifier.toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserEntity user = (isEmail
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier)
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        user.setLastLogin(LocalDateTime.from(Instant.now()));
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());

        // Create refresh token

        String refresh = refreshToken(user);

        return new AuthTokensResponse(accessToken, refresh, toUserResponse(user));
    }

    private String refreshToken(UserEntity user) {
        String refresh = TokenUtil.newOpaqueToken();
        String hash = TokenUtil.sha256Hex(refresh);

        RefreshTokenEntity rte = new RefreshTokenEntity();
        rte.setUser(user);
        rte.setTokenHash(hash);
        rte.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        refreshTokenRepository.save(rte);
        return refresh;
    }

    public AuthTokensResponse refresh(RefreshRequest request) {
        String refresh = request.refreshToken();
        String hash = TokenUtil.sha256Hex(refresh);

        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        UserEntity user = stored.getUser();

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        String newRefresh = refreshToken(user);

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthTokensResponse(accessToken, newRefresh, toUserResponse(user));
    }

    public void logout(LogoutRequest request) {
        String hash = TokenUtil.sha256Hex(request.refreshToken());

        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    public UserResponse register(UserRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        // 1) Fetch default plan (FREE)
        PlanEntity freePlan = planRepository.findByInternalNameAndIsActiveTrue("FREE_PLAN")
                .orElseGet(() -> planRepository.findFirstByTypeAndIsActiveTrue(PlanEntity.PlanType.FREE)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "No active FREE plan found. Seed the plans table."
                        )));

        // 2) Create user
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setLastLogin(LocalDateTime.now());
        user.setCurrentStorageUsed(0L);

        // Optional: set default language
        // LanguageEntity defaultLang = languageRepository.findByCode("en").orElseThrow(...)
        // user.setPreferredLanguageEntity(defaultLang);

        UserEntity savedUser = userRepository.save(user);

        // 3) Create subscription (ACTIVE, no endDate)
        UserSubscriptionEntity sub = new UserSubscriptionEntity();
        sub.setUser(savedUser);
        sub.setPlanEntity(freePlan);
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(null);
        sub.setStatus("ACTIVE");
        sub.setRenewalDate(null);
        sub.setPaymentMethodId(null);

        UserSubscriptionEntity savedSub = userSubscriptionRepository.save(sub);

        // 4) Link it back to user if you keep activeSubscription on UserEntity
        // (Do this only if UserEntity has activeSubscription field)
        savedUser.setActiveSubscription(savedSub);
        savedUser = userRepository.save(savedUser);

        return toUserResponse(savedUser);
    }

    public UserResponse getUserResponseById(Long id) {
        Optional<UserEntity> entity = userRepository.findById(id);
        return entity.map(this::toUserResponse).orElse(null);
    }

    private UserResponse toUserResponse(UserEntity u) {
        Integer storageLimitGb = null;
        String langCode = null;

        if (u.getActiveSubscription() != null && u.getActiveSubscription().getPlanEntity() != null) {
            storageLimitGb = u.getActiveSubscription().getPlanEntity().getStorageLimitGB();
        }
        if (u.getPreferredLanguageEntity() != null) {
            langCode = u.getPreferredLanguageEntity().getCode();
        }

        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getCreatedAt(),
                u.getLastLogin(),
                u.getCurrentStorageUsed(),
                storageLimitGb,
                langCode
        );
    }
}
