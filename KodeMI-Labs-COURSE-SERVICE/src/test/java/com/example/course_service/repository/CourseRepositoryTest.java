package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.example.course_service.model.CourseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseRepositoryTest {

    private DynamoDBMapper dynamoDBMapper;
    private CourseRepository courseRepository;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        courseRepository = new CourseRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsDynamoDBMapper() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        course.setTitle("Java Basics");

        courseRepository.save(course);

        verify(dynamoDBMapper, times(1)).save(course);
    }

    @Test
    void findAll_ReturnsList() {
        @SuppressWarnings("unchecked")
        com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage<CourseEntity> scanResultPage = mock(com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage.class);

        CourseEntity c1 = new CourseEntity();
        c1.setCourseId("C101");
        CourseEntity c2 = new CourseEntity();
        c2.setCourseId("C102");
        when(scanResultPage.getResults()).thenReturn(List.of(c1, c2));

        when(dynamoDBMapper.scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanResultPage);

        List<CourseEntity> list = courseRepository.findAll();

        verify(dynamoDBMapper, times(1))
                .scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class));
        assertEquals(2, list.size());
    }

    @Test
    void findById_ReturnsCourse() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        when(dynamoDBMapper.load(CourseEntity.class, "C101")).thenReturn(course);

        CourseEntity result = courseRepository.findById("C101");

        verify(dynamoDBMapper, times(1)).load(CourseEntity.class, "C101");
        assertEquals("C101", result.getCourseId());
    }

    @Test
    void findById_ReturnsNull_WhenNotFound() {
        when(dynamoDBMapper.load(CourseEntity.class, "999")).thenReturn(null);

        CourseEntity result = courseRepository.findById("999");

        assertNull(result);
    }

    @Test
    void findByIsVerified_ReturnsList() {
        @SuppressWarnings("unchecked")
        com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage<CourseEntity> scanResultPage = mock(com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage.class);
        when(scanResultPage.getResults()).thenReturn(List.of(new CourseEntity()));
        when(dynamoDBMapper.scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanResultPage);

        List<CourseEntity> result = courseRepository.findByIsVerified(true);

        assertNotNull(result);
        verify(dynamoDBMapper, times(1))
                .scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class));
    }

    @Test
    void findByCategoryId_ReturnsList() {
        @SuppressWarnings("unchecked")
        com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage<CourseEntity> scanResultPage = mock(com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage.class);
        when(scanResultPage.getResults()).thenReturn(List.of(new CourseEntity()));
        when(dynamoDBMapper.scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanResultPage);

        List<CourseEntity> result = courseRepository.findByCategoryId("cat-1");

        assertNotNull(result);
        verify(dynamoDBMapper, times(1))
                .scanPage(eq(CourseEntity.class), any(DynamoDBScanExpression.class));
    }
}