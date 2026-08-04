package com.example.kodemilabs.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.example.kodemilabs.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenRepoTest {

    private DynamoDBMapper dynamoDBMapper;
    private RefreshTokenRepo repo;

    private RefreshToken token;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        repo = new RefreshTokenRepo(dynamoDBMapper);

        token = new RefreshToken();
        token.setToken("token123");
        token.setEmail("test@mail.com");
    }

    // 🔧 helper for PaginatedQueryList
    private PaginatedQueryList<RefreshToken> mockQueryList(RefreshToken token) {
        PaginatedQueryList<RefreshToken> list = mock(PaginatedQueryList.class);

        if (token == null) {
            when(list.isEmpty()).thenReturn(true);
            when(list.size()).thenReturn(0);
        } else {
            when(list.isEmpty()).thenReturn(false);
            when(list.size()).thenReturn(1);
            when(list.get(0)).thenReturn(token);
        }

        return list;
    }

    // ✅ SAVE
    @Test
    void save_shouldCallMapper() {
        repo.save(token);
        verify(dynamoDBMapper).save(token);
    }

    // ✅ FIND SUCCESS
    @Test
    void find_shouldReturnToken_whenExists() {
        when(dynamoDBMapper.load(RefreshToken.class, "token123"))
                .thenReturn(token);

        RefreshToken result = repo.find("token123");

        assertNotNull(result);
        assertEquals("token123", result.getToken());
    }

    // ❌ FIND NOT FOUND
    @Test
    void find_shouldReturnNull_whenNotExists() {
        when(dynamoDBMapper.load(RefreshToken.class, "token123"))
                .thenReturn(null);

        RefreshToken result = repo.find("token123");

        assertNull(result);
    }

    // ✅ DELETE SUCCESS
    @Test
    void delete_shouldDelete_whenExists() {
        when(dynamoDBMapper.load(RefreshToken.class, "token123"))
                .thenReturn(token);

        repo.delete("token123");

        verify(dynamoDBMapper).delete(token);
    }

    // ❌ DELETE NOT FOUND
    @Test
    void delete_shouldDoNothing_whenNotExists() {
        when(dynamoDBMapper.load(RefreshToken.class, "token123"))
                .thenReturn(null);

        repo.delete("token123");

        verify(dynamoDBMapper, never()).delete(any());
    }

    // ✅ FIND BY EMAIL SUCCESS
    @Test
    void findByEmail_shouldReturnToken_whenExists() {
        PaginatedQueryList<RefreshToken> list = mockQueryList(token);

        doReturn(list).when(dynamoDBMapper).query(
                eq(RefreshToken.class),
                any()
        );

        RefreshToken result = repo.findByEmail("test@mail.com");

        assertNotNull(result);
        assertEquals("token123", result.getToken());
    }

    // ❌ FIND BY EMAIL EMPTY
    @Test
    void findByEmail_shouldReturnNull_whenEmpty() {
        PaginatedQueryList<RefreshToken> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(RefreshToken.class),
                any()
        );

        RefreshToken result = repo.findByEmail("test@mail.com");

        assertNull(result);
    }

    // ✅ FIND ALL BY EMAIL
    @Test
    void findAllByEmail_shouldReturnList() {
        PaginatedQueryList<RefreshToken> list = mockQueryList(token);

        doReturn(list).when(dynamoDBMapper).query(
                eq(RefreshToken.class),
                any()
        );

        List<RefreshToken> result = repo.findAllByEmail("test@mail.com");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ❌ DELETE ALL - NO TOKENS
    @Test
    void deleteAllByEmail_shouldDoNothing_whenEmpty() {
        PaginatedQueryList<RefreshToken> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(RefreshToken.class),
                any()
        );

        repo.deleteAllByEmail("test@mail.com");

        verify(dynamoDBMapper, never()).batchDelete(anyList());
    }

    // ✅ DELETE ALL - TOKENS EXIST
    @Test
    void deleteAllByEmail_shouldDelete_whenExists() {
        PaginatedQueryList<RefreshToken> list = mockQueryList(token);

        doReturn(list).when(dynamoDBMapper).query(
                eq(RefreshToken.class),
                any()
        );

        repo.deleteAllByEmail("test@mail.com");

        verify(dynamoDBMapper).batchDelete(list);
    }
}
