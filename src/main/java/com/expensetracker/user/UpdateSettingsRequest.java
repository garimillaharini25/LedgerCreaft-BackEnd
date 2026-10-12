package com.expensetracker.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {

    @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be light, dark, or system")
    private String theme;

    @Pattern(regexp = "^(TODAY|THIS_WEEK|THIS_MONTH|LAST_MONTH|ALL)$", message = "Default date range must be TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, or ALL")
    private String defaultDateRange;

    private Boolean budgetNotificationsEnabled;
}
