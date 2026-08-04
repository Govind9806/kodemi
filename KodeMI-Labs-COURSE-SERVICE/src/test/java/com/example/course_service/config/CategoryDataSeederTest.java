package com.example.course_service.config;

import com.example.course_service.model.CategoryEntity;
import com.example.course_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class CategoryDataSeederTest {

    private CategoryDataSeeder seeder;
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        categoryRepository = mock(CategoryRepository.class);
        seeder = new CategoryDataSeeder(categoryRepository);
    }

    @Test
    void run_SkipsIfExisting() throws Exception {
        CategoryEntity existing = new CategoryEntity();
        existing.setCategoryId("old-1");
        when(categoryRepository.findAll()).thenReturn(List.of(existing));

        seeder.run();

        verify(categoryRepository, never()).delete(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void run_EmptyExisting_SeedsAllCategories() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        seeder.run();

        verify(categoryRepository, never()).delete(any());
        verify(categoryRepository, atLeastOnce()).save(any(CategoryEntity.class));
    }
}
