package com.example.kodemilabs.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepoTest {

    private DynamoDBMapper dynamoDBMapper;
    private UserRepo userRepo;

    private User user;

    @BeforeEach
    void setUp() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        userRepo = new UserRepo(dynamoDBMapper);

        user = new User();
        user.setUserId("user123");
        user.setEmail("test@mail.com");
        user.setName("Govind");
        user.setUsername("govind123");
        user.setRole(Role.TRAINER);
        user.setStatus("APPROVAL_PENDING");
    }

    // 🔧 Helper to mock PaginatedQueryList
    private PaginatedQueryList<User> mockQueryList(User user) {
        PaginatedQueryList<User> list = mock(PaginatedQueryList.class);

        if (user == null) {
            when(list.isEmpty()).thenReturn(true);
        } else {
            when(list.isEmpty()).thenReturn(false);
            when(list.get(0)).thenReturn(user);
        }

        return list;
    }

    // ✅ SAVE USER
    @Test
    void save_shouldCallDynamoDBMapperSave() {
        userRepo.save(user);
        verify(dynamoDBMapper).save(user);
    }

    // ✅ GET USER BY ID SUCCESS
    @Test
    void getUserById_shouldReturnUser_whenExists() {
        when(dynamoDBMapper.load(User.class, "user123"))
                .thenReturn(user);

        User result = userRepo.getUserById("user123");

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
    }

    // ❌ GET USER BY ID NOT FOUND
    @Test
    void getUserById_shouldReturnNull_whenNotExists() {
        when(dynamoDBMapper.load(User.class, "user123"))
                .thenReturn(null);

        User result = userRepo.getUserById("user123");

        assertNull(result);
    }

    // ✅ FIND BY ID SUCCESS
    @Test
    void findById_shouldReturnOptionalUser_whenExists() {
        when(dynamoDBMapper.load(User.class, "user123"))
                .thenReturn(user);

        Optional<User> result = userRepo.findById("user123");

        assertTrue(result.isPresent());
    }

    // ❌ FIND BY ID NOT FOUND
    @Test
    void findById_shouldReturnEmptyOptional_whenNotExists() {
        when(dynamoDBMapper.load(User.class, "user123"))
                .thenReturn(null);

        Optional<User> result = userRepo.findById("user123");

        assertTrue(result.isEmpty());
    }

    // ✅ GET USER BY EMAIL SUCCESS
    @Test
    void getUserByEmail_shouldReturnUser_whenExists() {
        PaginatedQueryList<User> list = mockQueryList(user);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        User result = userRepo.getUserByEmail("test@mail.com");

        assertNotNull(result);
        assertEquals("test@mail.com", result.getEmail());
    }

    // ❌ GET USER BY EMAIL NOT FOUND
    @Test
    void getUserByEmail_shouldReturnNull_whenNoResults() {
        PaginatedQueryList<User> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        User result = userRepo.getUserByEmail("test@mail.com");

        assertNull(result);
    }

    // ✅ GET FULL NAME
    @Test
    void getFullNameByEmail_shouldReturnName_whenUserExists() {
        PaginatedQueryList<User> list = mockQueryList(user);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        String result = userRepo.getFullNameByEmail("test@mail.com");

        assertEquals("Govind", result);
    }

    // ❌ GET FULL NAME NOT FOUND
    @Test
    void getFullNameByEmail_shouldReturnNull_whenUserNotExists() {
        PaginatedQueryList<User> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        String result = userRepo.getFullNameByEmail("test@mail.com");

        assertNull(result);
    }

    // ✅ GET USERNAME
    @Test
    void getUsernameByEmail_shouldReturnUsername_whenUserExists() {
        PaginatedQueryList<User> list = mockQueryList(user);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        String result = userRepo.getUsernameByEmail("test@mail.com");

        assertEquals("govind123", result);
    }

    // ❌ GET USERNAME NOT FOUND
    @Test
    void getUsernameByEmail_shouldReturnNull_whenUserNotExists() {
        PaginatedQueryList<User> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        String result = userRepo.getUsernameByEmail("test@mail.com");

        assertNull(result);
    }

    // ✅ GET PENDING TRAINERS
    @Test
    void getPendingTrainers_shouldReturnList() {

        PaginatedQueryList<User> list = mock(PaginatedQueryList.class);

        if (user == null) {
            when(list.isEmpty()).thenReturn(true);
            when(list.size()).thenReturn(0);
        } else {
            when(list.isEmpty()).thenReturn(false);
            when(list.size()).thenReturn(1);   // ✅ IMPORTANT FIX
            when(list.get(0)).thenReturn(user);
        }

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        var result = userRepo.getPendingTrainers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ❌ GET PENDING TRAINERS EMPTY
    @Test
    void getPendingTrainers_shouldReturnEmptyList_whenNoResults() {
        PaginatedQueryList<User> list = mockQueryList(null);

        doReturn(list).when(dynamoDBMapper).query(
                eq(User.class),
                any()
        );

        var result = userRepo.getPendingTrainers();

        assertTrue(result.isEmpty());
    }

    // ✅ FIND BY IDS NULL OR EMPTY
    @Test
    void findByIds_nullOrEmpty_shouldReturnEmptyList() {
        assertTrue(userRepo.findByIds(null).isEmpty());
        assertTrue(userRepo.findByIds(java.util.Collections.emptyList()).isEmpty());
    }

    // ✅ FIND BY IDS SUCCESS
    @Test
    void findByIds_validIds_shouldReturnUsers() {
        java.util.Map<String, java.util.List<Object>> resultMap = new java.util.HashMap<>();
        resultMap.put("UserTable", java.util.List.of(user));

        when(dynamoDBMapper.batchLoad(any(java.util.List.class))).thenReturn(resultMap);

        java.util.List<User> results = userRepo.findByIds(java.util.List.of("user123"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("user123", results.get(0).getUserId());
    }
}
