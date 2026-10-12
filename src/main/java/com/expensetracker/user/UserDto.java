package com.expensetracker.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String currency;
    private String theme;
    private String defaultDateRange;
    private boolean budgetNotificationsEnabled;
    private Instant createdAt;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .currency(user.getCurrency())
                .theme(user.getTheme())
                .defaultDateRange(user.getDefaultDateRange())
                .budgetNotificationsEnabled(user.isBudgetNotificationsEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
