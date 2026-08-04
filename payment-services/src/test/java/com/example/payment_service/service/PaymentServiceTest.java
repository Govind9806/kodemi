package com.example.payment_service.service;

import com.example.payment_service.dto.request.PaymentOrderRequest;
import com.example.payment_service.dto.request.PaymentVerifyRequest;
import com.example.payment_service.dto.response.PaymentOrderResponse;
import com.example.payment_service.dto.response.PaymentVerificationResponse;
import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.feign.NotificationClient;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        razorpayClient.orders = mock(com.razorpay.OrderClient.class);
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test-secret");
    }

    @Test
    void createOrder_success() throws Exception {
        PaymentOrderRequest request = new PaymentOrderRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setTargetId("course-123");
        request.setTargetType("COURSE");
        request.setUserid("user-123");
        request.setCurrency("INR");

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_12345");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);
        doNothing().when(paymentRepository).save(any(Payment.class));

        PaymentOrderResponse response = paymentService.createOrder(request);

        assertNotNull(response);
        assertEquals("order_12345", response.getOrderId());
        assertEquals("CREATED", response.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createOrder_invalidAmount() {
        PaymentOrderRequest request = new PaymentOrderRequest();
        request.setAmount(BigDecimal.ZERO);

        assertThrows(PaymentException.class, () -> {
            paymentService.createOrder(request);
        });
    }

    @Test
    void createOrder_missingTarget() {
        PaymentOrderRequest request = new PaymentOrderRequest();
        request.setAmount(BigDecimal.valueOf(100));

        assertThrows(PaymentException.class, () -> {
            paymentService.createOrder(request);
        });
    }

    @Test
    void getPaymentStatus_success() {
        Payment payment = new Payment();
        payment.setOrderId("order_123");
        when(paymentRepository.findById("order_123")).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentStatus("order_123");

        assertNotNull(result);
        assertEquals("order_123", result.getOrderId());
    }

    @Test
    void getPaymentStatus_notFound() {
        when(paymentRepository.findById("order_123")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> {
            paymentService.getPaymentStatus("order_123");
        });
    }

    @Test
    void verifyAccess_paid() {
        List<Payment> list = new ArrayList<>();
        Payment p = new Payment();
        p.setUserId("user-123");
        p.setTargetId("course-123");
        p.setTargetType("COURSE");
        p.setStatus("SUCCESS");
        p.setPaymentId("pay-123");
        p.setAmount(BigDecimal.valueOf(100));
        list.add(p);

        when(paymentRepository.findByUserId("user-123")).thenReturn(list);

        PaymentVerificationResponse res = paymentService.verifyAccess("user-123", "course-123", "COURSE");

        assertTrue(res.isPaid());
        assertEquals("pay-123", res.getPaymentId());
        assertEquals(BigDecimal.valueOf(100), res.getAmount());
    }

    @Test
    void verifyAccess_unpaid() {
        when(paymentRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        PaymentVerificationResponse res = paymentService.verifyAccess("user-123", "course-123", "COURSE");

        assertFalse(res.isPaid());
        assertEquals(BigDecimal.ZERO, res.getAmount());
    }

    @Test
    void verifyPayment_validationFailures() {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        
        // Missing order id
        assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));

        request.setRazorpayOrderId("order-123");
        // Missing payment id
        assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));

        request.setRazorpayPaymentId("pay-123");
        // Missing signature
        assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));
    }

    @Test
    void verifyPayment_success() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        Payment payment = new Payment();
        payment.setOrderId("order-123");
        payment.setStatus("CREATED");
        payment.setUserId("user-123");

        when(paymentRepository.findById("order-123")).thenReturn(Optional.of(payment));

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null); // success

            String result = paymentService.verifyPayment(request);
            assertEquals("Payment Verified Successfully", result);
            assertEquals("SUCCESS", payment.getStatus());
            assertEquals("pay-123", payment.getPaymentId());
        }
    }

    @Test
    void verifyPayment_alreadyProcessed() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        Payment payment = new Payment();
        payment.setOrderId("order-123");
        payment.setStatus("SUCCESS");

        when(paymentRepository.findById("order-123")).thenReturn(Optional.of(payment));

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null);

            assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));
        }
    }

    @Test
    void createOrder_nullCurrency() throws Exception {
        PaymentOrderRequest request = new PaymentOrderRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setTargetId("course-123");
        request.setTargetType("COURSE");
        request.setUserid("user-123");
        request.setCurrency(null); // test default currency branch

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_12345");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        PaymentOrderResponse response = paymentService.createOrder(request);
        assertNotNull(response);
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void verifyPayment_notFound() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-notFound");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        when(paymentRepository.findById("order-notFound")).thenReturn(Optional.empty());

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null);

            assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));
        }
    }

    @Test
    void verifyPayment_notificationError() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        Payment payment = new Payment();
        payment.setOrderId("order-123");
        payment.setStatus("CREATED");
        payment.setUserId("user-123");
        payment.setPaymentId("pay-123");

        when(paymentRepository.findById("order-123")).thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("Notification service down")).when(notificationClient).sendInternalNotification(any(), any());

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null);

            String result = paymentService.verifyPayment(request);
            assertEquals("Payment Verified Successfully", result);
        }
    }

    @Test
    void verifyPayment_signatureRazorpayException() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenThrow(new com.razorpay.RazorpayException("Invalid signature"));

            assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));
        }
    }

    @Test
    void verifyPayment_signatureGenericException() throws Exception {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = org.mockito.Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenThrow(new RuntimeException("Generic error"));

            assertThrows(PaymentException.class, () -> paymentService.verifyPayment(request));
        }
    }
}
