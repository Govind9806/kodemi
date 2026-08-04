package com.example.course_service.service.impl;

import com.example.course_service.dto.response.CategoryResponseDTO;
import com.example.course_service.exception.CategoryNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true),
        @CacheEvict(value = "allCourses", allEntries = true),
        @CacheEvict(value = "instructorCourses", allEntries = true),
        @CacheEvict(value = "verifiedCourses", allEntries = true),
        @CacheEvict(value = "courseDetails", allEntries = true),
        @CacheEvict(value = "liveCourseDetails", allEntries = true),
        @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public String createCategory(CategoryEntity category) {
        log.info("Entering createCategory with name: {}, slug: {}", category != null ? category.getName() : null, category != null ? category.getSlug() : null);
        if(category == null){
            throw new NullException("Null Body.");
        }
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setCategoryId(UUID.randomUUID().toString());
        categoryEntity.setName(category.getName());
        categoryEntity.setSlug(category.getSlug());
        categoryEntity.setDescription(category.getDescription());
        categoryEntity.setDisplayOrder(category.getDisplayOrder());
        categoryEntity.setIsActive(category.getIsActive());
        categoryEntity.setSubCategories(category.getSubCategories());
        categoryRepository.save(categoryEntity);
        log.info("Exiting createCategory successfully, created ID: {}", categoryEntity.getCategoryId());
        return "Category Created Successfully.";
    }

    @Cacheable(value = "categories", key = "#categoryId")
    public CategoryResponseDTO getCategoryById(String categoryId) {
        log.info("Entering getCategoryById for ID: {}", categoryId);

        CategoryEntity category = categoryRepository.findById(categoryId);

        if (category == null) {
            throw new CategoryNotFoundException(categoryId);
        }

        log.info("Exiting getCategoryById successfully for ID: {}", categoryId);
        return mapDto(category);
    }

    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("Entering getAllCategories");

        List<CategoryEntity> entities = categoryRepository.findAll();
        List<CategoryResponseDTO> categories = new java.util.ArrayList<>();
        for (CategoryEntity entity : entities) {
            categories.add(mapDto(entity));
        }
        log.info("Exiting getAllCategories, found {} categories", categories.size());
        return categories;
    }

    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true),
        @CacheEvict(value = "allCourses", allEntries = true),
        @CacheEvict(value = "instructorCourses", allEntries = true),
        @CacheEvict(value = "verifiedCourses", allEntries = true),
        @CacheEvict(value = "courseDetails", allEntries = true),
        @CacheEvict(value = "liveCourseDetails", allEntries = true),
        @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public String updateCategory(String categoryId, CategoryEntity category) {
        log.info("Entering updateCategory for ID: {}, name: {}", categoryId, category != null ? category.getName() : null);
        if(category == null){
            throw new NullException("Null Body.");
        }
        CategoryEntity existing = categoryRepository.findById(categoryId);

        if (existing == null) {
            throw new CategoryNotFoundException(categoryId);
        }

        existing.setName(category.getName());
        existing.setSlug(category.getSlug());
        existing.setDescription(category.getDescription());
        existing.setDisplayOrder(category.getDisplayOrder());
        existing.setIsActive(category.getIsActive());
        if (category.getSubCategories() != null) {
            existing.setSubCategories(category.getSubCategories());
        }

        categoryRepository.save(existing);

        log.info("Exiting updateCategory successfully for ID: {}", categoryId);
        return "Category Updated Successfully";
    }

    private CategoryResponseDTO mapDto(CategoryEntity entity) {

        return CategoryResponseDTO.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .subCategories(entity.getSubCategories())
                .build();
    }
}
