package com.example.course_service.service;
import com.example.course_service.dto.response.CategoryResponseDTO;
import com.example.course_service.model.CategoryEntity;
import java.util.List;

public interface CategoryService {
    String createCategory(CategoryEntity category);
    CategoryResponseDTO getCategoryById(String categoryId);
    List<CategoryResponseDTO> getAllCategories();
    String updateCategory(String categoryId, CategoryEntity category);
}
