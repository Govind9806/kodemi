package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.ModuleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ModuleRepositoryTest {

    private DynamoDBMapper dynamoDBMapper;
    private ModuleRepository moduleRepository;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        moduleRepository = new ModuleRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsDynamoDBMapper() {
        ModuleEntity module = new ModuleEntity();
        module.setModuleId("M101");

        moduleRepository.save(module);

        verify(dynamoDBMapper).save(module);
    }

    @Test
    void findById_ReturnsModule() {
        ModuleEntity module = new ModuleEntity();
        module.setModuleId("M101");

        when(dynamoDBMapper.load(ModuleEntity.class, "M101")).thenReturn(module);

        ModuleEntity result = moduleRepository.findById("M101");

        assertNotNull(result);
        assertEquals("M101", result.getModuleId());
    }

    @Test
    void findById_ReturnsNull_WhenNotFound() {
        when(dynamoDBMapper.load(ModuleEntity.class, "999")).thenReturn(null);

        assertNull(moduleRepository.findById("999"));
    }

    @Test
    void findAll_ReturnsList() {
        @SuppressWarnings("unchecked")
        PaginatedScanList<ModuleEntity> scanList = mock(PaginatedScanList.class);

        when(scanList.size()).thenReturn(2);

        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");

        ModuleEntity m2 = new ModuleEntity();
        m2.setModuleId("M2");

        when(scanList.get(0)).thenReturn(m1);
        when(scanList.get(1)).thenReturn(m2);

        when(dynamoDBMapper.scan(eq(ModuleEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanList);

        List<ModuleEntity> result = moduleRepository.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findByCourseId_ReturnsList() {

        @SuppressWarnings("unchecked")
        PaginatedQueryList<ModuleEntity> queryList = mock(PaginatedQueryList.class);

        when(queryList.size()).thenReturn(1);

        ModuleEntity module = new ModuleEntity();
        module.setModuleId("M101");

        when(queryList.get(0)).thenReturn(module);

        // ✅ CORRECT STUB
        when(dynamoDBMapper.query(eq(ModuleEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(queryList);

        List<ModuleEntity> result = moduleRepository.findByCourseId("course-1");

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(dynamoDBMapper)
                .query(eq(ModuleEntity.class), any(DynamoDBQueryExpression.class));
    }
}