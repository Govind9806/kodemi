package com.example.payment_service.service;
import java.util.List;
import com.example.payment_service.dto.request.PaymentOrderRequest;
import com.example.payment_service.dto.response.PaymentOrderResponse;
import com.example.payment_service.dto.request.PaymentVerifyRequest;
import com.example.payment_service.exception.PaymentException;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.notification.NotificationPublisher;
import com.example.payment_service.dto.notification.NotificationRequest;
import com.example.payment_service.dto.notification.NotificationType;
import com.example.payment_service.dto.notification.NotificationChannel;
import com.example.payment_service.dto.response.PaymentVerificationResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.example.payment_service.feign.EnrollmentClient;
import com.example.payment_service.dto.request.PaymentSuccessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final NotificationPublisher notificationPublisher;
    private final EnrollmentClient enrollmentClient;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    
    public PaymentOrderResponse createOrder(PaymentOrderRequest request) {
        try {
            log.info("Request to create payment order received | userId: {}, amount: {}, targetId: {}, targetType: {}",
                    request.getUserid(), request.getAmount(), request.getTargetId(), request.getTargetType());
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid amount for payment order creation: {}", request.getAmount());
                throw new PaymentException("Amount must be greater than zero");
            }
            if (request.getTargetId() == null || request.getTargetType() == null) {
                log.warn("Missing target details: targetId: {}, targetType: {}", request.getTargetId(), request.getTargetType());
                throw new PaymentException("Target ID and Target Type are required");
            }
            if (verifyAccess(request.getUserid(), request.getTargetId(), request.getTargetType()).isPaid()) {
                log.warn("User {} is already enrolled in target {}", request.getUserid(), request.getTargetId());
                throw new PaymentException("You are already enrolled in this course");
            }
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            
            log.info("Initiating order creation with Razorpay: amountInPaise: {}, receipt: {}", orderRequest.getInt("amount"), orderRequest.getString("receipt"));
            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created successfully: razorpayOrderId: {}", (String) order.get("id"));
            
            Payment payment = new Payment();
            payment.setOrderId(order.get("id"));
            payment.setUserId(request.getUserid());
            payment.setTargetId(request.getTargetId());
            payment.setTargetType(request.getTargetType());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
            payment.setStatus("CREATED");
            payment.setCreatedAt(LocalDateTime.now());
            
            paymentRepository.save(payment);
            log.info("Initial payment record saved to DynamoDB: orderId: {}", payment.getOrderId());
            
            PaymentOrderResponse response = new PaymentOrderResponse();
            response.setOrderId(payment.getOrderId());
            response.setAmount(request.getAmount());
            response.setCurrency(payment.getCurrency());
            response.setStatus("CREATED");
            log.info("Payment order created: {}", payment.getOrderId());
            return response;
        } catch (Exception e) {
            log.error("Error creating payment order", e);
            throw new PaymentException("Failed to create payment order");
        }
    }
    public String verifyPayment(PaymentVerifyRequest request) {
        try {
            log.info("Received request to verify payment | orderId: {}, paymentId: {}", request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            if (request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank())
                throw new PaymentException("razorpay_order_id is required");
            if (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank())
                throw new PaymentException("razorpay_payment_id is required");
            if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank())
                throw new PaymentException("razorpay_signature is required");

            java.util.Optional<Payment> opt = paymentRepository.findById(request.getRazorpayOrderId());
            if (!opt.isPresent()) {
                log.error("Payment record not found in database for orderId: {}", request.getRazorpayOrderId());
                throw new PaymentException("Payment not found");
            }
            Payment payment = opt.get();
            log.info("Found payment record | orderId: {}, currentStatus: {}", payment.getOrderId(), payment.getStatus());

            try {
                log.info("Verifying signature with Razorpay for orderId: {}", request.getRazorpayOrderId());
                verifyRazorpaySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
                log.info("Signature verified successfully for orderId: {}", request.getRazorpayOrderId());
            } catch (PaymentException e) {
                payment.setStatus("FAILED");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                try {
                    NotificationRequest notif = NotificationRequest.builder()
                            .userId(payment.getUserId())
                            .title("Payment Failed")
                            .message("Your payment failed. Please try again.")
                            .type(NotificationType.PAYMENT_FAILED)
                            .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                            .referenceId(payment.getOrderId())
                            .referenceType("PAYMENT")
                            .build();
                    notificationPublisher.publish(notif);
                } catch (Exception ex) {
                    log.error("Failed to send PAYMENT_FAILED notification", ex);
                }
                throw e;
            }
            log.info("Found payment record | orderId: {}, currentStatus: {}", payment.getOrderId(), payment.getStatus());

            if ("SUCCESS".equals(payment.getStatus())) {
                log.warn("Payment already processed and marked SUCCESS for orderId: {}", request.getRazorpayOrderId());
                throw new PaymentException("Payment already processed");
            }

            payment.setStatus("SUCCESS");
            payment.setPaymentId(request.getRazorpayPaymentId());
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("Payment marked as SUCCESS in database for orderId: {}, paymentId: {}", payment.getOrderId(), payment.getPaymentId());

            try {
                if (payment.getTargetId() != null && payment.getTargetType() != null) {
                    PaymentSuccessRequest enrollReq = PaymentSuccessRequest.builder()
                            .paymentId(payment.getPaymentId())
                            .orderId(payment.getOrderId())
                            .userId(payment.getUserId())
                            .targetId(payment.getTargetId())
                            .targetType(payment.getTargetType())
                            .status("SUCCESS")
                            .build();
                    log.info("Triggering enrollment activation for userId: {}, targetId: {}, targetType: {}", payment.getUserId(), payment.getTargetId(), payment.getTargetType());
                    enrollmentClient.markPaymentSuccess(enrollReq);
                }
            } catch (Exception ex) {
                log.error("Failed to trigger enrollment activation for orderId {}", payment.getOrderId(), ex);
            }
            
            try {
                NotificationRequest notif = NotificationRequest.builder()
                        .userId(payment.getUserId())
                        .title("Payment Successful")
                        .message("Your payment was successful. Your course access is now being activated.")
                        .type(NotificationType.PAYMENT_SUCCESSFUL)
                        .channels(List.of(NotificationChannel.EMAIL))
                        .referenceId(payment.getPaymentId())
                        .referenceType("PAYMENT")
                        .build();
                log.info("Sending notification for successful payment to userId: {}", payment.getUserId());
                notificationPublisher.publish(notif);
            } catch (Exception ex) {
                log.error("Failed to send PAYMENT_SUCCESSFUL notification for paymentId {}", payment.getPaymentId(), ex);
            }

            return "Payment Verified Successfully";

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            throw new PaymentException("Payment verification failed");
        }
    }

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            org.json.JSONObject options = new org.json.JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (com.razorpay.RazorpayException e) {
            log.error("Signature verification error", e);
            throw new PaymentException("Invalid payment signature — verification failed");
        } catch (Exception e) {
            log.error("Signature verification error", e);
            throw new PaymentException("Signature verification error");
        }
    }
    public Payment getPaymentStatus(String orderId) {
        log.info("Retrieving payment status for orderId: {}", orderId);
        java.util.Optional<Payment> opt = paymentRepository.findById(orderId);
        if (!opt.isPresent()) {
            log.warn("Payment record not found for orderId: {}", orderId);
            throw new PaymentException("Payment not found");
        }
        Payment payment = opt.get();
        log.info("Retrieved payment record | orderId: {}, status: {}", payment.getOrderId(), payment.getStatus());
        return payment;
    }

    public PaymentVerificationResponse verifyAccess(String userId, String targetId, String targetType) {
        log.info("Verifying access status | userId: {}, targetId: {}, targetType: {}", userId, targetId, targetType);
        List<Payment> payments = paymentRepository.findByUserId(userId);
        log.info("Found {} payment records for userId: {}", payments != null ? payments.size() : 0, userId);
        if (payments != null) {
            for (Payment p : payments) {
                if (p != null && "SUCCESS".equalsIgnoreCase(p.getStatus()) && targetId != null && targetId.equalsIgnoreCase(p.getTargetId())) {
                    boolean typeMatches = targetType == null || targetType.isBlank() || 
                            targetType.equalsIgnoreCase(p.getTargetType()) ||
                            ("CONFERENCE".equalsIgnoreCase(targetType) && ("RESOURCE".equalsIgnoreCase(p.getTargetType()) || "BUY_RESOURCE".equalsIgnoreCase(p.getTargetType()))) ||
                            ("RESOURCE".equalsIgnoreCase(targetType) && ("CONFERENCE".equalsIgnoreCase(p.getTargetType()) || "BUY_RESOURCE".equalsIgnoreCase(p.getTargetType())));
                    if (typeMatches) {
                        log.info("Access GRANTED | userId: {}, targetId: {}, targetType: {} | paymentId: {}", userId, targetId, targetType, p.getPaymentId());
                        return PaymentVerificationResponse.builder()
                                .isPaid(true)
                                .paymentId(p.getPaymentId())
                                .amount(p.getAmount())
                                .build();
                    }
                }
            }
        }
        log.warn("Access DENIED | userId: {}, targetId: {}, targetType: {} | No successful payment found", userId, targetId, targetType);
        return PaymentVerificationResponse.builder()
                .isPaid(false)
                .paymentId(null)
                .amount(BigDecimal.ZERO)
                .build();
    }
}
