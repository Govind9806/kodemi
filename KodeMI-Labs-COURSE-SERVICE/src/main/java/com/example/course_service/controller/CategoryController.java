package com.example.course_service.controller;
import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.response.CategoryResponseDTO;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @RequiresRole("TRAINER")
    @PostMapping("/create")
    public ResponseEntity<String> createCategory(@RequestBody CategoryEntity category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @GetMapping("/get/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable String categoryId) {
        CategoryResponseDTO category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/get/all")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    @RequiresRole("TRAINER")
    @PutMapping("/edit/{categoryId}")
    public ResponseEntity<String> updateCategory(
            @PathVariable String categoryId,
            @RequestBody CategoryEntity category) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, category));
    }

}
