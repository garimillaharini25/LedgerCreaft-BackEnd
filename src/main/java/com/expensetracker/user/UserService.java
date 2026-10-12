package com.expensetracker.user;

import com.expensetracker.common.BadRequestException;
import com.expensetracker.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = findUserById(userId);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        user.setName(request.getName().trim());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            user.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserDto updateSettings(Long userId, UpdateSettingsRequest request) {
        User user = findUserById(userId);

        if (request.getTheme() != null) {
            user.setTheme(request.getTheme());
        }
        if (request.getDefaultDateRange() != null) {
            user.setDefaultDateRange(request.getDefaultDateRange());
        }
        if (request.getBudgetNotificationsEnabled() != null) {
            user.setBudgetNotificationsEnabled(request.getBudgetNotificationsEnabled());
        }

        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
