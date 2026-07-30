package com.ashraf.auth.service;

import com.ashraf.core.entity.RefreshToken;
import com.ashraf.core.entity.User;
import com.ashraf.core.repository.RefreshTokenRepository;
import com.ashraf.shared.exception.InvalidRefreshTokenException;
import com.ashraf.shared.utils.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenService {

    private static final int EXPIRY_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenUtil tokenUtil;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, TokenUtil tokenUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenUtil = tokenUtil;
    }

    public String issueRefreshToken(User user) {
        String rawToken = tokenUtil.generateRawToken();
        String hash = tokenUtil.hashToken(rawToken);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash);
        entity.setExpiresAt(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS));
        entity.setRevoked(false);
        // createdAt intentionally not set here — @PrePersist on the entity handles it

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    public RefreshTokenResult rotate(String rawToken) {
        String hash = tokenUtil.hashToken(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllActiveSessionsByUserId(existing.getUser().getId());
            throw new InvalidRefreshTokenException();
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        String newRawToken = issueRefreshToken(user);

        return new RefreshTokenResult(newRawToken, user);
    }

    public void revokeSession(String rawToken) {
        String hash = tokenUtil.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    public void revokeAllSessions(Long userId) {
        refreshTokenRepository.revokeAllActiveSessionsByUserId(userId);
    }
}