package com.expensetracker.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    List<Expense> findByUserIdAndExpenseDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserId(Long userId);

    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    @Modifying
    @Query("UPDATE Expense e SET e.category.id = :newCategoryId WHERE e.category.id = :oldCategoryId AND e.user.id = :userId")
    int reassignCategory(@Param("oldCategoryId") Long oldCategoryId,
                         @Param("newCategoryId") Long newCategoryId,
                         @Param("userId") Long userId);
}
