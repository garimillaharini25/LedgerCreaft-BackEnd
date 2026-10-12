package com.expensetracker.budget;

import com.expensetracker.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monthly_budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "`month`", "`year`"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(name = "`month`", nullable = false)
    private int month;

    @Column(name = "`year`", nullable = false)
    private int year;

    @Column(name = "total_budget", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(name = "warning_threshold", nullable = false)
    @Builder.Default
    private int warningThreshold = 80;

    @OneToMany(mappedBy = "monthlyBudget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetCategory> categoryBudgets = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
