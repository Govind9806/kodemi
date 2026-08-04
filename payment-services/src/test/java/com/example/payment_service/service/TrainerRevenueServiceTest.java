package com.example.payment_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.payment_service.dto.response.TrainerRevenueResponse;
import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.exception.WalletNotFoundException;
import com.example.payment_service.model.Payment;
import com.example.payment_service.model.Wallet;
import com.example.payment_service.model.WalletTransaction;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.repository.WalletRepository;
import com.example.payment_service.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class TrainerRevenueServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private WalletService walletService;

    private String trainerId = "trainer-001";
    private Wallet trainerWallet;

    @BeforeEach
    public void setUp() {
        trainerWallet = new Wallet();
        trainerWallet.setUserId(trainerId);
        trainerWallet.setUserType("TRAINER");
        trainerWallet.setBalance(new BigDecimal("25000.00"));
        trainerWallet.setTotalEarned(new BigDecimal("45000.00"));
        trainerWallet.setTotalSpent(BigDecimal.ZERO);
        trainerWallet.setPendingPayout(new BigDecimal("10000.00"));
        trainerWallet.setStatus("ACTIVE");
        trainerWallet.setUpdatedAt(LocalDateTime.now());
    }

    // ============ TEST 1: Basic Revenue Analytics ============
    @Test
    public void testGetTrainerRevenueAnalytics_Success() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertNotNull(response);
        assertEquals(trainerId, response.getTrainerId());
        assertEquals(2, response.getTotalCoursesSold()); // 2 course purchases
        assertEquals(1, response.getTotalResourcesSold()); // 1 resource purchase
        assertTrue(response.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("25000.00"), response.getBalance());

        verify(walletRepository, times(1)).findByUserId(trainerId);
        verify(transactionRepository, times(1)).findByUserIdAndTransactionTypes(eq(trainerId), anyList());
        verify(paymentRepository, times(1)).findByUserId(trainerId);
    }

    // ============ TEST 2: Wallet Not Found ============
    @Test
    public void testGetTrainerRevenueAnalytics_WalletNotFound() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class, () -> {
            walletService.getTrainerRevenueAnalytics(trainerId);
        });

        verify(walletRepository, times(1)).findByUserId(trainerId);
    }

    // ============ TEST 3: Invalid Trainer ID ============
    @Test
    public void testGetTrainerRevenueAnalytics_InvalidTrainerId() {
        // Act & Assert
        assertThrows(PaymentException.class, () -> {
            walletService.getTrainerRevenueAnalytics(null);
        });

        assertThrows(PaymentException.class, () -> {
            walletService.getTrainerRevenueAnalytics("");
        });
    }

    // ============ TEST 4: No Sales History ============
    @Test
    public void testGetTrainerRevenueAnalytics_NoSales() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(new ArrayList<>()); // No transactions
        when(paymentRepository.findByUserId(trainerId)).thenReturn(new ArrayList<>()); // No payments

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertNotNull(response);
        assertEquals(0L, response.getTotalCoursesSold());
        assertEquals(0L, response.getTotalResourcesSold());
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
        assertTrue(response.getCourseRevenue().isEmpty());
        assertTrue(response.getResourceRevenue().isEmpty());
        assertTrue(response.getMonthlyBreakdown().isEmpty());
    }

    // ============ TEST 5: Course Revenue Aggregation ============
    @Test
    public void testGetTrainerRevenueAnalytics_CourseAggregation() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertNotNull(response.getCourseRevenue());
        assertTrue(response.getCourseRevenue().size() > 0);

        // Verify course breakdown contains correct data
        TrainerRevenueResponse.RevenueByCourse course = response.getCourseRevenue().get(0);
        assertNotNull(course.getCourseId());
        assertTrue(course.getUnitsSold() > 0);
        assertTrue(course.getRevenue().compareTo(BigDecimal.ZERO) > 0);
    }

    // ============ TEST 6: Resource Revenue Aggregation ============
    @Test
    public void testGetTrainerRevenueAnalytics_ResourceAggregation() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertNotNull(response.getResourceRevenue());
        assertTrue(response.getResourceRevenue().size() > 0);

        // Verify resource breakdown contains correct data
        TrainerRevenueResponse.RevenueByResource resource = response.getResourceRevenue().get(0);
        assertNotNull(resource.getResourceId());
        assertTrue(resource.getUnitsSold() > 0);
        assertTrue(resource.getRevenue().compareTo(BigDecimal.ZERO) > 0);
    }

    // ============ TEST 7: Monthly Revenue Breakdown ============
    @Test
    public void testGetTrainerRevenueAnalytics_MonthlyBreakdown() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertNotNull(response.getMonthlyBreakdown());
        assertTrue(response.getMonthlyBreakdown().size() > 0);

        // Verify monthly data
        TrainerRevenueResponse.MonthlyRevenue monthly = response.getMonthlyBreakdown().get(0);
        assertNotNull(monthly.getMonth());
        assertTrue(monthly.getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(monthly.getTransactionCount() > 0);
    }

    // ============ TEST 8: Data Consistency ============
    @Test
    public void testGetTrainerRevenueAnalytics_DataConsistency() {
        // Arrange - Total mock transactions = 14000.00 (8000 + 6000)
        Wallet consistentWallet = new Wallet();
        consistentWallet.setUserId(trainerId);
        consistentWallet.setBalance(new BigDecimal("8000.00"));
        consistentWallet.setPendingPayout(new BigDecimal("2000.00"));
        consistentWallet.setTotalEarned(new BigDecimal("14000.00")); // totalWithdrawn = 14000 - 8000 - 2000 = 4000
        consistentWallet.setUpdatedAt(LocalDateTime.now());

        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(consistentWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert - Verify formula: balance + pending + withdrawn = totalEarned (which equals totalRevenue)
        BigDecimal sum = response.getBalance()
                .add(response.getPendingPayout())
                .add(response.getTotalWithdrawn());

        assertEquals(0, sum.compareTo(response.getTotalRevenue()),
                "Data consistency check failed: balance + pending + withdrawn should equal totalRevenue");
    }

    // ============ TEST 9: Multiple Course Sales ============
    @Test
    public void testGetTrainerRevenueAnalytics_MultipleCourses() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        List<Payment> payments = new ArrayList<>();

        // Add payments for different courses
        for (int i = 1; i <= 5; i++) {
            payments.add(createPayment("course-00" + i, "RECORDED_COURSE", new BigDecimal("1000")));
        }
        when(paymentRepository.findByUserId(trainerId)).thenReturn(payments);

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert
        assertTrue(response.getTotalCoursesSold() >= 5);
    }

    // ============ TEST 10: Monthly Sorting ============
    @Test
    public void testGetTrainerRevenueAnalytics_MonthlySorting() {
        // Arrange
        when(walletRepository.findByUserId(trainerId)).thenReturn(Optional.of(trainerWallet));
        when(transactionRepository.findByUserIdAndTransactionTypes(eq(trainerId), anyList()))
                .thenReturn(createMockTransactions());
        when(paymentRepository.findByUserId(trainerId)).thenReturn(createMockPayments());

        // Act
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);

        // Assert - Verify months are sorted descending
        List<TrainerRevenueResponse.MonthlyRevenue> monthlyBreakdown = response.getMonthlyBreakdown();
        for (int i = 0; i < monthlyBreakdown.size() - 1; i++) {
            String current = monthlyBreakdown.get(i).getMonth();
            String next = monthlyBreakdown.get(i + 1).getMonth();
            assertTrue(current.compareTo(next) >= 0, "Monthly breakdown should be sorted descending");
        }
    }

    // ============ HELPER METHODS ============

    private List<WalletTransaction> createMockTransactions() {
        List<WalletTransaction> transactions = new ArrayList<>();

        WalletTransaction txn1 = new WalletTransaction();
        txn1.setTransactionId("txn-001");
        txn1.setUserId(trainerId);
        txn1.setTransactionType("EARNING");
        txn1.setAmount(new BigDecimal("8000.00"));
        txn1.setStatus("SUCCESS");
        txn1.setCreatedAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        transactions.add(txn1);

        WalletTransaction txn2 = new WalletTransaction();
        txn2.setTransactionId("txn-002");
        txn2.setUserId(trainerId);
        txn2.setTransactionType("EARNING");
        txn2.setAmount(new BigDecimal("6000.00"));
        txn2.setStatus("SUCCESS");
        txn2.setCreatedAt(LocalDateTime.of(2026, 6, 20, 10, 0));
        transactions.add(txn2);

        return transactions;
    }

    private List<Payment> createMockPayments() {
        List<Payment> payments = new ArrayList<>();

        // Course 1
        Payment p1 = createPayment("course-001", "RECORDED_COURSE", new BigDecimal("10000"));
        payments.add(p1);

        // Course 2
        Payment p2 = createPayment("course-002", "LIVE_COURSE", new BigDecimal("5000"));
        payments.add(p2);

        // Resource 1
        Payment p3 = createPayment("resource-001", "RESOURCE", new BigDecimal("2000"));
        payments.add(p3);

        return payments;
    }

    private Payment createPayment(String targetId, String targetType, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrderId("order-" + System.nanoTime());
        payment.setUserId(trainerId);
        payment.setTargetId(targetId);
        payment.setTargetType(targetType);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
}
