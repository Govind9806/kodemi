package com.example.course_service.service;

import com.example.course_service.dto.response.CategoryResponseDTO;
import com.example.course_service.exception.CategoryNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceImplTest {

    private CategoryServiceImpl categoryService;
    private CategoryRepository categoryRepository;

    private CategoryEntity category;

    @BeforeEach
    void setup() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryServiceImpl(categoryRepository);

        category = new CategoryEntity();
        category.setCategoryId("123");
        category.setName("Programming");
        category.setSlug("programming");
        category.setDescription("Programming courses");
        category.setDisplayOrder(1);
        category.setIsActive(true);
    }

    // ===============================
    // CREATE CATEGORY
    // ===============================

    @Test
    void createCategory_Success() {
        // repo.save is now void, no need to mock return value.
        // By default mockito does nothing for void methods.

        String result = categoryService.createCategory(category);

        assertEquals("Category Created Successfully.", result);
        verify(categoryRepository, times(1)).save(any(CategoryEntity.class));
    }

    @Test
    void createCategory_NullBody_ThrowsException() {
        assertThrows(NullException.class,
                () -> categoryService.createCategory(null));

        verify(categoryRepository, never()).save(any());
    }

    // ===============================
    // GET BY ID
    // ===============================

    @Test
    void getCategoryById_Found_ReturnsDTO() {
        when(categoryRepository.findById("123"))
                .thenReturn(category);

        CategoryResponseDTO response =
                categoryService.getCategoryById("123");

        assertNotNull(response);
        assertEquals("123", response.getCategoryId());
        assertEquals("Programming", response.getName());
    }

    @Test
    void getCategoryById_NotFound_ThrowsException() {
        when(categoryRepository.findById("999"))
                .thenReturn(null);

        assertThrows(CategoryNotFoundException.class,
                () -> categoryService.getCategoryById("999"));
    }

    // ===============================
    // GET ALL
    // ===============================

    @Test
    void getAllCategories_ReturnsList() {
        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        List<CategoryResponseDTO> result =
                categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Programming", result.get(0).getName());
    }

    // ===============================
    // UPDATE CATEGORY
    // ===============================

    @Test
    void updateCategory_Success() {
        when(categoryRepository.findById("123"))
                .thenReturn(category);

        // repo.save is now void, no need to mock return value
        String result =
                categoryService.updateCategory("123", category);

        assertEquals("Category Updated Successfully", result);
        verify(categoryRepository, times(1)).save(any(CategoryEntity.class));
    }

    @Test
    void updateCategory_NotFound_ThrowsException() {
        when(categoryRepository.findById("999"))
                .thenReturn(null);

        assertThrows(CategoryNotFoundException.class,
                () -> categoryService.updateCategory("999", category));
    }

    @Test
    void updateCategory_NullBody_ThrowsException() {
        assertThrows(NullException.class,
                () -> categoryService.updateCategory("123", null));

        verify(categoryRepository, never()).save(any());
    }
}