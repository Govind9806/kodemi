package com.example.payment_service.service;

import com.example.payment_service.dto.request.*;
import com.example.payment_service.dto.response.*;
import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.exception.WalletNotFoundException;
import com.example.payment_service.feign.NotificationClient;
import com.example.payment_service.model.PayoutRequest;
import com.example.payment_service.model.Wallet;
import com.example.payment_service.model.WalletTransaction;
import com.example.payment_service.repository.PayoutRequestRepository;
import com.example.payment_service.repository.WalletRepository;
import com.example.payment_service.repository.WalletTransactionRepository;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.util.JwtUtil;
import com.razorpay.Order;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        razorpayClient.orders = mock(com.razorpay.OrderClient.class);
        razorpayClient.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
        ReflectionTestUtils.setField(walletService, "razorpayKeySecret", "test-secret");
        ReflectionTestUtils.setField(walletService, "maxRechargeAmount", BigDecimal.valueOf(1000000));
    }

    @Test
    void createWallet_success() {
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet w = walletService.createWallet("user-123", "LEARNER");

        assertNotNull(w);
        assertEquals("user-123", w.getUserId());
        assertEquals("LEARNER", w.getUserType());
        assertEquals(BigDecimal.ZERO, w.getBalance());
    }

    @Test
    void createWallet_validationFailure() {
        assertThrows(PaymentException.class, () -> walletService.createWallet("", "LEARNER"));
        assertThrows(PaymentException.class, () -> walletService.createWallet("user-123", ""));
    }

    @Test
    void addFunds_success() throws Exception {
        AddFundsRequest request = new AddFundsRequest();
        request.setUserId("user-123");
        request.setAmount(BigDecimal.valueOf(1000));
        request.setCurrency("INR");
        request.setReturnUrl("http://localhost/callback");

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        PaymentLink mockLink = mock(PaymentLink.class);
        when(mockLink.get("short_url")).thenReturn("http://razorpay/short");
        when(mockLink.get("id")).thenReturn("plink_123");
        when(razorpayClient.paymentLink.create(any(JSONObject.class))).thenReturn(mockLink);

        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentOrderResponse response = walletService.addFunds(request, "LEARNER");

        assertNotNull(response);
        assertEquals("order_123", response.getOrderId());
        assertEquals("http://razorpay/short", response.getPaymentLink());
    }

    @Test
    void addFunds_validationFailures() {
        AddFundsRequest request = new AddFundsRequest();
        
        // Missing user ID
        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));

        request.setUserId("user-123");
        request.setAmount(BigDecimal.ZERO);
        // Low amount
        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));

        request.setAmount(BigDecimal.valueOf(2000000));
        // Exceeds max amount
        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));
    }

    @Test
    void addFunds_suspendedWallet() {
        AddFundsRequest request = new AddFundsRequest();
        request.setUserId("user-123");
        request.setAmount(BigDecimal.valueOf(1000));

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setStatus("SUSPENDED");

        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));
    }

    @Test
    void confirmAddFunds_orderNotFound() {
        AddFundsConfirmRequest request = new AddFundsConfirmRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        when(transactionRepository.findByRazorpayOrderId("order-123")).thenReturn(new ArrayList<>());

        assertThrows(PaymentException.class, () -> walletService.confirmAddFunds(request));
    }

    @Test
    void confirmAddFunds_success() {
        AddFundsConfirmRequest request = new AddFundsConfirmRequest();
        request.setRazorpayOrderId("order-123");
        request.setRazorpayPaymentId("pay-123");
        request.setRazorpaySignature("sig-123");

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId("user-123");
        tx.setStatus("PENDING");
        tx.setAmount(BigDecimal.valueOf(100));
        List<WalletTransaction> existing = new ArrayList<>();
        existing.add(tx);

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setBalance(BigDecimal.valueOf(50));
        wallet.setStatus("ACTIVE");

        when(transactionRepository.findByRazorpayOrderId("order-123")).thenReturn(existing);
        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));

        try (MockedStatic<com.razorpay.Utils> mockedUtils = Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null);

            walletService.confirmAddFunds(request);

            assertEquals("SUCCESS", tx.getStatus());
            assertEquals(BigDecimal.valueOf(150), wallet.getBalance());
        }
    }

    @Test
    void buyCourse_success() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("learner-1");
        request.setCourseId("course-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(200));
        request.setCourseName("Java Programming");

        Wallet learnerWallet = new Wallet();
        learnerWallet.setUserId("learner-1");
        learnerWallet.setBalance(BigDecimal.valueOf(500));
        learnerWallet.setTotalSpent(BigDecimal.ZERO);
        learnerWallet.setStatus("ACTIVE");

        Wallet trainerWallet = new Wallet();
        trainerWallet.setUserId("trainer-1");
        trainerWallet.setBalance(BigDecimal.ZERO);
        trainerWallet.setTotalEarned(BigDecimal.ZERO);
        trainerWallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(learnerWallet));
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(trainerWallet));

        PaymentOrderResponse response = walletService.buyCourse(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(300), learnerWallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), learnerWallet.getTotalSpent());
        assertEquals(BigDecimal.valueOf(160.0), trainerWallet.getBalance());
        verify(paymentRepository, times(1)).save(any(com.example.payment_service.model.Payment.class));
    }

    @Test
    void buyCourse_validationFailures() {
        BuyCourseRequest request = new BuyCourseRequest();
        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));

        request.setUserId("learner-1");
        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));

        request.setCourseId("course-1");
        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));

        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.ZERO);
        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void buyCourse_suspendedWallet() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("learner-1");
        request.setCourseId("course-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setStatus("SUSPENDED");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void buyCourse_insufficientBalance() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("learner-1");
        request.setCourseId("course-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setBalance(BigDecimal.valueOf(50));
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void buyCourse_selfPurchase() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("learner-1");
        request.setCourseId("course-1");
        request.setTrainerId("learner-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setBalance(BigDecimal.valueOf(200));
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void buyResource_success() {
        BuyResourceRequest request = new BuyResourceRequest();
        request.setUserId("learner-1");
        request.setResourceId("res-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));
        request.setResourceName("Notes");

        Wallet learnerWallet = new Wallet();
        learnerWallet.setUserId("learner-1");
        learnerWallet.setBalance(BigDecimal.valueOf(200));
        learnerWallet.setTotalSpent(BigDecimal.ZERO);
        learnerWallet.setStatus("ACTIVE");

        Wallet trainerWallet = new Wallet();
        trainerWallet.setUserId("trainer-1");
        trainerWallet.setBalance(BigDecimal.ZERO);
        trainerWallet.setTotalEarned(BigDecimal.ZERO);
        trainerWallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(learnerWallet));
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(trainerWallet));

        PaymentOrderResponse response = walletService.buyResource(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100), learnerWallet.getBalance());
        assertEquals(BigDecimal.valueOf(80.0), trainerWallet.getBalance());
        verify(paymentRepository, times(1)).save(any(com.example.payment_service.model.Payment.class));
    }

    @Test
    void buyResource_validationFailures() {
        BuyResourceRequest request = new BuyResourceRequest();
        assertThrows(PaymentException.class, () -> walletService.buyResource(request));

        request.setUserId("learner-1");
        assertThrows(PaymentException.class, () -> walletService.buyResource(request));

        request.setResourceId("res-1");
        assertThrows(PaymentException.class, () -> walletService.buyResource(request));

        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.ZERO);
        assertThrows(PaymentException.class, () -> walletService.buyResource(request));
    }

    @Test
    void buyResource_suspendedWallet() {
        BuyResourceRequest request = new BuyResourceRequest();
        request.setUserId("learner-1");
        request.setResourceId("res-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setStatus("SUSPENDED");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyResource(request));
    }

    @Test
    void buyResource_insufficientBalance() {
        BuyResourceRequest request = new BuyResourceRequest();
        request.setUserId("learner-1");
        request.setResourceId("res-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setBalance(BigDecimal.valueOf(50));
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyResource(request));
    }

    @Test
    void buyResource_selfPurchase() {
        BuyResourceRequest request = new BuyResourceRequest();
        request.setUserId("learner-1");
        request.setResourceId("res-1");
        request.setTrainerId("learner-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setBalance(BigDecimal.valueOf(200));
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.buyResource(request));
    }

    @Test
    void getWalletBalance_success() {
        Wallet w = new Wallet();
        w.setUserId("user-1");
        w.setBalance(BigDecimal.valueOf(50));
        w.setStatus("ACTIVE");

        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(w));

        WalletResponse response = walletService.getWalletBalance("user-1");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(50), response.getBalance());
    }

    @Test
    void getWalletBalance_validationFailure() {
        assertThrows(PaymentException.class, () -> walletService.getWalletBalance(""));
    }

    @Test
    void getTransactionHistory_success() {
        Wallet w = new Wallet();
        w.setUserId("user-1");
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(w));

        List<WalletTransaction> list = new ArrayList<>();
        WalletTransaction t = new WalletTransaction();
        t.setTransactionId("tx-1");
        t.setCreatedAt(LocalDateTime.now());
        list.add(t);

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(list);

        TransactionHistoryResponse response = walletService.getTransactionHistory("user-1");

        assertNotNull(response);
        assertEquals(1, response.getTotalCount());
    }

    @Test
    void getTransactionHistory_validationFailure() {
        assertThrows(PaymentException.class, () -> walletService.getTransactionHistory(""));
    }

    @Test
    void getTrainerWallet_success() {
        Wallet w = new Wallet();
        w.setUserId("trainer-1");
        w.setBalance(BigDecimal.valueOf(50));
        w.setStatus("ACTIVE");

        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(w));

        WalletResponse response = walletService.getTrainerWallet("trainer-1");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(50), response.getBalance());
    }

    @Test
    void getTrainerWallet_validationFailure() {
        assertThrows(PaymentException.class, () -> walletService.getTrainerWallet(""));
    }

    @Test
    void requestPayout_success() {
        PayoutRequestDto request = new PayoutRequestDto();
        request.setAmount(BigDecimal.valueOf(200));
        request.setBankAccount("123456789");
        request.setIfscCode("IFSC0001");
        request.setAccountHolderName("John Doe");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setBalance(BigDecimal.valueOf(500));
        wallet.setPendingPayout(BigDecimal.ZERO);
        wallet.setStatus("ACTIVE");

        when(jwtUtil.extractUserId(anyString())).thenReturn("trainer-1");
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));
        when(payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING")).thenReturn(new ArrayList<>());

        String payoutId = walletService.requestPayout(request, "token");

        assertNotNull(payoutId);
        assertEquals(BigDecimal.valueOf(300), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), wallet.getPendingPayout());
    }

    @Test
    void requestPayout_validationFailures() {
        PayoutRequestDto request = new PayoutRequestDto();
        request.setAmount(BigDecimal.valueOf(50)); // low amount
        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token"));

        request.setAmount(BigDecimal.valueOf(200));
        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token")); // missing bank

        request.setBankAccount("1234");
        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token")); // missing ifsc

        request.setIfscCode("IFSC");
        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token")); // missing name
    }

    @Test
    void requestPayout_suspendedWallet() {
        PayoutRequestDto request = new PayoutRequestDto();
        request.setAmount(BigDecimal.valueOf(200));
        request.setBankAccount("1234");
        request.setIfscCode("IFSC");
        request.setAccountHolderName("Name");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setStatus("SUSPENDED");

        when(jwtUtil.extractUserId(anyString())).thenReturn("trainer-1");
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token"));
    }

    @Test
    void requestPayout_insufficientBalance() {
        PayoutRequestDto request = new PayoutRequestDto();
        request.setAmount(BigDecimal.valueOf(200));
        request.setBankAccount("1234");
        request.setIfscCode("IFSC");
        request.setAccountHolderName("Name");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setBalance(BigDecimal.valueOf(100));
        wallet.setStatus("ACTIVE");

        when(jwtUtil.extractUserId(anyString())).thenReturn("trainer-1");
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token"));
    }

    @Test
    void requestPayout_hasPending() {
        PayoutRequestDto request = new PayoutRequestDto();
        request.setAmount(BigDecimal.valueOf(200));
        request.setBankAccount("1234");
        request.setIfscCode("IFSC");
        request.setAccountHolderName("Name");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setBalance(BigDecimal.valueOf(500));
        wallet.setStatus("ACTIVE");

        List<PayoutRequest> pendings = new ArrayList<>();
        PayoutRequest pr = new PayoutRequest();
        pr.setTrainerId("trainer-1");
        pendings.add(pr);

        when(jwtUtil.extractUserId(anyString())).thenReturn("trainer-1");
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));
        when(payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING")).thenReturn(pendings);

        assertThrows(PaymentException.class, () -> walletService.requestPayout(request, "token"));
    }

    @Test
    void getAdminDashboard_success() {
        List<WalletTransaction> txs = new ArrayList<>();
        WalletTransaction t1 = new WalletTransaction();
        t1.setStatus("SUCCESS");
        t1.setTransactionType("BUY_COURSE");
        t1.setAmount(BigDecimal.valueOf(100));
        txs.add(t1);

        List<Wallet> wallets = new ArrayList<>();
        Wallet w1 = new Wallet();
        w1.setUserType("LEARNER");
        w1.setStatus("ACTIVE");
        wallets.add(w1);

        when(transactionRepository.findAll()).thenReturn(txs);
        when(walletRepository.findAll()).thenReturn(wallets);

        AdminDashboardResponse res = walletService.getAdminDashboard();

        assertNotNull(res);
        assertEquals(BigDecimal.valueOf(100), res.getTotalRevenue());
        assertEquals(1, res.getActiveLearners());
    }

    @Test
    void processPayoutRequest_approve() {
        PayoutRequest payout = new PayoutRequest();
        payout.setPayoutId("payout-1");
        payout.setTrainerId("trainer-1");
        payout.setAmount(BigDecimal.valueOf(100));
        payout.setStatus("PENDING");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setPendingPayout(BigDecimal.valueOf(100));

        when(payoutRequestRepository.findById("payout-1")).thenReturn(Optional.of(payout));
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));

        walletService.processPayoutRequest("payout-1", "admin-1", "APPROVE", "Approved");

        assertEquals("PROCESSED", payout.getStatus());
        assertEquals(BigDecimal.ZERO, wallet.getPendingPayout());
    }

    @Test
    void processPayoutRequest_hold() {
        PayoutRequest payout = new PayoutRequest();
        payout.setPayoutId("payout-1");
        payout.setTrainerId("trainer-1");
        payout.setAmount(BigDecimal.valueOf(100));
        payout.setStatus("PENDING");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setPendingPayout(BigDecimal.valueOf(100));

        when(payoutRequestRepository.findById("payout-1")).thenReturn(Optional.of(payout));
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));

        walletService.processPayoutRequest("payout-1", "admin-1", "HOLD", "Hold it");

        assertEquals("HOLD", payout.getStatus());
    }

    @Test
    void processPayoutRequest_reject() {
        PayoutRequest payout = new PayoutRequest();
        payout.setPayoutId("payout-1");
        payout.setTrainerId("trainer-1");
        payout.setAmount(BigDecimal.valueOf(100));
        payout.setStatus("PENDING");

        Wallet wallet = new Wallet();
        wallet.setUserId("trainer-1");
        wallet.setBalance(BigDecimal.valueOf(500));
        wallet.setPendingPayout(BigDecimal.valueOf(100));

        when(payoutRequestRepository.findById("payout-1")).thenReturn(Optional.of(payout));
        when(walletRepository.findByUserId("trainer-1")).thenReturn(Optional.of(wallet));

        walletService.processPayoutRequest("payout-1", "admin-1", "REJECT", "Rejected");

        assertEquals("REJECTED", payout.getStatus());
        assertEquals(BigDecimal.valueOf(600), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, wallet.getPendingPayout());
    }

    @Test
    void processPayoutRequest_validationFailures() {
        assertThrows(PaymentException.class, () -> walletService.processPayoutRequest("", "admin-1", "APPROVE", ""));
        assertThrows(PaymentException.class, () -> walletService.processPayoutRequest("payout-1", "", "APPROVE", ""));
        assertThrows(PaymentException.class, () -> walletService.processPayoutRequest("payout-1", "admin-1", "INVALID", ""));
    }

    @Test
    void verifyAddFundsPayment_success() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId("user-1");
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setStatus("PENDING");

        List<WalletTransaction> txs = new ArrayList<>();
        txs.add(transaction);

        Wallet wallet = new Wallet();
        wallet.setUserId("user-1");
        wallet.setBalance(BigDecimal.valueOf(50));

        when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(txs);
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));

        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order-1");
        request.setRazorpayPaymentId("pay-1");
        request.setRazorpaySignature("sig-1");

        try (MockedStatic<com.razorpay.Utils> mockedUtils = Mockito.mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(inv -> null);

            walletService.verifyAddFundsPayment(request);

            assertEquals("SUCCESS", transaction.getStatus());
            assertEquals(BigDecimal.valueOf(150), wallet.getBalance());
        }
    }

    @Test
    void confirmAddFundsTest_success() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId("user-1");
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setStatus("PENDING");

        List<WalletTransaction> txs = new ArrayList<>();
        txs.add(transaction);

        Wallet wallet = new Wallet();
        wallet.setUserId("user-1");
        wallet.setBalance(BigDecimal.valueOf(50));

        when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(txs);
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));

        AddFundsConfirmRequest request = new AddFundsConfirmRequest();
        request.setRazorpayOrderId("order-1");
        request.setRazorpayPaymentId("pay-1");

        walletService.confirmAddFundsTest(request);

        assertEquals("SUCCESS", transaction.getStatus());
        assertEquals(BigDecimal.valueOf(150), wallet.getBalance());
    }

    @Test
    void generateTestSignature_success() {
        String sig = walletService.generateTestSignature("order-1", "pay-1");
        assertNotNull(sig);
    }

    @Test
    void verifyPaymentLink_success() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId("user-1");
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setStatus("PENDING");

        List<WalletTransaction> txs = new ArrayList<>();
        txs.add(transaction);

        Wallet wallet = new Wallet();
        wallet.setUserId("user-1");
        wallet.setBalance(BigDecimal.valueOf(50));
        wallet.setStatus("ACTIVE");

        when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(txs);
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));

        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 10000L); // 100 INR in paise
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            walletService.verifyPaymentLink("plink-1", 10000L, "order-1");

            assertEquals("SUCCESS", transaction.getStatus());
            assertEquals(BigDecimal.valueOf(150), wallet.getBalance());
        }
    }

    @Test
    void addFunds_walletNotFound() {
        AddFundsRequest request = new AddFundsRequest();
        request.setUserId("unknown-user");
        request.setAmount(BigDecimal.valueOf(100));

        when(walletRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.addFunds(request, "LEARNER"));
    }

    @Test
    void addFunds_closedWallet() {
        AddFundsRequest request = new AddFundsRequest();
        request.setUserId("user-123");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setStatus("CLOSED");

        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));

        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));
    }

    @Test
    void addFunds_genericException() throws Exception {
        AddFundsRequest request = new AddFundsRequest();
        request.setUserId("user-123");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));
        when(razorpayClient.orders.create(any(JSONObject.class))).thenThrow(new RuntimeException("Razorpay down"));

        assertThrows(PaymentException.class, () -> walletService.addFunds(request, "LEARNER"));
    }

    @Test
    void getWalletBalance_walletNotFound() {
        when(walletRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletBalance("unknown-user"));
        assertThrows(PaymentException.class, () -> walletService.getWalletBalance(""));
    }

    @Test
    void getTransactionHistory_walletNotFound() {
        when(walletRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.getTransactionHistory("unknown-user"));
        assertThrows(PaymentException.class, () -> walletService.getTransactionHistory(""));
    }

    @Test
    void getTrainerWallet_walletNotFound() {
        when(walletRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.getTrainerWallet("unknown-user"));
        assertThrows(PaymentException.class, () -> walletService.getTrainerWallet(""));
    }

    @Test
    void getAllTransactionHistory_success() {
        WalletTransaction t1 = new WalletTransaction();
        t1.setTransactionId("t1");
        t1.setCreatedAt(LocalDateTime.of(2026, 6, 1, 12, 0));

        WalletTransaction t2 = new WalletTransaction();
        t2.setTransactionId("t2");
        t2.setCreatedAt(LocalDateTime.of(2026, 6, 2, 12, 0));

        WalletTransaction t3 = new WalletTransaction();
        t3.setTransactionId("t3");
        t3.setCreatedAt(null);

        WalletTransaction t4 = new WalletTransaction();
        t4.setTransactionId("t4");
        t4.setCreatedAt(null);

        List<WalletTransaction> list = List.of(t1, t2, t3, t4);
        when(transactionRepository.findAll()).thenReturn(list);

        TransactionHistoryResponse res = walletService.getAllTransactionHistory();
        assertNotNull(res);
        assertEquals(4, res.getTotalCount());
        // Verify sorting (t2, then t1, then nulls)
        assertEquals("t2", res.getTransactions().get(0).getTransactionId());
        assertEquals("t1", res.getTransactions().get(1).getTransactionId());
    }

    @Test
    void getAllPayouts_success() {
        PayoutRequest p = new PayoutRequest();
        when(payoutRequestRepository.findAll()).thenReturn(List.of(p));
        List<PayoutRequest> res = walletService.getAllPayouts();
        assertNotNull(res);
        assertEquals(1, sizeOf(res));
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    @Test
    void confirmAddFunds_validationFailures() {
        AddFundsConfirmRequest req = new AddFundsConfirmRequest();
        
        // order ID empty
        assertThrows(PaymentException.class, () -> walletService.confirmAddFunds(req));

        req.setRazorpayOrderId("order-123");
        // payment ID empty
        assertThrows(PaymentException.class, () -> walletService.confirmAddFunds(req));

        req.setRazorpayPaymentId("pay-123");
        // signature empty
        assertThrows(PaymentException.class, () -> walletService.confirmAddFunds(req));
    }

    @Test
    void buyCourse_walletNotFound() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("unknown-user");
        request.setCourseId("course-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        when(walletRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void buyCourse_genericException() {
        BuyCourseRequest request = new BuyCourseRequest();
        request.setUserId("learner-1");
        request.setCourseId("course-1");
        request.setTrainerId("trainer-1");
        request.setAmount(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setUserId("learner-1");
        wallet.setBalance(BigDecimal.valueOf(200));
        wallet.setStatus("ACTIVE");

        when(walletRepository.findByUserId("learner-1")).thenReturn(Optional.of(wallet));
        // Force mock error by throwing runtime exception during save
        when(walletRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(PaymentException.class, () -> walletService.buyCourse(request));
    }

    @Test
    void verifyPaymentLink_notCompleted() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "issued");
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }

    @Test
    void verifyPaymentLink_amountMismatch() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 5000L); // mismatch
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }

    @Test
    void verifyPaymentLink_orderNotFound() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 10000L);
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(new ArrayList<>());
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }

    @Test
    void verifyPaymentLink_alreadyProcessed() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 10000L);
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            WalletTransaction tx = new WalletTransaction();
            tx.setStatus("SUCCESS");
            when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(List.of(tx));
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }

    @Test
    void verifyPaymentLink_alreadyFailed() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 10000L);
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            WalletTransaction tx = new WalletTransaction();
            tx.setStatus("FAILED");
            when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(List.of(tx));
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }

    @Test
    void verifyPaymentLink_walletNotFound() {
        try (MockedConstruction<RazorpayClient> mocked = Mockito.mockConstruction(RazorpayClient.class, (mock, context) -> {
            mock.paymentLink = mock(com.razorpay.PaymentLinkClient.class);
            PaymentLink link = mock(PaymentLink.class);
            JSONObject json = new JSONObject();
            json.put("status", "paid");
            json.put("amount_paid", 10000L);
            when(link.toJson()).thenReturn(json);
            when(mock.paymentLink.fetch("plink-1")).thenReturn(link);
        })) {
            WalletTransaction tx = new WalletTransaction();
            tx.setStatus("PENDING");
            tx.setUserId("user-1");
            when(transactionRepository.findByRazorpayOrderId("order-1")).thenReturn(List.of(tx));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.empty());
            assertThrows(PaymentException.class, () -> walletService.verifyPaymentLink("plink-1", 10000L, "order-1"));
        }
    }
}
