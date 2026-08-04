package com.example.payment_service.service;

import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.model.Wallet;
import com.example.payment_service.model.WalletTransaction;
import com.example.payment_service.repository.WalletRepository;
import com.example.payment_service.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private RefundService refundService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processRefund_success() {
        WalletTransaction originalTx = new WalletTransaction();
        originalTx.setTransactionId("tx-123");
        originalTx.setUserId("user-123");
        originalTx.setStatus("SUCCESS");
        originalTx.setAmount(BigDecimal.valueOf(500));
        originalTx.setTransactionType("BUY_COURSE");
        originalTx.setDescription("Purchase Course");

        Wallet wallet = new Wallet();
        wallet.setUserId("user-123");
        wallet.setBalance(BigDecimal.valueOf(100));
        wallet.setTotalSpent(BigDecimal.valueOf(500));

        when(transactionRepository.findById("tx-123")).thenReturn(Optional.of(originalTx));
        when(walletRepository.findByUserId("user-123")).thenReturn(Optional.of(wallet));

        refundService.processRefund("tx-123", "admin-1", "Duplicate purchase");

        assertEquals(BigDecimal.valueOf(600), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(0), wallet.getTotalSpent());
        assertEquals("REFUNDED", originalTx.getStatus());

        verify(transactionRepository, times(2)).save(any(WalletTransaction.class));
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void processRefund_txNotFound() {
        when(transactionRepository.findById("tx-123")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> {
            refundService.processRefund("tx-123", "admin-1", "reason");
        });
    }

    @Test
    void processRefund_txNotSuccessful() {
        WalletTransaction originalTx = new WalletTransaction();
        originalTx.setTransactionId("tx-123");
        originalTx.setStatus("FAILED");

        when(transactionRepository.findById("tx-123")).thenReturn(Optional.of(originalTx));

        assertThrows(PaymentException.class, () -> {
            refundService.processRefund("tx-123", "admin-1", "reason");
        });
    }
}
