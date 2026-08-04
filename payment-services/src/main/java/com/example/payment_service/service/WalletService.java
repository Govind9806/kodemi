package com.example.payment_service.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.payment_service.dto.request.AddFundsConfirmRequest;
import com.example.payment_service.dto.request.AddFundsRequest;
import com.example.payment_service.dto.request.BuyCourseRequest;
import com.example.payment_service.dto.request.BuyResourceRequest;
import com.example.payment_service.dto.request.PaymentVerifyRequest;
import com.example.payment_service.dto.request.PayoutRequestDto;
import com.example.payment_service.dto.response.AdminDashboardResponse;
import com.example.payment_service.dto.response.PaymentOrderResponse;
import com.example.payment_service.dto.response.TransactionHistoryResponse;
import com.example.payment_service.dto.response.TrainerRevenueResponse;
import com.example.payment_service.dto.response.WalletResponse;
import com.example.payment_service.model.Payment;
import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.exception.WalletNotFoundException;
import com.example.payment_service.service.notification.NotificationPublisher;
import com.example.payment_service.dto.notification.NotificationRequest;
import com.example.payment_service.dto.notification.BroadcastNotificationRequest;
import com.example.payment_service.dto.notification.NotificationType;
import com.example.payment_service.dto.notification.NotificationChannel;
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
import com.example.payment_service.feign.EnrollmentClient;
import com.example.payment_service.dto.request.PaymentSuccessRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final NotificationPublisher notificationPublisher;
    private final JwtUtil jwtUtil;
    private final EnrollmentClient enrollmentClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${wallet.max-recharge-amount:1000000}")
    private BigDecimal maxRechargeAmount;

    // ─── Public: Create Wallet ────────────────────────────────────────────────

    public Wallet createWallet(String userId, String userType) {
        if (userId == null || userId.isBlank())
            throw new PaymentException("userId is required");
        if (userType == null || userType.isBlank())
            throw new PaymentException("userType is required (LEARNER or TRAINER)");
        return createNewWallet(userId, userType);
    }

    // ─── Learner: Add Funds ───────────────────────────────────────────────────

    public PaymentOrderResponse addFunds(AddFundsRequest request, String userType) {
        // Validate request fields
        if (request.getUserId() == null || request.getUserId().isBlank())
            throw new PaymentException("userId is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ONE) < 0)
            throw new PaymentException("Amount must be at least ₹1");
        if (request.getAmount().compareTo(maxRechargeAmount) > 0)
            throw new PaymentException("Amount cannot exceed ₹" + java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN")).format(maxRechargeAmount).replace("₹", "") + " per transaction");

        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(request.getUserId());
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException(request.getUserId());
        }
        Wallet wallet = optWallet.get();
        if ("SUSPENDED".equals(wallet.getStatus()) || "CLOSED".equals(wallet.getStatus()))
            throw new PaymentException("Wallet is " + wallet.getStatus().toLowerCase() + " and cannot receive funds");

        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in subunits (paise for INR)
            BigDecimal amountInPaise = request.getAmount().multiply(new BigDecimal(100));
            orderRequest.put("amount", amountInPaise.intValue());
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", "wlt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));

            JSONObject paymentLinkRequest = new JSONObject();

            paymentLinkRequest.put("amount", amountInPaise.longValue());
            paymentLinkRequest.put("currency",
                    request.getCurrency() != null ? request.getCurrency() : "INR");
            paymentLinkRequest.put("description", "Wallet Recharge");
            paymentLinkRequest.put("reference_id",
                    "wlt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));

            paymentLinkRequest.put(
                    "callback_url",
                    request.getReturnUrl()
            );

            paymentLinkRequest.put(
                    "callback_method",
                    "get"
            );

            Order order = razorpayClient.orders.create(orderRequest);

            PaymentLink paymentLink =
                    razorpayClient.paymentLink.create(paymentLinkRequest);

            String paymentUrl = paymentLink.get("short_url");
            String paymentUrlId = paymentLink.get("id");
            WalletTransaction transaction = new WalletTransaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setUserId(request.getUserId());
            transaction.setTransactionType("ADD_FUNDS");
            transaction.setAmount(request.getAmount());
            transaction.setDescription("Add funds to wallet");
            transaction.setRazorpayOrderId(order.get("id"));
            transaction.setStatus("PENDING");
            transaction.setCreatedAt(now());

            transactionRepository.save(transaction);

            return new PaymentOrderResponse(order.get("id"), request.getAmount(), amountInPaise.longValue(),
                    request.getCurrency() != null ? request.getCurrency() : "INR", "CREATED", paymentUrl, paymentUrlId);

        } catch (PaymentException | WalletNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating add funds order: {}", e.getMessage(), e);
            throw new PaymentException("Failed to create add funds order: " + e.getMessage());
        }
    }

    // ─── Learner: Confirm Add Funds (after Razorpay payment) ─────────────────

    public void confirmAddFunds(AddFundsConfirmRequest request) {
        log.info("Received Confirm Request: {}", request);

        if (request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank())
            throw new PaymentException("razorpay_order_id is required. Received: " + request);
        if (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank())
            throw new PaymentException("razorpay_payment_id is required");
        if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank())
            throw new PaymentException("razorpay_signature is required");

        verifyRazorpaySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        List<WalletTransaction> existing = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
        if (existing.isEmpty())
            throw new PaymentException("Order not found — possible replay attack");

        WalletTransaction transaction = existing.get(0);

        if ("SUCCESS".equals(transaction.getStatus()))
            throw new PaymentException("Payment already processed");
        if ("FAILED".equals(transaction.getStatus()))
            throw new PaymentException("This payment has already been marked as failed");

        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(transaction.getUserId());
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for user: " + transaction.getUserId());
        }
        Wallet wallet = optWallet.get();

        if ("SUSPENDED".equals(wallet.getStatus()) || "CLOSED".equals(wallet.getStatus()))
            throw new PaymentException("Wallet is " + wallet.getStatus().toLowerCase() + " and cannot receive funds");

        transaction.setStatus("SUCCESS");
        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setBalanceBefore(wallet.getBalance());
        transaction.setBalanceAfter(wallet.getBalance().add(transaction.getAmount()));
        transaction.setUpdatedAt(now());

        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        wallet.setUpdatedAt(now());

        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        log.info("Wallet credited ₹{} for user {} — paymentId: {}",
                transaction.getAmount(), transaction.getUserId(), request.getRazorpayPaymentId());

        // Notify learner that wallet was topped up
        try {
            NotificationRequest notif = NotificationRequest.builder()
                    .userId(transaction.getUserId())
                    .title("Wallet Topped Up ✅")
                    .message("Your wallet has been credited with ₹" + transaction.getAmount() + ". New balance: ₹" + wallet.getBalance() + ".")
                    .type(NotificationType.WALLET_TOPPED_UP)
                    .channels(java.util.List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceId(transaction.getTransactionId())
                    .referenceType("WALLET")
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception ex) {
            log.error("Failed to send WALLET_TOPPED_UP notification for userId {}", transaction.getUserId(), ex);
        }
    }

    // ─── Learner: Buy Course ──────────────────────────────────────────────────

    public PaymentOrderResponse buyCourse(BuyCourseRequest request) {
        log.info("Request to buy course starting | userId: {}, courseId: {}, trainerId: {}, amount: {}",
                request.getUserId(), request.getCourseId(), request.getTrainerId(), request.getAmount());
        if (request.getUserId() == null || request.getUserId().isBlank())
            throw new PaymentException("userId is required");
        if (request.getCourseId() == null || request.getCourseId().isBlank())
            throw new PaymentException("courseId is required");
        if (request.getTrainerId() == null || request.getTrainerId().isBlank())
            throw new PaymentException("trainerId is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new PaymentException("Amount must be greater than ₹0");

        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(request.getUserId());
        if (!optWallet.isPresent()) {
            log.warn("Wallet not found for user: {}", request.getUserId());
            throw new WalletNotFoundException("Wallet not found for user: " + request.getUserId());
        }
        Wallet wallet = optWallet.get();
        log.info("Learner wallet found | userId: {}, balance: {}, status: {}", wallet.getUserId(), wallet.getBalance(), wallet.getStatus());

        if ("SUSPENDED".equals(wallet.getStatus()) || "CLOSED".equals(wallet.getStatus())) {
            log.warn("Wallet is blocked: userId: {}, status: {}", wallet.getUserId(), wallet.getStatus());
            throw new PaymentException("Your wallet is " + wallet.getStatus().toLowerCase() + ". Please contact support");
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient wallet balance for userId: {} | required: {}, available: {}", wallet.getUserId(), request.getAmount(), wallet.getBalance());
            throw new PaymentException("Insufficient wallet balance. Available: ₹"
                    + wallet.getBalance() + ", Required: ₹" + request.getAmount());
        }

        // Prevent buying from yourself
        if (request.getUserId().equals(request.getTrainerId())) {
            log.warn("User {} tried to buy their own course", request.getUserId());
            throw new PaymentException("You cannot purchase your own course");
        }

        // Prevent duplicate purchase if already enrolled/purchased
        if (isAlreadyPurchased(request.getUserId(), request.getCourseId())) {
            log.warn("User {} is already enrolled in course {}", request.getUserId(), request.getCourseId());
            throw new PaymentException("You are already enrolled in this course");
        }

        try {
            WalletTransaction transaction = new WalletTransaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setUserId(request.getUserId());
            transaction.setTransactionType("BUY_COURSE");
            transaction.setAmount(request.getAmount());
            transaction.setDescription("Purchase course: " + request.getCourseName());
            transaction.setReferenceId(request.getCourseId());
            transaction.setStatus("SUCCESS");
            transaction.setBalanceBefore(wallet.getBalance());
            transaction.setBalanceAfter(wallet.getBalance().subtract(request.getAmount()));
            transaction.setCreatedAt(now());

            wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
            wallet.setTotalSpent(wallet.getTotalSpent().add(request.getAmount()));
            wallet.setUpdatedAt(now());

            updateTrainerEarnings(request.getTrainerId(), request.getAmount());
            walletRepository.save(wallet);
            transactionRepository.save(transaction);
            log.info("Updated trainer earnings and saved learner wallet transaction | transactionId: {}", transaction.getTransactionId());

            String targetType = request.getEffectiveTargetType();
            com.example.payment_service.model.Payment payment = com.example.payment_service.model.Payment.builder()
                    .orderId("wlt_ord_" + transaction.getTransactionId())
                    .paymentId("wlt_pay_" + transaction.getTransactionId())
                    .userId(request.getUserId())
                    .targetId(request.getCourseId())
                    .targetType(targetType)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            log.info("Persisted access verification payment for targetType={} to database", targetType);

            try {
                PaymentSuccessRequest enrollReq = PaymentSuccessRequest.builder()
                        .paymentId(payment.getPaymentId())
                        .orderId(payment.getOrderId())
                        .userId(request.getUserId())
                        .targetId(request.getCourseId())
                        .targetType(targetType)
                        .status("SUCCESS")
                        .build();
                log.info("Triggering enrollment activation for wallet purchase targetType={}: userId={}, courseId={}", targetType, request.getUserId(), request.getCourseId());
                enrollmentClient.markPaymentSuccess(enrollReq);
            } catch (Exception ex) {
                log.error("Failed to trigger enrollment activation for wallet course purchase", ex);
            }

            log.info("Course purchase completed successfully | transactionId: {}", transaction.getTransactionId());

            // Notify learner of successful course purchase
            try {
                NotificationRequest learnerNotif = NotificationRequest.builder()
                        .userId(request.getUserId())
                        .title("Course Purchase Successful 🎉")
                        .message("You have successfully purchased '" + request.getCourseName() + "'. Start learning now!")
                        .type(NotificationType.PAYMENT_SUCCESSFUL)
                        .channels(java.util.List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                        .referenceId(request.getCourseId())
                        .referenceType("COURSE")
                        .build();
                notificationPublisher.publish(learnerNotif);
            } catch (Exception ex) {
                log.error("Failed to send course purchase notification for userId {}", request.getUserId(), ex);
            }

            return new PaymentOrderResponse(transaction.getTransactionId(),
                    request.getAmount(), request.getAmount().multiply(new BigDecimal(100)).longValue(), "INR", "SUCCESS", "Null", "Null");

        } catch (PaymentException | WalletNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing course purchase: ", e);
            throw new PaymentException("Failed to process course purchase");
        }
    }
    public void verifyPaymentLink(String paymentLinkId,
                              Long expectedAmount,
                              String orderId) {

            try {

                RazorpayClient razorpay =
                        new RazorpayClient(razorpayKeyId, razorpayKeySecret);

                PaymentLink paymentLink =
                        razorpay.paymentLink.fetch(paymentLinkId);

                JSONObject json = paymentLink.toJson();

                String status = json.getString("status");
                long amountPaid = json.optLong("amount_paid", 0);

                if (!"paid".equalsIgnoreCase(status)) {
                    throw new PaymentException("Payment not completed");
                }

                if (amountPaid != expectedAmount) {
                    throw new PaymentException(
                            "Amount mismatch. Expected: "
                                    + expectedAmount
                                    + ", Paid: "
                                    + amountPaid);
                }

                List<WalletTransaction> existing =
                        transactionRepository.findByRazorpayOrderId(orderId);

                if (existing.isEmpty()) {
                    throw new PaymentException(
                            "Order not found — possible replay attack");
                }

                WalletTransaction transaction = existing.get(0);

                if ("SUCCESS".equals(transaction.getStatus())) {
                    throw new PaymentException("Payment already processed");
                }

                if ("FAILED".equals(transaction.getStatus())) {
                    throw new PaymentException(
                            "This payment has already been marked as failed");
                }

                java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(transaction.getUserId());
                if (!optWallet.isPresent()) {
                    throw new WalletNotFoundException("Wallet not found for user: " + transaction.getUserId());
                }
                Wallet wallet = optWallet.get();

                if ("SUSPENDED".equals(wallet.getStatus())
                        || "CLOSED".equals(wallet.getStatus())) {

                    throw new PaymentException(
                            "Wallet is "
                                    + wallet.getStatus().toLowerCase()
                                    + " and cannot receive funds");
                }

                transaction.setStatus("SUCCESS");

                // Store payment link id as reference
                transaction.setRazorpayPaymentId(paymentLinkId);

                transaction.setBalanceBefore(wallet.getBalance());

                transaction.setBalanceAfter(
                        wallet.getBalance().add(transaction.getAmount()));

                transaction.setUpdatedAt(now());

                wallet.setBalance(
                        wallet.getBalance().add(transaction.getAmount()));

                wallet.setUpdatedAt(now());

                transactionRepository.save(transaction);
                walletRepository.save(wallet);

                log.info(
                        "Wallet credited ₹{} for user {} — paymentLinkId: {}",
                        transaction.getAmount(),
                        transaction.getUserId(),
                        paymentLinkId);

            } catch (PaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to verify payment link", e);

                throw new PaymentException(
                        "Failed to verify payment link: " + e.getMessage());
            }
        }
        

    // ─── Learner: Buy Resource ────────────────────────────────────────────────

    public PaymentOrderResponse buyResource(BuyResourceRequest request) {
        log.info("Request to buy resource starting | userId: {}, resourceId: {}, trainerId: {}, amount: {}",
                request.getUserId(), request.getResourceId(), request.getTrainerId(), request.getAmount());
        if (request.getUserId() == null || request.getUserId().isBlank())
            throw new PaymentException("userId is required");
        if (request.getResourceId() == null || request.getResourceId().isBlank())
            throw new PaymentException("resourceId is required");
        if (request.getTrainerId() == null || request.getTrainerId().isBlank())
            throw new PaymentException("trainerId is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new PaymentException("Amount must be greater than ₹0");

        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(request.getUserId());
        if (!optWallet.isPresent()) {
            log.warn("Wallet not found for user: {}", request.getUserId());
            throw new WalletNotFoundException("Wallet not found for user: " + request.getUserId());
        }
        Wallet wallet = optWallet.get();
        log.info("Learner wallet found | userId: {}, balance: {}, status: {}", wallet.getUserId(), wallet.getBalance(), wallet.getStatus());

        if ("SUSPENDED".equals(wallet.getStatus()) || "CLOSED".equals(wallet.getStatus())) {
            log.warn("Wallet is blocked: userId: {}, status: {}", wallet.getUserId(), wallet.getStatus());
            throw new PaymentException("Your wallet is " + wallet.getStatus().toLowerCase() + ". Please contact support");
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient wallet balance for userId: {} | required: {}, available: {}", wallet.getUserId(), request.getAmount(), wallet.getBalance());
            throw new PaymentException("Insufficient wallet balance. Available: ₹"
                    + wallet.getBalance() + ", Required: ₹" + request.getAmount());
        }

        if (request.getUserId().equals(request.getTrainerId())) {
            log.warn("User {} tried to buy their own resource", request.getUserId());
            throw new PaymentException("You cannot purchase your own resource");
        }

        String targetId = request.getResourceId();
        if (isAlreadyPurchased(request.getUserId(), targetId)) {
            log.warn("User {} is already enrolled in resource {}", request.getUserId(), targetId);
            throw new PaymentException("You are already enrolled in this course");
        }

        try {
            WalletTransaction transaction = new WalletTransaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setUserId(request.getUserId());
            transaction.setTransactionType("BUY_RESOURCE");
            transaction.setAmount(request.getAmount());
            transaction.setDescription("Purchase resource: " + request.getResourceName());
            transaction.setReferenceId(request.getResourceId());
            transaction.setStatus("SUCCESS");
            transaction.setBalanceBefore(wallet.getBalance());
            transaction.setBalanceAfter(wallet.getBalance().subtract(request.getAmount()));
            transaction.setCreatedAt(now());

            wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
            wallet.setTotalSpent(wallet.getTotalSpent().add(request.getAmount()));
            wallet.setUpdatedAt(now());

            updateTrainerEarnings(request.getTrainerId(), request.getAmount());
            walletRepository.save(wallet);
            transactionRepository.save(transaction);
            log.info("Updated trainer earnings and saved learner wallet transaction | transactionId: {}", transaction.getTransactionId());

            // Create corresponding payments for access verification
            String targetType = request.getEffectiveTargetType();
            com.example.payment_service.model.Payment payment = com.example.payment_service.model.Payment.builder()
                    .orderId("wlt_ord_res_" + transaction.getTransactionId())
                    .paymentId("wlt_pay_" + transaction.getTransactionId())
                    .userId(request.getUserId())
                    .targetId(request.getResourceId())
                    .targetType(targetType)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            log.info("Persisted access verification payment for targetType={} to database", targetType);

            // Trigger enrollment activation in Enrollment Service
            try {
                com.example.payment_service.dto.request.PaymentSuccessRequest enrollReq = com.example.payment_service.dto.request.PaymentSuccessRequest.builder()
                        .paymentId(payment.getPaymentId())
                        .orderId(payment.getOrderId())
                        .userId(request.getUserId())
                        .targetId(request.getResourceId())
                        .targetType(targetType)
                        .status("SUCCESS")
                        .build();
                log.info("Triggering enrollment activation for wallet resource purchase targetType={}: userId={}, targetId={}", targetType, request.getUserId(), request.getResourceId());
                enrollmentClient.markPaymentSuccess(enrollReq);
            } catch (Exception ex) {
                log.error("Failed to trigger enrollment activation for wallet resource purchase", ex);
            }

            log.info("Resource purchase completed successfully | transactionId: {}", transaction.getTransactionId());

            // Notify learner of successful resource purchase
            try {
                NotificationRequest learnerNotif = NotificationRequest.builder()
                        .userId(request.getUserId())
                        .title("Resource Purchase Successful 🎉")
                        .message("You have successfully purchased '" + request.getResourceName() + "'. Access it anytime!")
                        .type(NotificationType.PAYMENT_SUCCESSFUL)
                        .channels(java.util.List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                        .referenceId(request.getResourceId())
                        .referenceType("RESOURCE")
                        .build();
                notificationPublisher.publish(learnerNotif);
            } catch (Exception ex) {
                log.error("Failed to send resource purchase notification for userId {}", request.getUserId(), ex);
            }

            return new PaymentOrderResponse(transaction.getTransactionId(),
                    request.getAmount(), request.getAmount().multiply(new BigDecimal(100)).longValue(), "INR", "SUCCESS", "Null", "Null");

        } catch (PaymentException | WalletNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing resource purchase: ", e);
            throw new PaymentException("Failed to process resource purchase");
        }
    }

    // ─── Learner: Wallet Balance & Transactions ───────────────────────────────

    public WalletResponse getWalletBalance(String userId) {
        if (userId == null || userId.isBlank())
            throw new PaymentException("userId is required");
        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(userId);
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for user: " + userId);
        }
        Wallet wallet = optWallet.get();
        return mapToWalletResponse(wallet);
    }

    public TransactionHistoryResponse getTransactionHistory(String userId) {
        if (userId == null || userId.isBlank())
            throw new PaymentException("userId is required");

        // Verify user wallet exists
        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(userId);
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for user: " + userId);
        }

        List<WalletTransaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return new TransactionHistoryResponse(transactions, transactions.size());
    }

    // ─── Trainer ───────────────────────────────────────────────────────────

    public WalletResponse getTrainerWallet(String trainerId) {
        if (trainerId == null || trainerId.isBlank())
            throw new PaymentException("trainerId is required");
        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(trainerId);
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for trainer: " + trainerId);
        }
        Wallet wallet = optWallet.get();
        return mapToWalletResponse(wallet);
    }

    public TransactionHistoryResponse getTrainerTransactions(String trainerId) {
        return getTransactionHistory(trainerId);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * CHANGE COMMENT: NEW METHOD ADDED - TRAINER REVENUE ANALYTICS
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Purpose: Calculate and return comprehensive revenue analytics for a trainer
     * 
     * This method performs the following operations:
     * 1. Validates trainer ID
     * 2. Retrieves trainer wallet information
     * 3. Fetches all earning transactions (EARNING, BUY_COURSE, BUY_RESOURCE)
     * 4. Analyzes payment records for course/resource sales breakdown
     * 5. Aggregates data by course, resource, and month
     * 6. Calculates metrics (totals, counts, trends)
     * 7. Returns comprehensive response with breakdowns
     * 
     * @param trainerId - The ID of the trainer requesting analytics
     * @return TrainerRevenueResponse - Complete revenue analytics with breakdowns
     * 
     * @throws PaymentException - If trainerId is null or empty
     * @throws WalletNotFoundException - If trainer wallet doesn't exist
     * 
     * Response includes:
     *   - Total courses sold
     *   - Total resources sold
     *   - Revenue breakdown by course
     *   - Revenue breakdown by resource
     *   - Monthly revenue trends
     *   - Current balance, pending payout, withdrawn amount
     * 
     * Used by: GET /api/v1/trainer-wallet/revenue-analytics
     */
    public TrainerRevenueResponse getTrainerRevenueAnalytics(String trainerId) {
        // Step 1: Validate trainerId
        if (trainerId == null || trainerId.isBlank())
            throw new PaymentException("trainerId is required");

        // Step 2: Get trainer wallet (throws WalletNotFoundException if not found)
        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(trainerId);
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for trainer: " + trainerId);
        }
        Wallet wallet = optWallet.get();

        // Step 3: Get all earning transactions for this trainer
        // CHANGE: Uses new findByUserIdAndTransactionTypes() method to filter specific types
        List<String> earningTypes = java.util.List.of("BUY_COURSE", "BUY_RESOURCE", "EARNING");
        List<WalletTransaction> trainerTransactions = transactionRepository
                .findByUserIdAndTransactionTypes(trainerId, earningTypes);

        // Step 4: Initialize aggregation variables
        long coursesSold = 0;
        long resourcesSold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        java.util.Map<String, java.util.Map<String, Object>> courseMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, Object>> resourceMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, Object>> monthlyMap = new java.util.LinkedHashMap<>();

        // Step 5: Aggregate earning transactions
        // CHANGE: Calculate total revenue and monthly breakdown from EARNING transactions
        for (WalletTransaction t : trainerTransactions) {
            if ("SUCCESS".equals(t.getStatus())) {
                if ("EARNING".equals(t.getTransactionType())) {
                    // Add to total revenue
                    totalRevenue = totalRevenue.add(t.getAmount());

                    // Group by month for monthly breakdown (format: YYYY-MM)
                    String month = t.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                    monthlyMap.putIfAbsent(month, new java.util.HashMap<>());
                    java.util.Map<String, Object> monthData = monthlyMap.get(month);
                    BigDecimal existing = (BigDecimal) monthData.getOrDefault("amount", BigDecimal.ZERO);
                    monthData.put("amount", existing.add(t.getAmount()));
                    monthData.put("transactionCount", (Long) monthData.getOrDefault("transactionCount", 0L) + 1);
                }
            }
        }

        // Step 6: Analyze course and resource sales via Payment records
        // CHANGE: Query payments to get detailed breakdown of what courses/resources were sold
        List<com.example.payment_service.model.Payment> payments = paymentRepository.findByUserId(trainerId);

        for (com.example.payment_service.model.Payment p : payments) {
            if ("SUCCESS".equals(p.getStatus())) {
                // Count and aggregate course sales (RECORDED_COURSE and LIVE_COURSE types)
                if ("RECORDED_COURSE".equals(p.getTargetType()) || "LIVE_COURSE".equals(p.getTargetType())) {
                    coursesSold++;
                    String courseId = p.getTargetId();
                    courseMap.putIfAbsent(courseId, new java.util.HashMap<>());
                    java.util.Map<String, Object> courseData = courseMap.get(courseId);
                    Long units = (Long) courseData.getOrDefault("units", 0L);
                    BigDecimal revenue = (BigDecimal) courseData.getOrDefault("revenue", BigDecimal.ZERO);
                    courseData.put("courseId", courseId);
                    courseData.put("units", units + 1);
                    courseData.put("revenue", revenue.add(p.getAmount()));
                }
                // Count and aggregate resource sales (RESOURCE and RECORDING types)
                else if ("RESOURCE".equals(p.getTargetType()) || "RECORDING".equals(p.getTargetType())) {
                    resourcesSold++;
                    String resourceId = p.getTargetId();
                    resourceMap.putIfAbsent(resourceId, new java.util.HashMap<>());
                    java.util.Map<String, Object> resourceData = resourceMap.get(resourceId);
                    Long units = (Long) resourceData.getOrDefault("units", 0L);
                    BigDecimal revenue = (BigDecimal) resourceData.getOrDefault("revenue", BigDecimal.ZERO);
                    resourceData.put("resourceId", resourceId);
                    resourceData.put("units", units + 1);
                    resourceData.put("revenue", revenue.add(p.getAmount()));
                }
            }
        }

        // Step 7: Build course revenue breakdown
        // CHANGE: Transform aggregated course data into response objects
        List<TrainerRevenueResponse.RevenueByCourse> courseRevenue = new java.util.ArrayList<>();
        for (String courseId : courseMap.keySet()) {
            java.util.Map<String, Object> courseData = courseMap.get(courseId);
            TrainerRevenueResponse.RevenueByCourse course = TrainerRevenueResponse.RevenueByCourse.builder()
                    .courseId(courseId)
                    .courseName("Course " + courseId) // TODO: Can be enhanced with actual course names from course service
                    .unitsSold((Long) courseData.get("units"))
                    .revenue((BigDecimal) courseData.get("revenue"))
                    .build();
            courseRevenue.add(course);
        }

        // Step 8: Build resource revenue breakdown
        // CHANGE: Transform aggregated resource data into response objects
        List<TrainerRevenueResponse.RevenueByResource> resourceRevenue = new java.util.ArrayList<>();
        for (String resourceId : resourceMap.keySet()) {
            java.util.Map<String, Object> resourceData = resourceMap.get(resourceId);
            TrainerRevenueResponse.RevenueByResource resource = TrainerRevenueResponse.RevenueByResource.builder()
                    .resourceId(resourceId)
                    .resourceName("Resource " + resourceId) // TODO: Can be enhanced with actual resource names from resource service
                    .unitsSold((Long) resourceData.get("units"))
                    .revenue((BigDecimal) resourceData.get("revenue"))
                    .build();
            resourceRevenue.add(resource);
        }

        // Step 9: Build monthly breakdown (sorted descending by month)
        // CHANGE: Transform aggregated monthly data into response objects, sorted by month
        List<TrainerRevenueResponse.MonthlyRevenue> monthlyBreakdown = new java.util.ArrayList<>();
        monthlyMap.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey())) // Sort descending (newest months first)
                .forEach(entry -> {
                    java.util.Map<String, Object> data = entry.getValue();
                    TrainerRevenueResponse.MonthlyRevenue monthly = TrainerRevenueResponse.MonthlyRevenue.builder()
                            .month(entry.getKey())
                            .amount((BigDecimal) data.get("amount"))
                            .transactionCount((Long) data.get("transactionCount"))
                            .build();
                    monthlyBreakdown.add(monthly);
                });

        // Step 10: Build and return comprehensive response
        // CHANGE: Compile all aggregated data into single response object
        return TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(coursesSold)
                .totalResourcesSold(resourcesSold)
                .totalTransactions((long) trainerTransactions.size())
                .totalRevenue(totalRevenue)
                .balance(wallet.getBalance())
                .pendingPayout(wallet.getPendingPayout())
                // CHANGE: Calculate total withdrawn (totalEarned - balance - pendingPayout)
                .totalWithdrawn(wallet.getTotalEarned().subtract(wallet.getBalance()).subtract(wallet.getPendingPayout()))
                .lastUpdated(wallet.getUpdatedAt())
                .courseRevenue(courseRevenue)
                .resourceRevenue(resourceRevenue)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    public String requestPayout(PayoutRequestDto request, String token) {

        String trainerId = jwtUtil.extractUserId(token);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new PaymentException("Minimum payout amount is ₹100");
        }

        if (request.getBankAccount() == null || request.getBankAccount().isBlank()) {
            log.info("Payout amount: {}", request.getAmount());
            throw new PaymentException("Bank account number is required");
        }

        if (request.getIfscCode() == null || request.getIfscCode().isBlank()) {
            throw new PaymentException("IFSC code is required");
        }

        if (request.getAccountHolderName() == null || request.getAccountHolderName().isBlank()) {
            throw new PaymentException("Account holder name is required");
        }

        java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(trainerId);
        if (!optWallet.isPresent()) {
            throw new WalletNotFoundException("Wallet not found for trainer: " + trainerId);
        }
        Wallet wallet = optWallet.get();

        if ("SUSPENDED".equalsIgnoreCase(wallet.getStatus()) || "CLOSED".equalsIgnoreCase(wallet.getStatus())) {
            throw new PaymentException("Wallet is " + wallet.getStatus().toLowerCase() + ". Payouts are not allowed");
        }

        if (wallet.getBalance() == null) {
            throw new PaymentException("Wallet balance is not available");
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new PaymentException("Insufficient balance for payout. Available: ₹"
                    + wallet.getBalance() + ", Requested: ₹" + request.getAmount());
        }

        List<PayoutRequest> pendingPayouts =
                payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");

        boolean hasPending = false;
        for (PayoutRequest p : pendingPayouts) {
            if (trainerId.equals(p.getTrainerId())) {
                hasPending = true;
                break;
            }
        }

        if (hasPending) {
            throw new PaymentException("You already have a pending payout request. Please wait for it to be processed");
        }

        try {
            PayoutRequest payoutRequest = new PayoutRequest();
            payoutRequest.setPayoutId(UUID.randomUUID().toString());
            payoutRequest.setTrainerId(trainerId);
            payoutRequest.setAmount(request.getAmount());
            payoutRequest.setBankAccount(request.getBankAccount());
            payoutRequest.setIfscCode(request.getIfscCode());
            payoutRequest.setAccountHolderName(request.getAccountHolderName());
            payoutRequest.setStatus("PENDING");
            payoutRequest.setRequestedAt(now());

            BigDecimal currentPendingPayout = wallet.getPendingPayout() != null
                    ? wallet.getPendingPayout()
                    : BigDecimal.ZERO;

            wallet.setPendingPayout(currentPendingPayout.add(request.getAmount()));
            wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
            wallet.setUpdatedAt(now());

            payoutRequestRepository.save(payoutRequest);
            walletRepository.save(wallet);

            try {
                // To Trainer
                NotificationRequest trainerNotif = NotificationRequest.builder()
                        .userId(trainerId)
                        .title("Payout Requested")
                        .message("Your payout request of ₹" + request.getAmount() + " has been submitted.")
                        .type(NotificationType.PAYOUT_REQUESTED)
                        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                        .referenceId(payoutRequest.getPayoutId())
                        .referenceType("PAYOUT")
                        .build();
                notificationPublisher.publish(trainerNotif);

                // Broadcast to ADMIN
                BroadcastNotificationRequest adminNotif = BroadcastNotificationRequest.builder()
                        .title("New Payout Request")
                        .message("New payout request received from trainer.")
                        .type(NotificationType.PAYOUT_REQUESTED)
                        .channels(List.of(NotificationChannel.IN_APP))
                        .targetRole("ADMIN")
                        .sendMode("ROLE_BASED")
                        .referenceId(payoutRequest.getPayoutId())
                        .referenceType("PAYOUT")
                        .build();
                notificationPublisher.publishBroadcast(adminNotif);
            } catch (Exception ex) {
                log.error("Failed to send PAYOUT_REQUESTED notification for trainerId {}", trainerId, ex);
            }

            return payoutRequest.getPayoutId();

        } catch (PaymentException | WalletNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating payout request: ", e);
            throw new PaymentException("Failed to create payout request");
        }
    }
    public TransactionHistoryResponse getAllTransactionHistory() {

    // Force-load all records from DynamoDB PaginatedScanList
    List<WalletTransaction> transactions = new ArrayList<>();

    for (WalletTransaction transaction : transactionRepository.findAll()) {
        transactions.add(transaction);
        }

        // Sort newest first
        transactions.sort(new Comparator<WalletTransaction>() {
            @Override
            public int compare(WalletTransaction o1, WalletTransaction o2) {
                if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                if (o1.getCreatedAt() == null) return 1;
                if (o2.getCreatedAt() == null) return -1;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
        });

        int totalRecords = transactions.size();

        return new TransactionHistoryResponse(
                transactions,
                totalRecords
        );
    }

public AdminDashboardResponse getAdminDashboard() {

    List<WalletTransaction> allTransactions = transactionRepository.findAll();
    List<Wallet> allWallets = walletRepository.findAll();

    BigDecimal totalRevenue = BigDecimal.ZERO;
    BigDecimal totalPayouts = BigDecimal.ZERO;
    BigDecimal totalRefunds = BigDecimal.ZERO;
    for (WalletTransaction t : allTransactions) {
        if ("SUCCESS".equals(t.getStatus())) {
            if ("BUY_COURSE".equals(t.getTransactionType()) || "BUY_RESOURCE".equals(t.getTransactionType())) {
                totalRevenue = totalRevenue.add(t.getAmount());
            } else if ("PAYOUT".equals(t.getTransactionType())) {
                totalPayouts = totalPayouts.add(t.getAmount());
            } else if ("REFUND".equals(t.getTransactionType())) {
                totalRefunds = totalRefunds.add(t.getAmount());
            }
        }
    }

    BigDecimal pendingPayouts = BigDecimal.ZERO;
    int activeLearners = 0;
    int activeTrainers = 0;
    for (Wallet w : allWallets) {
        if ("TRAINER".equals(w.getUserType())) {
            if (w.getPendingPayout() != null) {
                pendingPayouts = pendingPayouts.add(w.getPendingPayout());
            }
            if ("ACTIVE".equals(w.getStatus())) {
                activeTrainers++;
            }
        } else if ("LEARNER".equals(w.getUserType())) {
            if ("ACTIVE".equals(w.getStatus())) {
                activeLearners++;
            }
        }
    }

    return new AdminDashboardResponse(totalRevenue, totalPayouts, pendingPayouts,
            totalRefunds, allTransactions.size(), activeLearners, activeTrainers);
}

public List<PayoutRequest> getAllPayouts() {
    return payoutRequestRepository.findAll();
}

public void processPayoutRequest(String payoutId, String adminId, String action, String remarks) {
    if (payoutId == null || payoutId.isBlank())
        throw new PaymentException("payoutId is required");
    if (adminId == null || adminId.isBlank())
        throw new PaymentException("adminId is required");
    if (!"APPROVE".equals(action) && !"REJECT".equals(action) && !"HOLD".equals(action))
        throw new PaymentException("Action must be either APPROVE or REJECT or HOLD");

    java.util.Optional<PayoutRequest> optPayout = payoutRequestRepository.findById(payoutId);
    if (!optPayout.isPresent()) {
        throw new PaymentException("Payout request not found: " + payoutId);
    }
    PayoutRequest payoutRequest = optPayout.get();

    if ("PROCESSED".equals(payoutRequest.getStatus()))
        throw new PaymentException("Payout request is already " + payoutRequest.getStatus().toLowerCase());

    java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(payoutRequest.getTrainerId());
    if (!optWallet.isPresent()) {
        throw new WalletNotFoundException("Trainer wallet not found");
    }
    Wallet trainerWallet = optWallet.get();

    if ("APPROVE".equals(action)) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setUserId(payoutRequest.getTrainerId());
        transaction.setTransactionType("PAYOUT");
        transaction.setAmount(payoutRequest.getAmount());
        transaction.setDescription("Payout processed by admin");
        transaction.setReferenceId(payoutId);
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(now());
        transactionRepository.save(transaction);

        payoutRequest.setStatus("PROCESSED");
        trainerWallet.setPendingPayout(
                trainerWallet.getPendingPayout().subtract(payoutRequest.getAmount()));

    }else if ("HOLD".equals(action)) {
        payoutRequest.setStatus("HOLD");
        // Notify trainer that payout is on hold
        try {
            NotificationRequest holdNotif = NotificationRequest.builder()
                    .userId(payoutRequest.getTrainerId())
                    .title("Payout On Hold")
                    .message("Your payout request of ₹" + payoutRequest.getAmount() + " has been put on hold by admin. " + (remarks != null ? "Reason: " + remarks : "Please contact support."))
                    .type(NotificationType.PAYOUT_HOLD)
                    .channels(java.util.List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceId(payoutId)
                    .referenceType("PAYOUT")
                    .build();
            notificationPublisher.publish(holdNotif);
        } catch (Exception ex) {
            log.error("Failed to send PAYOUT_HOLD notification for payoutId {}", payoutId, ex);
        }
    } else {
        trainerWallet.setBalance(trainerWallet.getBalance().add(payoutRequest.getAmount()));
        trainerWallet.setPendingPayout(
                trainerWallet.getPendingPayout().subtract(payoutRequest.getAmount()));
        payoutRequest.setStatus("REJECTED");
    }

    payoutRequest.setProcessedAt(now());
    payoutRequest.setProcessedBy(adminId);
    payoutRequest.setRemarks(remarks);
    trainerWallet.setUpdatedAt(now());
    
    if ("APPROVE".equals(action)) {
        try {
            NotificationRequest trainerNotif = NotificationRequest.builder()
                    .userId(payoutRequest.getTrainerId())
                    .title("Payout Processed")
                    .message("Your payout of ₹" + payoutRequest.getAmount() + " has been successfully processed.")
                    .type(NotificationType.PAYOUT_PROCESSED)
                    .channels(List.of(NotificationChannel.EMAIL))
                    .referenceId(payoutId)
                    .referenceType("PAYOUT")
                    .build();
            notificationPublisher.publish(trainerNotif);
        } catch (Exception ex) {
            log.error("Failed to send PAYOUT_PROCESSED notification for payoutId {}", payoutId, ex);
        }
    } else if ("REJECT".equals(action)) {
        try {
            NotificationRequest trainerNotif = NotificationRequest.builder()
                    .userId(payoutRequest.getTrainerId())
                    .title("Payout Rejected")
                    .message("Your payout request of ₹" + payoutRequest.getAmount() + " has been rejected. " + (remarks != null ? "Reason: " + remarks : ""))
                    .type(NotificationType.PAYOUT_REJECTED)
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceId(payoutId)
                    .referenceType("PAYOUT")
                    .build();
            notificationPublisher.publish(trainerNotif);
        } catch (Exception ex) {
            log.error("Failed to send PAYOUT_REJECTED notification for payoutId {}", payoutId, ex);
        }
    }

    payoutRequestRepository.save(payoutRequest);
    walletRepository.save(trainerWallet);


}



public void verifyAddFundsPayment(PaymentVerifyRequest request) {
    if (request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank())
        throw new PaymentException("razorpay_order_id is required");
    if (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank())
        throw new PaymentException("razorpay_payment_id is required");
    if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank())
        throw new PaymentException("razorpay_signature is required");

    verifyRazorpaySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

    List<WalletTransaction> transactions = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
    if (transactions.isEmpty())
        throw new PaymentException("Transaction not found for orderId: " + request.getRazorpayOrderId());

    WalletTransaction transaction = transactions.get(0);

    if ("SUCCESS".equals(transaction.getStatus()))
        throw new PaymentException("Payment already processed");

    java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(transaction.getUserId());
    if (!optWallet.isPresent()) {
        throw new WalletNotFoundException("Wallet not found for user: " + transaction.getUserId());
    }
    Wallet wallet = optWallet.get();

    transaction.setStatus("SUCCESS");
    transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
    transaction.setBalanceBefore(wallet.getBalance());
    transaction.setBalanceAfter(wallet.getBalance().add(transaction.getAmount()));
    transaction.setUpdatedAt(now());

    wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
    wallet.setUpdatedAt(now());

    transactionRepository.save(transaction);
    walletRepository.save(wallet);
}

// ─── Test Helpers ─────────────────────────────────────────────────────────

public void confirmAddFundsTest(AddFundsConfirmRequest request) {
    List<WalletTransaction> existing = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
    if (existing.isEmpty())
        throw new PaymentException("Order not found");

    WalletTransaction transaction = existing.get(0);
    if ("SUCCESS".equals(transaction.getStatus()))
        throw new PaymentException("Payment already processed");

    java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(transaction.getUserId());
    if (!optWallet.isPresent()) {
        throw new WalletNotFoundException("Wallet not found");
    }
    Wallet wallet = optWallet.get();

    transaction.setStatus("SUCCESS");
    transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
    transaction.setBalanceBefore(wallet.getBalance());
    transaction.setBalanceAfter(wallet.getBalance().add(transaction.getAmount()));
    transaction.setUpdatedAt(now());

    wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
    wallet.setUpdatedAt(now());

    transactionRepository.save(transaction);
    walletRepository.save(wallet);

    log.info("Wallet credited ₹{} for user {} (TEST MODE)", transaction.getAmount(), transaction.getUserId());
}

public String generateTestSignature(String orderId, String paymentId) {
    try {
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
        throw new PaymentException("Error generating signature: " + e.getMessage());
    }
}

// ─── Private Helpers ──────────────────────────────────────────────────────

private Wallet getOrCreateWallet(String userId, String userType) {
    java.util.Optional<Wallet> optWallet = walletRepository.findByUserId(userId);
    if (optWallet.isPresent()) {
        return optWallet.get();
    } else {
        return createNewWallet(userId, userType);
    }
}

private Wallet createNewWallet(String userId, String userType) {
    log.info("Creating wallet for a {} with userId: {}", userType, userId);
    Wallet wallet = new Wallet();
    wallet.setUserId(userId);
    wallet.setUserType(userType);
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setTotalEarned(BigDecimal.ZERO);
    wallet.setTotalSpent(BigDecimal.ZERO);
    wallet.setPendingPayout(BigDecimal.ZERO);
    wallet.setStatus("ACTIVE");
    wallet.setCreatedAt(now());
    wallet.setUpdatedAt(now());
    return walletRepository.save(wallet);
}

private void updateTrainerEarnings(String trainerId, BigDecimal amount) {
    // Auto-create trainer wallet if not exists (system-side operation)
    Wallet trainerWallet = getOrCreateWallet(trainerId, "TRAINER");
    BigDecimal trainerShare = amount.multiply(BigDecimal.valueOf(0.8));

    trainerWallet.setBalance(trainerWallet.getBalance().add(trainerShare));
    trainerWallet.setTotalEarned(trainerWallet.getTotalEarned().add(trainerShare));
    trainerWallet.setUpdatedAt(now());
    walletRepository.save(trainerWallet);

    WalletTransaction transaction = new WalletTransaction();
    transaction.setTransactionId(UUID.randomUUID().toString());
    transaction.setUserId(trainerId);
    transaction.setTransactionType("EARNING");
    transaction.setAmount(trainerShare);
    transaction.setDescription("Earning from course/resource sale");
    transaction.setStatus("SUCCESS");
    transaction.setCreatedAt(now());
    transactionRepository.save(transaction);

    try {
        NotificationRequest notif = NotificationRequest.builder()
                .userId(trainerId)
                .title("Wallet Credited")
                .message("Your wallet has been credited with ₹" + trainerShare + " for course purchase.")
                .type(NotificationType.WALLET_CREDITED)
                .channels(List.of(NotificationChannel.IN_APP))
                .referenceId(trainerWallet.getUserId())
                .referenceType("WALLET")
                .build();
        notificationPublisher.publish(notif);
    } catch (Exception ex) {
        log.error("Failed to send WALLET_CREDITED notification for trainerId {}", trainerId, ex);
    }
}

private WalletResponse mapToWalletResponse(Wallet wallet) {
    return new WalletResponse(wallet.getUserId(), wallet.getUserType(), wallet.getBalance(),
            wallet.getTotalEarned(), wallet.getTotalSpent(), wallet.getPendingPayout(), wallet.getStatus());
}

private LocalDateTime now() {
    return LocalDateTime.now();
}

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            org.json.JSONObject options = new org.json.JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);
            String computed = signature; // dummy to satisfy following code

        if (!computed.equals(signature)) {
            log.warn("Signature mismatch for orderId: {}", orderId);
            throw new PaymentException("Invalid payment signature — verification failed");
        }
    } catch (PaymentException e) {
        throw e;
    } catch (Exception e) {
        log.error("Signature verification error", e);
        throw new PaymentException("Signature verification error");
    }
}

    private boolean isAlreadyPurchased(String userId, String targetId) {
        if (userId == null || targetId == null || userId.isBlank() || targetId.isBlank()) {
            return false;
        }
        try {
            List<Payment> payments = paymentRepository.findByUserId(userId);
            if (payments != null) {
                for (Payment p : payments) {
                    if (p != null && targetId.equalsIgnoreCase(p.getTargetId()) && "SUCCESS".equalsIgnoreCase(p.getStatus())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check existing payments for user {}: {}", userId, e.getMessage());
        }
        return false;
    }
}
