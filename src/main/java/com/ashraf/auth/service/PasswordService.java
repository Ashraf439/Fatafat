package com.ashraf.auth.service;

import com.ashraf.core.entity.PasswordToken;
import com.ashraf.core.entity.User;
import com.ashraf.core.enums.PasswordTokenPurpose;
import com.ashraf.core.enums.Status;
import com.ashraf.core.repository.PasswordTokenRepository;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.core.repository.UserRolesRepository;
import com.ashraf.shared.exception.SuspendedAccountException;
import com.ashraf.shared.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Handles both flows that end with "user chooses a password":
 * <ul>
 *   <li>staff invite - owner adds staff, staff gets a link to set their first password;</li>
 *   <li>forgot password - any user requests a reset link.</li>
 * </ul>
 * Following the link and setting a password proves ownership of the email, so it also activates an
 * account that is still PENDING_VERIFICATION. SUSPENDED accounts are never re-activated.
 */
@Service
public class PasswordService {

    private static final Duration INVITE_TTL = Duration.ofHours(72);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final PasswordTokenRepository passwordTokenRepository;
    private final UserRepository userRepository;
    private final UserRolesRepository userRolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${frontend.restaurant-url:http://localhost:5173}")
    private String restaurantUrl;

    @Value("${frontend.customer-url:http://localhost:3000}")
    private String customerUrl;

    public PasswordService(PasswordTokenRepository passwordTokenRepository,
                           UserRepository userRepository,
                           UserRolesRepository userRolesRepository,
                           PasswordEncoder passwordEncoder,
                           TokenUtil tokenUtil,
                           RefreshTokenService refreshTokenService,
                           EmailService emailService) {
        this.passwordTokenRepository = passwordTokenRepository;
        this.userRepository = userRepository;
        this.userRolesRepository = userRolesRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    /** Called when an owner/manager adds a staff member. */
    @Transactional
    public void sendStaffInvite(User staffUser, String restaurantName) {
        String raw = issueToken(staffUser, PasswordTokenPurpose.INVITE, INVITE_TTL);
        emailService.sendStaffInviteEmail(staffUser.getEmail(), restaurantName, link(restaurantUrl, raw));
    }

    /**
     * "Forgot password". Deliberately silent when the email is unknown or suspended, so the endpoint
     * cannot be used to discover which emails are registered.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim())
                .filter(u -> u.getStatus() != Status.SUSPENDED)
                .ifPresent(u -> {
                    boolean customer = userRolesRepository.existsByUser_IdAndRole_Name(u.getId(), "CUSTOMER");
                    String raw = issueToken(u, PasswordTokenPurpose.RESET, RESET_TTL);
                    emailService.sendPasswordResetEmail(u.getEmail(), link(customer ? customerUrl : restaurantUrl, raw));
                });
    }

    /** Consumes a token from an invite/reset link and sets the new password. */
    @Transactional
    public void setPassword(String rawToken, String newPassword) {
        PasswordToken token = passwordTokenRepository.findByTokenHash(tokenUtil.hashToken(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("This link is invalid or has already been used."));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("This link has expired. Please request a new one.");
        }
        User user = token.getUser();
        if (user.getStatus() == Status.SUSPENDED) {
            throw new SuspendedAccountException("This account has been suspended.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        if (user.getStatus() == Status.PENDING_VERIFICATION) {
            user.setStatus(Status.ACTIVE);
        }
        userRepository.save(user);

        passwordTokenRepository.deleteByUser_Id(user.getId()); // single use, and kills any other outstanding link
        refreshTokenService.revokeAllSessions(user.getId());   // log out every existing session
    }

    private String issueToken(User user, PasswordTokenPurpose purpose, Duration ttl) {
        passwordTokenRepository.deleteByUser_Id(user.getId()); // only the newest link works
        String raw = tokenUtil.generateRawToken();
        PasswordToken token = new PasswordToken();
        token.setTokenHash(tokenUtil.hashToken(raw));
        token.setUser(user);
        token.setPurpose(purpose);
        token.setExpiresAt(Instant.now().plus(ttl));
        passwordTokenRepository.save(token);
        return raw;
    }

    private static String link(String baseUrl, String rawToken) {
        return baseUrl + "/set-password?token=" + rawToken;
    }
}
