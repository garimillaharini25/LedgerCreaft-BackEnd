package com.expensetracker.category;

import com.expensetracker.common.ApiResponse;
import com.expensetracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Endpoints for managing spending categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories available to user with spending and transaction stats")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories(@AuthenticationPrincipal UserPrincipal principal) {
        List<CategoryDto> categories = categoryService.getAllCategoriesWithStats(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    @Operation(summary = "Create custom category")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto categoryDto = categoryService.createCategory(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", categoryDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update custom category")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryDto categoryDto = categoryService.updateCategory(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", categoryDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom category with safe reassignment of expenses to Other")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        categoryService.deleteCategory(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted and expenses safely reassigned to 'Other'", null));
    }
}
