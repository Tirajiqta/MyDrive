package org.example.mydrive.utils;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.repositories.PasswordResetTokenRepository;
import org.example.mydrive.repositories.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JobScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
