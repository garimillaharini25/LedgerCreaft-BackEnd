package com.expensetracker.auth;

import com.expensetracker.common.BadRequestException;
import com.expensetracker.common.DuplicateResourceException;
import com.expensetracker.common.UnauthorizedException;
import com.expensetracker.security.JwtTokenProvider;
import com.expensetracker.user.User;
import com.expensetracker.user.UserDto;
import com.expensetracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final com.expensetracker.security.LoginRateLimiterService rateLimiterService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .currency("INR")
                .theme("light")
                .defaultDateRange("THIS_MONTH")
                .budgetNotificationsEnabled(true)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateTokenFromEmail(savedUser.getEmail(), savedUser.getId(), savedUser.getName());

        return AuthResponse.builder()
                .token(token)
                .user(UserDto.fromEntity(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        rateLimiterService.checkRateLimit(email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiterService.recordFailedAttempt(email);
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        rateLimiterService.recordSuccessfulLogin(email);
        String token = tokenProvider.generateTokenFromEmail(user.getEmail(), user.getId(), user.getName());

        return AuthResponse.builder()
                .token(token)
                .user(UserDto.fromEntity(user))
                .build();
    }
}
