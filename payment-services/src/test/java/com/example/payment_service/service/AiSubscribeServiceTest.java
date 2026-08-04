package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreateSubscriptionRequest;
import com.example.payment_service.dto.request.UpdateSubscriptionRequest;
import com.example.payment_service.dto.response.SubscriptionResponse;
import com.example.payment_service.enums.SubscriptionStatus;
import com.example.payment_service.exception.DuplicateSubscriptionCreationException;
import com.example.payment_service.exception.ResourceNotFoundException;
import com.example.payment_service.model.AiSubscribe;
import com.example.payment_service.repository.AiSubscribeRepository;
import com.example.payment_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiSubscribeServiceTest {

    @Mock
    private AiSubscribeRepository repository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AiSubscribeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createSubscription_success() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanId("plan-1");
        request.setPaymentId("pay-1");
        request.setAutoRenew(true);
        request.setStartDate(LocalDateTime.now());
        request.setEndDate(LocalDateTime.now().plusMonths(1));

        when(jwtUtil.extractUserId(anyString())).thenReturn("user-1");
        when(repository.findByUserId("user-1")).thenReturn(java.util.Collections.emptyList()); // No subscription exists
        when(repository.save(any(AiSubscribe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse response = service.createSubscription(request, "token");

        assertNotNull(response);
        assertEquals("user-1", response.getUserId());
        assertEquals("plan-1", response.getPlanId());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertTrue(response.getAutoRenew());
        verify(repository, times(1)).save(any(AiSubscribe.class));
    }

    @Test
    void createSubscription_duplicateException() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        when(jwtUtil.extractUserId(anyString())).thenReturn("user-1");
        when(repository.findByUserId("user-1")).thenReturn(List.of(new AiSubscribe())); // Existing subscription exists

        assertThrows(DuplicateSubscriptionCreationException.class, () -> {
            service.createSubscription(request, "token");
        });
    }

    @Test
    void getSubscriptionById_success() {
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setUserId("user-1");

        when(repository.findById("sub-1")).thenReturn(Optional.of(sub));

        SubscriptionResponse response = service.getSubscriptionById("sub-1");

        assertNotNull(response);
        assertEquals("sub-1", response.getSubscriptionId());
    }

    @Test
    void getSubscriptionById_notFound() {
        when(repository.findById("sub-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getSubscriptionById("sub-1");
        });
    }

    @Test
    void getSubscriptionsByUserId_success() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        list.add(sub);

        when(repository.findByUserId("user-1")).thenReturn(list);

        List<SubscriptionResponse> responses = service.getSubscriptionsByUserId("user-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("sub-1", responses.get(0).getSubscriptionId());
    }

    @Test
    void getActiveSubscriptionByUserId_success() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setStatus(SubscriptionStatus.ACTIVE);
        list.add(sub);

        when(repository.findByUserId("user-1")).thenReturn(list);

        SubscriptionResponse response = service.getActiveSubscriptionByUserId("user-1");

        assertNotNull(response);
        assertEquals("sub-1", response.getSubscriptionId());
    }

    @Test
    void getActiveSubscriptionByUserId_notFound() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setStatus(SubscriptionStatus.CANCELLED);
        list.add(sub);

        when(repository.findByUserId("user-1")).thenReturn(list);

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getActiveSubscriptionByUserId("user-1");
        });
    }

    @Test
    void getAllSubscriptions_success() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        list.add(sub);

        when(repository.findAll()).thenReturn(list);

        List<SubscriptionResponse> responses = service.getAllSubscriptions();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void updateSubscription_success() {
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setStatus(SubscriptionStatus.ACTIVE);

        when(repository.findById("sub-1")).thenReturn(Optional.of(sub));
        when(repository.save(any(AiSubscribe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest();
        request.setPlanId("new-plan");
        request.setStatus(SubscriptionStatus.CANCELLED);
        request.setAutoRenew(false);
        request.setEndDate(LocalDateTime.now().plusDays(5));

        SubscriptionResponse response = service.updateSubscription("sub-1", request);

        assertNotNull(response);
        assertEquals("new-plan", response.getPlanId());
        assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
        assertFalse(response.getAutoRenew());
    }

    @Test
    void cancelSubscription_success() {
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setStatus(SubscriptionStatus.ACTIVE);

        when(repository.findById("sub-1")).thenReturn(Optional.of(sub));
        when(repository.save(any(AiSubscribe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse response = service.cancelSubscription("sub-1");

        assertNotNull(response);
        assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
        assertFalse(response.getAutoRenew());
    }

    @Test
    void renewSubscription_success() {
        AiSubscribe sub = new AiSubscribe();
        sub.setSubscriptionId("sub-1");
        sub.setStatus(SubscriptionStatus.CANCELLED);

        when(repository.findById("sub-1")).thenReturn(Optional.of(sub));
        when(repository.save(any(AiSubscribe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newEndDate = LocalDateTime.now().plusMonths(1);
        SubscriptionResponse response = service.renewSubscription("sub-1", newEndDate);

        assertNotNull(response);
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(newEndDate, response.getEndDate());
    }

    @Test
    void deleteSubscription_success() {
        doNothing().when(repository).deleteById("sub-1");

        service.deleteSubscription("sub-1");

        verify(repository, times(1)).deleteById("sub-1");
    }

    @Test
    void isSubscriptionActive_true() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setEndDate(LocalDateTime.now().plusDays(2));
        list.add(sub);

        when(repository.findByUserId("user-1")).thenReturn(list);

        assertTrue(service.isSubscriptionActive("user-1"));
    }

    @Test
    void isSubscriptionActive_false() {
        List<AiSubscribe> list = new ArrayList<>();
        AiSubscribe sub = new AiSubscribe();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setEndDate(LocalDateTime.now().minusDays(1)); // expired
        list.add(sub);

        when(repository.findByUserId("user-1")).thenReturn(list);

        assertFalse(service.isSubscriptionActive("user-1"));
    }
}
