package com.expensetracker.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDto {
    private String id;
    private String title;
    private String description;
    private String type; // POSITIVE, WARNING, INFO
    private String icon;
}
