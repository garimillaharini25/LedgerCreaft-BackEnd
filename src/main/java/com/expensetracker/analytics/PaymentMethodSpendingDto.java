package com.expensetracker.analytics;

import com.expensetracker.expense.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodSpendingDto {
    private PaymentMethod paymentMethod;
    private String displayName;
    private BigDecimal totalAmount;
    private double percentage;
    private long transactionCount;
}
