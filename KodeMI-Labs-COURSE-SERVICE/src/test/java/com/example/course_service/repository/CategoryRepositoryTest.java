package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.CategoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategoryRepositoryTest {

    private DynamoDBMapper dynamoDBMapper;
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        categoryRepository = new CategoryRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsDynamoDBMapper() {
        CategoryEntity category = new CategoryEntity();
        category.setCategoryId("C101");
        category.setName("Programming");

        categoryRepository.save(category);

        verify(dynamoDBMapper, times(1)).save(category);
    }

    @Test
    void findAll_ReturnsList() {
        @SuppressWarnings("unchecked")
        PaginatedScanList<CategoryEntity> paginatedList = mock(PaginatedScanList.class);

        when(paginatedList.size()).thenReturn(2);

        CategoryEntity c1 = new CategoryEntity();
        c1.setCategoryId("C101");
        CategoryEntity c2 = new CategoryEntity();
        c2.setCategoryId("C102");

        when(paginatedList.get(0)).thenReturn(c1);
        when(paginatedList.get(1)).thenReturn(c2);

        when(dynamoDBMapper.scan(eq(CategoryEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(paginatedList);

        List<CategoryEntity> result = categoryRepository.findAll();

        assertEquals(2, result.size());
        verify(dynamoDBMapper).scan(eq(CategoryEntity.class), any(DynamoDBScanExpression.class));
    }

    @Test
    void findById_ReturnsCategory() {
        CategoryEntity category = new CategoryEntity();
        category.setCategoryId("C101");

        when(dynamoDBMapper.load(CategoryEntity.class, "C101")).thenReturn(category);

        CategoryEntity result = categoryRepository.findById("C101");

        assertNotNull(result);
        assertEquals("C101", result.getCategoryId());
        verify(dynamoDBMapper).load(CategoryEntity.class, "C101");
    }

    @Test
    void findById_ReturnsNull_WhenNotFound() {
        when(dynamoDBMapper.load(CategoryEntity.class, "999")).thenReturn(null);

        CategoryEntity result = categoryRepository.findById("999");

        assertNull(result);
    }

    @Test
    void findByName_ReturnsCategory() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Programming");

        @SuppressWarnings("unchecked")
        PaginatedQueryList<CategoryEntity> queryResult = mock(PaginatedQueryList.class);

        when(queryResult.isEmpty()).thenReturn(false);
        when(queryResult.get(0)).thenReturn(category);

        when(dynamoDBMapper.query(eq(CategoryEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(queryResult);

        CategoryEntity result = categoryRepository.findByName("Programming");

        assertNotNull(result);
        assertEquals("Programming", result.getName());

        verify(dynamoDBMapper)
                .query(eq(CategoryEntity.class), any(DynamoDBQueryExpression.class));
    }

    @Test
    void findByName_ReturnsNull_WhenNotFound() {
        @SuppressWarnings("unchecked")
        PaginatedQueryList<CategoryEntity> queryResult = mock(PaginatedQueryList.class);

        when(queryResult.isEmpty()).thenReturn(true);

        when(dynamoDBMapper.query(eq(CategoryEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(queryResult);

        CategoryEntity result = categoryRepository.findByName("Unknown");

        assertNull(result);
    }

    @Test
    void delete_CallsDynamoDBMapper() {
        CategoryEntity category = new CategoryEntity();
        category.setCategoryId("C101");

        categoryRepository.delete(category);

        verify(dynamoDBMapper, times(1)).delete(category);
    }
}