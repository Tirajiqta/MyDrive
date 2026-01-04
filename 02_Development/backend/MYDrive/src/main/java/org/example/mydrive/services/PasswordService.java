package org.example.mydrive.services;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ChangePasswordRequest;
import org.example.mydrive.dto.ForgotPasswordRequest;
import org.example.mydrive.dto.ResetPasswordRequest;
import org.example.mydrive.entities.PasswordResetTokenEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.PasswordResetTokenRepository;
import org.example.mydrive.repositories.UserRepository;
import org.example.mydrive.utils.TokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final long resetTtlSeconds = 60L * 30; // 30 minutes

    public void forgotPassword(ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase();

        // Do NOT reveal whether the email exists
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = TokenUtil.newOpaqueToken();
            String hash = TokenUtil.sha256Hex(rawToken);

            PasswordResetTokenEntity prt = new PasswordResetTokenEntity();
            prt.setUser(user);
            prt.setTokenHash(hash);
            prt.setExpiresAt(Instant.now().plusSeconds(resetTtlSeconds));
            passwordResetTokenRepository.save(prt);

            // Send email (simple text)
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("MYDrive Password Reset");
            msg.setText("Use this token to reset your password:\n\n" + rawToken + "\n\nExpires in 30 minutes.");
            mailSender.send(msg);
        });
    }

    public void resetPassword(ResetPasswordRequest req) {
        String hash = TokenUtil.sha256Hex(req.token());

        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token already used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        UserEntity user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    public void changePassword(ChangePasswordRequest req, JwtAuthenticationToken auth) {
        String email = auth.getToken().getSubject(); // subject = email
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}
