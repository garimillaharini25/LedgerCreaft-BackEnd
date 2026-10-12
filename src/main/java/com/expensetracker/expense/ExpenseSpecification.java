package com.expensetracker.expense;

import com.expensetracker.category.Category;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {

    public static Specification<Expense> filterExpenses(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            PaymentMethod paymentMethod,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce user isolation
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Join<Expense, Category> categoryJoin = root.join("category");

                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate notesPredicate = cb.like(cb.lower(root.get("notes")), searchPattern);
                Predicate catNamePredicate = cb.like(cb.lower(categoryJoin.get("name")), searchPattern);

                predicates.add(cb.or(descPredicate, notesPredicate, catNamePredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
