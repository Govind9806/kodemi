package com.example.course_service.controller;

import com.example.course_service.dto.response.CategoryResponseDTO;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryControllerTest {

    private CategoryController categoryController;
    private CategoryService categoryService;

    @BeforeEach
    void setup() {
        categoryService = mock(CategoryService.class);
        categoryController = new CategoryController(categoryService);
    }

    // ==================== createCategory ====================
    @Test
    void createCategory_CallsService() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Programming");

        when(categoryService.createCategory(category))
                .thenReturn("Category created successfully");

        ResponseEntity<String> response =
                categoryController.createCategory(category);

        verify(categoryService, times(1)).createCategory(category);
        assertEquals("Category created successfully", response.getBody());
    }

    // ==================== getCategoryById ====================
    @Test
    void getCategoryById_ReturnsCategory() {

        CategoryResponseDTO dto = CategoryResponseDTO.builder()
                .categoryId("1")
                .name("Programming")
                .slug("programming")
                .description("Programming courses")
                .displayOrder(1)
                .isActive(true)
                .build();

        when(categoryService.getCategoryById("1")).thenReturn(dto);

        ResponseEntity<CategoryResponseDTO> response =
                categoryController.getCategoryById("1");

        verify(categoryService, times(1)).getCategoryById("1");

        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().getCategoryId());
        assertEquals("Programming", response.getBody().getName());
        assertEquals("programming", response.getBody().getSlug());
        assertTrue(response.getBody().getIsActive());
    }

    // ==================== getAllCategories ====================
    @Test
    void getAllCategories_ReturnsList() {

        CategoryResponseDTO dto1 = CategoryResponseDTO.builder()
                .categoryId("1")
                .name("Programming")
                .build();

        CategoryResponseDTO dto2 = CategoryResponseDTO.builder()
                .categoryId("2")
                .name("Design")
                .build();

        List<CategoryResponseDTO> mockList = Arrays.asList(dto1, dto2);

        when(categoryService.getAllCategories()).thenReturn(mockList);

        ResponseEntity<List<CategoryResponseDTO>> response =
                categoryController.getAllCategories();

        verify(categoryService, times(1)).getAllCategories();

        assertEquals(2, response.getBody().size());
        assertEquals("Programming", response.getBody().get(0).getName());
        assertEquals("2", response.getBody().get(1).getCategoryId());
    }

    // ==================== updateCategory ====================
    @Test
    void updateCategory_CallsService() {

        CategoryEntity category = new CategoryEntity();
        category.setName("Updated Name");

        when(categoryService.updateCategory("1", category))
                .thenReturn("Category updated successfully");

        ResponseEntity<String> response =
                categoryController.updateCategory("1", category);

        verify(categoryService, times(1))
                .updateCategory("1", category);

        assertEquals("Category updated successfully", response.getBody());
    }
}