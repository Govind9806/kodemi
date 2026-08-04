package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.LessonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LessonRepositoryTest {

    private DynamoDBMapper dynamoDBMapper;
    private LessonRepository lessonRepository;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        lessonRepository = new LessonRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsDynamoDBMapper() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L101");
        lesson.setTitle("Intro");

        lessonRepository.save(lesson);

        verify(dynamoDBMapper).save(lesson);
    }

    @Test
    void findById_ReturnsLesson() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L101");

        when(dynamoDBMapper.load(LessonEntity.class, "L101")).thenReturn(lesson);

        LessonEntity result = lessonRepository.findById("L101");

        assertNotNull(result);
        assertEquals("L101", result.getLessonId());
    }

    @Test
    void findById_ReturnsNull_WhenNotFound() {
        when(dynamoDBMapper.load(LessonEntity.class, "999")).thenReturn(null);

        assertNull(lessonRepository.findById("999"));
    }

    @Test
    void findAll_ReturnsList() {
        @SuppressWarnings("unchecked")
        PaginatedScanList<LessonEntity> paginatedList = mock(PaginatedScanList.class);

        when(paginatedList.size()).thenReturn(2);

        LessonEntity l1 = new LessonEntity();
        l1.setLessonId("L1");

        LessonEntity l2 = new LessonEntity();
        l2.setLessonId("L2");

        when(paginatedList.get(0)).thenReturn(l1);
        when(paginatedList.get(1)).thenReturn(l2);

        when(dynamoDBMapper.scan(eq(LessonEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(paginatedList);

        List<LessonEntity> result = lessonRepository.findAll();

        assertEquals(2, result.size());
        verify(dynamoDBMapper).scan(eq(LessonEntity.class), any(DynamoDBScanExpression.class));
    }

    @Test
    void findByModuleId_ReturnsList() {
        @SuppressWarnings("unchecked")
        PaginatedQueryList<LessonEntity> paginatedList = mock(PaginatedQueryList.class);

        when(paginatedList.size()).thenReturn(1);

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L101");

        when(paginatedList.get(0)).thenReturn(lesson);

        when(dynamoDBMapper.query(eq(LessonEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(paginatedList);

        List<LessonEntity> result = lessonRepository.findByModuleId("module-1");

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(dynamoDBMapper)
                .query(eq(LessonEntity.class), any(DynamoDBQueryExpression.class));
    }

    @Test
    void delete_CallsDynamoDBMapper() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L101");

        lessonRepository.delete(lesson);

        verify(dynamoDBMapper).delete(lesson);
    }
}