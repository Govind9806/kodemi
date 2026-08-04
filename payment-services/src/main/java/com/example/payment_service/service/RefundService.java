package com.example.payment_service.service;

import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.model.Wallet;
import com.example.payment_service.model.WalletTransaction;
import com.example.payment_service.repository.WalletRepository;
import com.example.payment_service.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import com.example.payment_service.service.notification.NotificationPublisher;
import com.example.payment_service.dto.notification.NotificationRequest;
import com.example.payment_service.dto.notification.NotificationType;
import com.example.payment_service.dto.notification.NotificationChannel;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public void processRefund(String originalTransactionId, String adminId, String reason) {
        try {
            log.info("Processing refund request | originalTransactionId: {}, adminId: {}, reason: {}",
                    originalTransactionId, adminId, reason);
            // Find original transaction
            java.util.Optional<WalletTransaction> optTx = transactionRepository.findById(originalTransactionId);
            if (!optTx.isPresent()) {
                log.warn("Original transaction not found for ID: {}", originalTransactionId);
                throw new PaymentException("Original transaction not found");
            }
            WalletTransaction originalTransaction = optTx.get();
            log.info("Found original transaction | ID: {}, status: {}, type: {}, amount: {}",
                    originalTransaction.getTransactionId(), originalTransaction.getStatus(), originalTransaction.getTransactionType(), originalTransaction.getAmount());

            if (!"SUCCESS".equals(originalTransaction.getStatus())) {
                log.warn("Cannot refund transaction {} because status is: {}", originalTransactionId, originalTransaction.getStatus());
                throw new PaymentException("Cannot refund non-successful transaction");
            }
            
            try {
                NotificationRequest initNotif = NotificationRequest.builder()
                        .userId(originalTransaction.getUserId())
                        .title("Refund Initiated")
                        .message("We have initiated a refund for your transaction.")
                        .type(NotificationType.REFUND_INITIATED)
                        .channels(List.of(NotificationChannel.EMAIL))
                        .referenceId(originalTransactionId)
                        .referenceType("TRANSACTION")
                        .build();
                notificationPublisher.publish(initNotif);
            } catch (Exception ex) {
                log.error("Failed to send REFUND_INITIATED notification", ex);
            }

            // Get learner wallet
            java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(originalTransaction.getUserId());
            if (!optWallet.isPresent()) {
                log.error("Learner wallet not found for userId: {}", originalTransaction.getUserId());
                throw new PaymentException("Learner wallet not found");
            }
            Wallet learnerWallet = optWallet.get();
            log.info("Learner wallet found | userId: {}, currentBalance: {}", learnerWallet.getUserId(), learnerWallet.getBalance());

            // Create refund transaction
            WalletTransaction refundTransaction = new WalletTransaction();
            refundTransaction.setTransactionId(UUID.randomUUID().toString());
            refundTransaction.setUserId(originalTransaction.getUserId());
            refundTransaction.setTransactionType("REFUND");
            refundTransaction.setAmount(originalTransaction.getAmount());
            refundTransaction.setDescription("Refund for: " + originalTransaction.getDescription() + " - Reason: " + reason);
            refundTransaction.setReferenceId(originalTransactionId);
            refundTransaction.setStatus("SUCCESS");
            refundTransaction.setBalanceBefore(learnerWallet.getBalance());
            refundTransaction.setBalanceAfter(learnerWallet.getBalance().add(originalTransaction.getAmount()));
            refundTransaction.setCreatedAt(LocalDateTime.now());

            // Update learner wallet
            learnerWallet.setBalance(learnerWallet.getBalance().add(originalTransaction.getAmount()));
            learnerWallet.setTotalSpent(learnerWallet.getTotalSpent().subtract(originalTransaction.getAmount()));
            learnerWallet.setUpdatedAt(LocalDateTime.now());
            log.info("Calculated new learner wallet balance | balanceBefore: {}, balanceAfter: {}", refundTransaction.getBalanceBefore(), refundTransaction.getBalanceAfter());

            // If it was a course/resource purchase, deduct from trainer earnings
            if ("BUY_COURSE".equals(originalTransaction.getTransactionType()) || 
                "BUY_RESOURCE".equals(originalTransaction.getTransactionType())) {
                log.info("Deducting refund amount from trainer earnings for transaction type: {}", originalTransaction.getTransactionType());
                reverseTrainerEarning(originalTransaction);
            }

            // Mark original transaction as refunded
            originalTransaction.setStatus("REFUNDED");
            originalTransaction.setUpdatedAt(LocalDateTime.now());

            transactionRepository.save(refundTransaction);
            transactionRepository.save(originalTransaction);
            walletRepository.save(learnerWallet);

            log.info("Refund processed successfully | originalTransactionId: {}, refundTransactionId: {}", originalTransactionId, refundTransaction.getTransactionId());

            try {
                NotificationRequest compNotif = NotificationRequest.builder()
                        .userId(originalTransaction.getUserId())
                        .title("Refund Completed")
                        .message("Your refund has been successfully processed to your wallet.")
                        .type(NotificationType.REFUND_COMPLETED)
                        .channels(List.of(NotificationChannel.EMAIL))
                        .referenceId(refundTransaction.getTransactionId())
                        .referenceType("REFUND")
                        .build();
                notificationPublisher.publish(compNotif);
            } catch (Exception ex) {
                log.error("Failed to send REFUND_COMPLETED notification", ex);
            }

        } catch (Exception e) {
            log.error("Error processing refund for transaction: {}", originalTransactionId, e);
            throw new PaymentException("Failed to process refund");
        }
    }

    private void reverseTrainerEarning(WalletTransaction originalTransaction) {
        // Calculate trainer share (80% of original amount)
        BigDecimal trainerShare = originalTransaction.getAmount().multiply(BigDecimal.valueOf(0.8));
        
        // This would typically involve finding the trainer from the course/resource
        // For now, we'll create a placeholder method
        // In a real implementation, you'd need to fetch trainer ID from course/resource service
        
        log.info("Trainer earning reversal needed for amount: {}", trainerShare);
        // TODO: Implement trainer earning reversal logic
    }
}