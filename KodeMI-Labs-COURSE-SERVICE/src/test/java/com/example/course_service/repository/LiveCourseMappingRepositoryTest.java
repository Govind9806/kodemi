package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.example.course_service.model.LiveCourseMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiveCourseMappingRepositoryTest {

    private LiveCourseMappingRepository repository;
    private DynamoDBMapper dynamoDBMapper;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        repository = new LiveCourseMappingRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsMapper() {
        LiveCourseMapping mapping = new LiveCourseMapping();
        repository.save(mapping);
        verify(dynamoDBMapper).save(mapping);
    }

    @Test
    void findById_ReturnsMapping() {
        LiveCourseMapping mapping = new LiveCourseMapping();
        when(dynamoDBMapper.load(LiveCourseMapping.class, "m1")).thenReturn(mapping);
        assertEquals(mapping, repository.findById("m1"));
    }

    @Test
    void findByCourseId_ReturnsFirst() {
        LiveCourseMapping mapping = new LiveCourseMapping();
        mapping.setCourseId("course-1");

        PaginatedScanList<LiveCourseMapping> scanResults = mock(PaginatedScanList.class);
        when(scanResults.isEmpty()).thenReturn(false);
        when(scanResults.get(0)).thenReturn(mapping);

        when(dynamoDBMapper.scan(eq(LiveCourseMapping.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanResults);

        LiveCourseMapping result = repository.findByCourseId("course-1");
        assertNotNull(result);
        assertEquals("course-1", result.getCourseId());
    }

    @Test
    void findByCourseId_NullCourseId_ReturnsNull() {
        assertNull(repository.findByCourseId(null));
    }

    @Test
    void findByCourseId_NoResults_ReturnsNull() {
        PaginatedScanList<LiveCourseMapping> scanResults = mock(PaginatedScanList.class);
        when(scanResults.isEmpty()).thenReturn(true);

        when(dynamoDBMapper.scan(eq(LiveCourseMapping.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanResults);
        assertNull(repository.findByCourseId("course-1"));
    }

    @Test
    void delete_ExistingMapping_CallsDelete() {
        LiveCourseMapping mapping = new LiveCourseMapping();
        when(dynamoDBMapper.load(LiveCourseMapping.class, "m1")).thenReturn(mapping);

        repository.delete("m1");

        verify(dynamoDBMapper).delete(mapping);
    }

    @Test
    void delete_NotFound_DoesNotCallDelete() {
        when(dynamoDBMapper.load(LiveCourseMapping.class, "m1")).thenReturn(null);

        repository.delete("m1");

        verify(dynamoDBMapper, never()).delete(any());
    }
}
