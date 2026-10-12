package com.expensetracker.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private boolean systemCategory;
    @Builder.Default
    private BigDecimal totalSpending = BigDecimal.ZERO;
    @Builder.Default
    private long transactionCount = 0L;

    public static CategoryDto fromEntity(Category category, BigDecimal totalSpending, long transactionCount) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .systemCategory(category.isSystemCategory())
                .totalSpending(totalSpending != null ? totalSpending : BigDecimal.ZERO)
                .transactionCount(transactionCount)
                .build();
    }
}
