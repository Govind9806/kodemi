package com.example.enrollment_progress_service.service;

import com.example.enrollment_progress_service.dto.request.EnrollmentRequest;
import com.example.enrollment_progress_service.dto.request.PaymentSuccessRequest;
import com.example.enrollment_progress_service.dto.request.UnenrollRequest;
import com.example.enrollment_progress_service.dto.request.BatchEnrollmentStatsRequest;
import com.example.enrollment_progress_service.dto.response.*;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import com.example.enrollment_progress_service.exception.BadRequestException;
import com.example.enrollment_progress_service.exception.ResourceNotFoundException;
import com.example.enrollment_progress_service.feign.CourseClient;
import com.example.enrollment_progress_service.feign.LiveClassClient;
import com.example.enrollment_progress_service.feign.PaymentClient;
import com.example.enrollment_progress_service.feign.UserClient;
import com.example.enrollment_progress_service.service.notification.NotificationPublisher;
import com.example.enrollment_progress_service.dto.notification.NotificationRequest;
import com.example.enrollment_progress_service.dto.notification.NotificationType;
import com.example.enrollment_progress_service.dto.notification.NotificationChannel;
import com.example.enrollment_progress_service.model.EnrollmentEntity;
import com.example.enrollment_progress_service.repository.EnrollmentRepository;
import com.example.enrollment_progress_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;
    private final LiveClassClient liveClassClient;
    private final PaymentClient paymentClient;
    private final UserClient userClient;
    private final NotificationPublisher notificationPublisher;
    private final JwtUtil jwtUtil;

    @org.springframework.cache.annotation.Caching(evict = {
        @org.springframework.cache.annotation.CacheEvict(value = "userEnrollments", key = "#userId"),
        @org.springframework.cache.annotation.CacheEvict(value = "enrollmentStatus", key = "#userId + '_' + #request.targetId + '_' + #request.targetType"),
        @org.springframework.cache.annotation.CacheEvict(value = "accessCheck", key = "#userId + '_' + #request.targetId + '_' + #request.targetType")
    })
    public EnrollmentResponse enroll(String userId, EnrollmentRequest request, String token) {
        if (request == null || request.getTargetId() == null || request.getTargetId().isBlank()) {
            throw new BadRequestException("targetId (or courseId) is required and cannot be blank");
        }
        if (request.getTargetType() == null) {
            request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        }
        log.info("User {} enrolling in {} {}", userId, request.getTargetType(), request.getTargetId());

        EnrollmentEntity activeEnrollment = enrollmentRepository.findActiveEnrollment(userId, request.getTargetId(), request.getTargetType().name());
        if (activeEnrollment != null) {
            log.info("User {} already enrolled in {} {}", userId, request.getTargetType(), request.getTargetId());
            return buildEnrollmentResponse("Already enrolled", activeEnrollment.getEnrollmentId(), activeEnrollment.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, false, activeEnrollment.getPaymentId(), BigDecimal.ZERO);
        }

        EnrollmentItemInfoResponse itemInfo;
        try {
            if (request.getTargetType() == EnrollmentTargetType.RECORDED_COURSE || request.getTargetType() == EnrollmentTargetType.LIVE_COURSE) {
                itemInfo = courseClient.getCourseEnrollmentInfo(request.getTargetId());
            } else {
                itemInfo = liveClassClient.getConferenceEnrollmentInfo(request.getTargetId());
            }
        } catch (Exception e) {
            log.error("Error fetching target info: ", e);
            throw new BadRequestException("Invalid target or target not found");
        }

        if (itemInfo == null || itemInfo.getIsVerified() == null || !itemInfo.getIsVerified()) {
            throw new BadRequestException("Target item is not available or not verified");
        }

        log.info("Enrollment item info retrieved: isFree={}, price={}, isVerified={}",
                itemInfo.getIsFree(), itemInfo.getPrice(), itemInfo.getIsVerified());

        if (itemInfo.getIsFree() != null && itemInfo.getIsFree()) {
            log.info("Target item is free. Proceeding with instant enrollment activation for userId: {}", userId);
            EnrollmentEntity newEnrollment = createEnrollment(userId, request.getTargetId() , request.getTargetType(), EnrollmentStatus.ACTIVE, null, itemInfo != null ? itemInfo.getCreatorId() : null);
            sendEnrollmentNotifications(userId, request.getTargetId(), request.getTargetType(), true, token);
            return buildEnrollmentResponse("Enrollment successful", newEnrollment.getEnrollmentId(), newEnrollment.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, false, null, BigDecimal.ZERO);
        }

        log.info("Target item requires payment. Verifying payment status for userId: {}, targetId: {}", userId, request.getTargetId());
        PaymentVerificationResponse paymentResp;
        try {
            paymentResp = paymentClient.verifyPayment(userId, request.getTargetId(), request.getTargetType().name());
            log.info("Payment client verification result: isPaid={}, paymentId={}, amount={}",
                    paymentResp != null && paymentResp.isPaid(),
                    paymentResp != null ? paymentResp.getPaymentId() : null,
                    paymentResp != null ? paymentResp.getAmount() : null);
        } catch (Exception e) {
            log.error("Error verifying payment: ", e);
            throw new BadRequestException("Error verifying payment");
        }

        if (paymentResp != null && paymentResp.isPaid()) {
            if (paymentResp.getAmount() == null || paymentResp.getAmount().compareTo(itemInfo.getPrice()) < 0) {
                log.warn("Payment amount mismatch during enroll. Paid: {}, Required: {}", paymentResp.getAmount(), itemInfo.getPrice());
                throw new BadRequestException("Payment amount is less than the required price");
            }
            EnrollmentEntity newEnrollment = createEnrollment(userId, request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, paymentResp.getPaymentId(), itemInfo != null ? itemInfo.getCreatorId() : null);
            sendEnrollmentNotifications(userId, request.getTargetId(), request.getTargetType(), true, token);
            return buildEnrollmentResponse("Enrollment successful", newEnrollment.getEnrollmentId(), itemInfo.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, false, paymentResp.getPaymentId(), paymentResp.getAmount());
        }

        EnrollmentEntity pendingEnrollment = createEnrollment(userId, request.getTargetId(), request.getTargetType(), EnrollmentStatus.PAYMENT_REQUIRED, null, itemInfo != null ? itemInfo.getCreatorId() : null);
        return buildEnrollmentResponse("Payment required", pendingEnrollment.getEnrollmentId(), itemInfo.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.PAYMENT_REQUIRED, true, null, itemInfo.getPrice());
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @org.springframework.cache.annotation.CacheEvict(value = "userEnrollments", key = "#userId"),
        @org.springframework.cache.annotation.CacheEvict(value = "enrollmentStatus", key = "#userId + '_' + #request.targetId + '_' + #request.targetType"),
        @org.springframework.cache.annotation.CacheEvict(value = "accessCheck", key = "#userId + '_' + #request.targetId + '_' + #request.targetType")
    })
    public EnrollmentResponse unenroll(String userId, UnenrollRequest request) {
        log.info("User {} unenrolling from {} {}", userId, request.getTargetType(), request.getTargetId());

        EnrollmentEntity activeEnrollment = enrollmentRepository.findActiveEnrollment(userId, request.getTargetId(), request.getTargetType().name());
        if (activeEnrollment == null) {
            throw new ResourceNotFoundException("Active enrollment not found");
        }

        activeEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        activeEnrollment.setUnenrolledAt(Instant.now().toString());
        activeEnrollment.setUpdatedAt(Instant.now().toString());
        enrollmentRepository.save(activeEnrollment);

        // Notify the user of successful unenrollment
        try {
            NotificationRequest notif = NotificationRequest.builder()
                    .userId(userId)
                    .title("Unenrolled Successfully")
                    .message("You have successfully unenrolled from the " + (request.getTargetType() == EnrollmentTargetType.RECORDED_COURSE || request.getTargetType() == EnrollmentTargetType.LIVE_COURSE ? "course" : "class") + ".")
                    .type(NotificationType.ENROLLMENT_FAILED)
                    .channels(java.util.List.of(NotificationChannel.IN_APP))
                    .referenceId(request.getTargetId())
                    .referenceType(request.getTargetType().name())
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception e) {
            log.error("Failed to send unenrollment notification", e);
        }

        return buildEnrollmentResponse("Unenrolled successfully", activeEnrollment.getEnrollmentId(), activeEnrollment.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.CANCELLED, false, null, BigDecimal.ZERO);
    }

    @org.springframework.cache.annotation.Cacheable(value = "userEnrollments", key = "#userId")
    public List<EnrollmentEntity> getMyEnrollments(String userId) {
        log.info("Retrieving all enrollments for userId: {}", userId);

        List<EnrollmentEntity> enrollments =
                new ArrayList<>(enrollmentRepository.findByUserId(userId));

        log.info("Found {} enrollments for userId: {}", enrollments.size(), userId);

        return enrollments;
    }

    @org.springframework.cache.annotation.Cacheable(value = "enrollmentStatus", key = "#userId + '_' + #targetId + '_' + #targetTypeStr")
    public EnrollmentStatusResponse getStatus(String userId, String targetId, String targetTypeStr) {
        log.info("Retrieving enrollment status for userId: {}, targetId: {}, targetType: {}", userId, targetId, targetTypeStr);
        EnrollmentTargetType type;
        try {
            type = EnrollmentTargetType.valueOf(targetTypeStr);
        } catch (Exception e) {
            log.error("Failed to parse target type: {} for status query, userId: {}", targetTypeStr, userId, e);
            throw new BadRequestException("Invalid target type");
        }

        EnrollmentEntity activeEnrollment = enrollmentRepository.findActiveEnrollment(userId, targetId, type.name());
        if (activeEnrollment == null) {
            try {
                PaymentVerificationResponse paymentResp = paymentClient.verifyPayment(userId, targetId, type.name());
                if (paymentResp != null && paymentResp.isPaid()) {
                    log.info("Self-healing: Found verified payment for userId: {}, targetId: {}, targetType: {}. Activating enrollment...", userId, targetId, type);
                    PaymentSuccessRequest req = PaymentSuccessRequest.builder()
                            .userId(userId)
                            .targetId(targetId)
                            .targetType(type)
                            .paymentId(paymentResp.getPaymentId())
                            .status("SUCCESS")
                            .build();
                    activateEnrollmentAfterPayment(req);
                    activeEnrollment = enrollmentRepository.findActiveEnrollment(userId, targetId, type.name());
                }
            } catch (Exception e) {
                log.warn("Self-healing enrollment check failed for userId: {}, targetId: {}: {}", userId, targetId, e.getMessage());
            }
        }
        
        log.info("Enrollment status query for userId: {}, targetId: {}, targetType: {} -> active: {}", userId, targetId, type, activeEnrollment != null);
        return EnrollmentStatusResponse.builder()
                .enrolled(activeEnrollment != null)
                .status(activeEnrollment != null ? EnrollmentStatus.ACTIVE : null)
                .targetId(targetId)
                .targetType(type)
                .build();
    }

    @org.springframework.cache.annotation.Cacheable(value = "accessCheck", key = "#userId + '_' + #targetId + '_' + #targetTypeStr")
    public AccessCheckResponse checkAccess(String userId, String targetId, String targetTypeStr) {
        log.info("Checking access for userId: {}, targetId: {}, targetType: {}", userId, targetId, targetTypeStr);
        EnrollmentTargetType type;
        try {
            type = EnrollmentTargetType.valueOf(targetTypeStr);
        } catch (Exception e) {
            log.error("Failed to parse target type: {} for access check, userId: {}", targetTypeStr, userId, e);
            throw new BadRequestException("Invalid target type");
        }

        EnrollmentEntity activeEnrollment = enrollmentRepository.findActiveEnrollment(userId, targetId, type.name());
        
        boolean hasAccess = activeEnrollment != null;
        log.info("Access check result for userId: {}, targetId: {}, targetType: {} -> hasAccess: {}", userId, targetId, type, hasAccess);
        return AccessCheckResponse.builder()
                .hasAccess(hasAccess)
                .status(activeEnrollment != null ? EnrollmentStatus.ACTIVE : null)
                .targetId(targetId)
                .targetType(type)
                .build();
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @org.springframework.cache.annotation.CacheEvict(value = "userEnrollments", key = "#request.userId"),
        @org.springframework.cache.annotation.CacheEvict(value = "enrollmentStatus", key = "#request.userId + '_' + #request.targetId + '_' + #request.targetType"),
        @org.springframework.cache.annotation.CacheEvict(value = "accessCheck", key = "#request.userId + '_' + #request.targetId + '_' + #request.targetType")
    })
    public EnrollmentResponse activateEnrollmentAfterPayment(PaymentSuccessRequest request) {
        log.info("Payment success callback received | userId={} | targetId={} | targetType={} | orderId={}",
                request.getUserId(), request.getTargetId(), request.getTargetType(), request.getOrderId());

        if (!"SUCCESS".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Invalid payment status for activation");
        }

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByUserIdAndTargetIdAndTargetType(
                request.getUserId(), request.getTargetId(), request.getTargetType().name()
        );

        // Check if there is an active one first (Idempotency)
        EnrollmentEntity targetEnrollment = null;
        if (enrollments != null) {
            for (EnrollmentEntity e : enrollments) {
                if (e != null && EnrollmentStatus.ACTIVE.equals(e.getStatus())) {
                    targetEnrollment = e;
                    break;
                }
            }
        }

        if (targetEnrollment != null) {
            log.info("Duplicate payment callback ignored because enrollment already ACTIVE | enrollmentId={}", targetEnrollment.getEnrollmentId());
            return buildEnrollmentResponse("Enrollment is already ACTIVE", targetEnrollment.getEnrollmentId(), targetEnrollment.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, false, targetEnrollment.getPaymentId(), BigDecimal.ZERO);
        }

        // Security Check: Actually verify the payment from payment-service to prevent hacking
        PaymentVerificationResponse paymentResp = paymentClient.verifyPayment(request.getUserId(), request.getTargetId(), request.getTargetType().name());
        if (paymentResp == null || !paymentResp.isPaid()) {
            throw new BadRequestException("Payment has not been successfully verified by payment service");
        }

        EnrollmentItemInfoResponse itemInfo;
        try {
            if (request.getTargetType() == EnrollmentTargetType.RECORDED_COURSE || request.getTargetType() == EnrollmentTargetType.LIVE_COURSE) {
                itemInfo = courseClient.getCourseEnrollmentInfo(request.getTargetId());
            } else {
                itemInfo = liveClassClient.getConferenceEnrollmentInfo(request.getTargetId());
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to fetch item info");
        }

        // Find PAYMENT_REQUIRED one
        if (enrollments != null) {
            for (EnrollmentEntity e : enrollments) {
                if (e != null && EnrollmentStatus.PAYMENT_REQUIRED.equals(e.getStatus())) {
                    targetEnrollment = e;
                    break;
                }
            }
        }

        if (paymentResp.getAmount() == null || (itemInfo != null && itemInfo.getPrice() != null && itemInfo.getPrice().compareTo(BigDecimal.ZERO) > 0 && paymentResp.getAmount().compareTo(itemInfo.getPrice()) < 0)) {
            log.warn("Payment amount mismatch. Paid: {}, Required: {}", paymentResp.getAmount(), itemInfo != null ? itemInfo.getPrice() : null);
            throw new BadRequestException("Payment amount is less than the required price");
        }

        if (targetEnrollment == null) {
            log.info("No pre-existing enrollment record found to activate. Creating new ACTIVE enrollment | userId={} | targetId={}", request.getUserId(), request.getTargetId());
            targetEnrollment = createEnrollment(request.getUserId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, request.getPaymentId(), itemInfo != null ? itemInfo.getCreatorId() : null);
        }

        targetEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        targetEnrollment.setPaymentId(request.getPaymentId());
        targetEnrollment.setOrderId(request.getOrderId());
        targetEnrollment.setPaidAt(Instant.now().toString());
        targetEnrollment.setUpdatedAt(Instant.now().toString());
        if (targetEnrollment.getCreatorId() == null && itemInfo != null && itemInfo.getCreatorId() != null) {
            targetEnrollment.setCreatorId(itemInfo.getCreatorId());
        }
        
        enrollmentRepository.save(targetEnrollment);
        
        log.info("Enrollment activated successfully | enrollmentId={}", targetEnrollment.getEnrollmentId());
        
        sendEnrollmentNotifications(request.getUserId(), request.getTargetId(), request.getTargetType(), false, null);

        return buildEnrollmentResponse("Enrollment activated successfully", targetEnrollment.getEnrollmentId(), targetEnrollment.getCreatorId(), request.getTargetId(), request.getTargetType(), EnrollmentStatus.ACTIVE, false, targetEnrollment.getPaymentId(), BigDecimal.ZERO);
    }

    private EnrollmentEntity createEnrollment(String userId, String targetId, EnrollmentTargetType type, EnrollmentStatus status, String paymentId, String creatorId) {
        log.info("Creating new enrollment record in database: userId={}, targetId={}, type={}, status={}, paymentId={}, creatorId={}",
                userId, targetId, type, status, paymentId, creatorId);
        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setEnrollmentId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setTargetId(targetId);
        entity.setTargetType(type);
        entity.setStatus(status);
        entity.setPaymentId(paymentId);
        entity.setCreatorId(creatorId != null && !creatorId.isBlank() ? creatorId : "SYSTEM");
        String now = Instant.now().toString();
        entity.setEnrolledAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        enrollmentRepository.save(entity);
        log.info("Enrollment record saved successfully with enrollmentId: {}", entity.getEnrollmentId());
        return entity;
    }

    private void sendEnrollmentNotifications(String learnerId, String targetId, EnrollmentTargetType targetType, boolean isCreation, String token) {
        log.info("Triggering enrollment notifications: learnerId={}, targetId={}, targetType={}, isCreation={}",
                learnerId, targetId, targetType, isCreation);
        try {
            // A & B: Notify Learner
            String learnerMsg = isCreation ? "Your enrollment has been created successfully." : "Your enrollment is active. You can now access the course.";
            NotificationRequest learnerNotif = NotificationRequest.builder()
                    .userId(learnerId)
                    .title(isCreation ? "Enrollment Created" : "Enrollment Active")
                    .message(learnerMsg)
                    .type(NotificationType.COURSE_ENROLLED)
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceId(targetId)
                    .referenceType(targetType.name())
                    .build();
            log.info("Sending in-app/email notification to learner: {}", learnerId);
            notificationPublisher.publish(learnerNotif);

            // C: Notify Trainer (if RECORDED_COURSE or LIVE_COURSE)
            if (targetType == EnrollmentTargetType.RECORDED_COURSE || targetType == EnrollmentTargetType.LIVE_COURSE) {
                log.info("Fetching course details for targetId: {} to notify creator", targetId);
                CourseResponseDTO courseDetail = courseClient.getCourseDetail(targetId);
                if (courseDetail != null && courseDetail.getCreatorId() != null) {
                    NotificationRequest trainerNotif = NotificationRequest.builder()
                            .userId(courseDetail.getCreatorId())
                            .title("New Enrollment")
                            .message("A new learner has enrolled in your course: " + courseDetail.getTitle())
                            .type(NotificationType.COURSE_ENROLLED)
                            .channels(List.of(NotificationChannel.IN_APP))
                            .referenceId(targetId)
                            .referenceType(targetType.name())
                            .build();
                    log.info("Sending notification to course creator: {}", courseDetail.getCreatorId());
                    notificationPublisher.publish(trainerNotif);
                } else {
                    log.warn("Could not retrieve course creator for targetId: {}", targetId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send enrollment notifications for targetId {}", targetId, e);
        }
    }

    private EnrollmentResponse buildEnrollmentResponse(String message, String id, String creatorId, String targetId, EnrollmentTargetType type, EnrollmentStatus status, boolean req, String payId, BigDecimal amt) {
        return EnrollmentResponse.builder()
                .message(message)
                .enrollmentId(id)
                .creatorId(creatorId)
                .targetId(targetId)
                .targetType(type)
                .status(status)
                .paymentRequired(req)
                .paymentId(payId)
                .amount(amt)
                .build();
    }

    public List<String> getEnrolledLearners(String courseId) {
        log.info("Retrieving enrolled learners for courseId: {}", courseId);
        List<EnrollmentEntity> list = enrollmentRepository.findByTargetId(courseId);
        List<String> userIds = new java.util.ArrayList<>();
        if (list != null) {
            for (EnrollmentEntity e : list) {
                if (e != null && EnrollmentStatus.ACTIVE.equals(e.getStatus())) {
                    userIds.add(e.getUserId());
                }
            }
        }
        log.info("Found {} active learners enrolled in courseId: {}", userIds.size(), courseId);
        return userIds;
    }

    public CourseEnrollmentStatsResponse getEnrollmentStats(String targetId, String targetTypeStr) {
        log.info("Retrieving enrollment stats for targetId: {}, targetType: {}", targetId, targetTypeStr);
        
        String targetType = (targetTypeStr == null || targetTypeStr.isBlank()) 
                ? EnrollmentTargetType.RECORDED_COURSE.name() 
                : targetTypeStr;

        try {
            EnrollmentTargetType.valueOf(targetType);
        } catch (Exception e) {
            log.error("Failed to parse target type: {} for stats query, targetId: {}", targetTypeStr, targetId, e);
            throw new BadRequestException("Invalid target type");
        }

        List<EnrollmentEntity> list = enrollmentRepository.findByTargetId(targetId);
        long active = 0;
        long cancelled = 0;
        long paymentRequired = 0;
        long expired = 0;
        long total = 0;

        if (list != null) {
            for (EnrollmentEntity e : list) {
                if (e != null && e.getTargetType() != null && e.getTargetType().name().equals(targetType)) {
                    total++;
                    if (e.getStatus() != null) {
                        switch (e.getStatus()) {
                            case ACTIVE:
                                active++;
                                break;
                            case CANCELLED:
                                cancelled++;
                                break;
                            case PAYMENT_REQUIRED:
                                paymentRequired++;
                                break;
                            case EXPIRED:
                                expired++;
                                break;
                        }
                    }
                }
            }
        }

        log.info("Stats computed for targetId: {} | active: {}, cancelled: {}, paymentRequired: {}, expired: {}, total: {}", 
                targetId, active, cancelled, paymentRequired, expired, total);

        return CourseEnrollmentStatsResponse.builder()
                .targetId(targetId)
                .targetType(targetType)
                .activeCount(active)
                .cancelledCount(cancelled)
                .paymentRequiredCount(paymentRequired)
                .expiredCount(expired)
                .totalCount(total)
                .build();
    }

    public java.util.Map<String, CourseEnrollmentStatsResponse> getBatchEnrollmentStats(BatchEnrollmentStatsRequest request) {
        log.info("Retrieving batch enrollment stats for targetIds: {}", request.getTargetIds());
        java.util.Map<String, CourseEnrollmentStatsResponse> resultMap = new java.util.HashMap<>();
        if (request != null && request.getTargetIds() != null) {
            for (String targetId : request.getTargetIds()) {
                if (targetId != null && !targetId.isBlank()) {
                    resultMap.put(targetId, getEnrollmentStats(targetId, request.getTargetType()));
                }
            }
        }
        return resultMap;
    }

    public List<TrainerCourseStudentsResponse> getMyStudents(String token) {
        String creatorId = jwtUtil.extractUserId(token);
        log.info("Retrieving course & student enrollment breakdown for trainer creatorId: {}", creatorId);

        List<EnrollmentEntity> allEnrollments = new ArrayList<>(enrollmentRepository.findByCreatorId(creatorId));

        // Fallback & Auto-backfill: Resolve unassigned/SYSTEM enrollments for courses belonging to this creator
        try {
            List<EnrollmentEntity> unassigned = enrollmentRepository.findByCreatorId("SYSTEM");
            if (unassigned != null) {
                java.util.Set<String> matchedCourseIds = new java.util.HashSet<>();
                for (EnrollmentEntity entity : unassigned) {
                    if (entity == null || entity.getTargetId() == null) continue;
                    String targetId = entity.getTargetId();
                    if (matchedCourseIds.contains(targetId)) {
                        entity.setCreatorId(creatorId);
                        allEnrollments.add(entity);
                    } else {
                        try {
                            com.example.enrollment_progress_service.dto.response.CourseResponseDTO courseDetail = courseClient.getCourseDetail(targetId);
                            if (courseDetail != null && creatorId.equals(courseDetail.getCreatorId())) {
                                matchedCourseIds.add(targetId);
                                entity.setCreatorId(creatorId);
                                enrollmentRepository.save(entity);
                                allEnrollments.add(entity);
                            }
                        } catch (Exception ex) {
                            log.debug("Course detail lookup failed for targetId: {}", targetId);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed unassigned enrollments resolution: {}", ex.getMessage());
        }

        if (allEnrollments.isEmpty()) {
            return new ArrayList<>();
        }

        // Group enrollments by targetId (courseId)
        java.util.Map<String, List<EnrollmentEntity>> courseEnrollmentMap = new java.util.LinkedHashMap<>();
        for (EnrollmentEntity entity : allEnrollments) {
            if (entity != null && entity.getTargetId() != null) {
                courseEnrollmentMap.computeIfAbsent(entity.getTargetId(), k -> new ArrayList<>()).add(entity);
            }
        }

        List<TrainerCourseStudentsResponse> responseList = new ArrayList<>();

        for (java.util.Map.Entry<String, List<EnrollmentEntity>> entry : courseEnrollmentMap.entrySet()) {
            String courseId = entry.getKey();
            List<EnrollmentEntity> enrollments = entry.getValue();

            // 1. Fetch course details
            String courseTitle = "Course " + courseId;
            String targetTypeStr = enrollments.get(0).getTargetType() != null ? enrollments.get(0).getTargetType().name() : "RECORDED_COURSE";
            BigDecimal price = BigDecimal.ZERO;

            try {
                if (enrollments.get(0).getTargetType() == EnrollmentTargetType.RECORDED_COURSE || enrollments.get(0).getTargetType() == EnrollmentTargetType.LIVE_COURSE) {
                    com.example.enrollment_progress_service.dto.response.CourseResponseDTO courseDetail = courseClient.getCourseDetail(courseId);
                    if (courseDetail != null) {
                        if (courseDetail.getTitle() != null && !courseDetail.getTitle().isBlank()) {
                            courseTitle = courseDetail.getTitle();
                        }
                        if (courseDetail.getCourseType() != null && !courseDetail.getCourseType().isBlank()) {
                            targetTypeStr = courseDetail.getCourseType();
                        }
                        if (courseDetail.getPrice() != null) {
                            price = courseDetail.getPrice();
                        }
                    }
                } else {
                    EnrollmentItemInfoResponse itemInfo = liveClassClient.getConferenceEnrollmentInfo(courseId);
                    if (itemInfo != null) {
                        if (itemInfo.getTitle() != null && !itemInfo.getTitle().isBlank()) {
                            courseTitle = itemInfo.getTitle();
                        }
                        if (itemInfo.getPrice() != null) {
                            price = itemInfo.getPrice();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch details for courseId {}: {}", courseId, e.getMessage());
            }

            // 2. Build student list
            List<StudentDetailResponse> studentDetailsList = new ArrayList<>();
            for (EnrollmentEntity enrollment : enrollments) {
                String studentId = enrollment.getUserId();
                String studentName = "Learner (" + studentId + ")";
                String studentEmail = null;
                String studentPhoto = null;

                try {
                    LearnerProfileDTO profile = userClient.getLearnerDetails(studentId);
                    if (profile != null) {
                        if (profile.getFullName() != null && !profile.getFullName().isBlank()) {
                            studentName = profile.getFullName();
                        } else if (profile.getUsername() != null && !profile.getUsername().isBlank()) {
                            studentName = profile.getUsername();
                        }
                        studentEmail = profile.getEmail();
                        studentPhoto = profile.getProfilePictureUrl();
                    }
                } catch (Exception ex) {
                    log.warn("Unable to fetch student profile for userId {}: {}", studentId, ex.getMessage());
                }

                studentDetailsList.add(StudentDetailResponse.builder()
                        .enrollmentId(enrollment.getEnrollmentId())
                        .studentId(studentId)
                        .studentName(studentName)
                        .studentEmail(studentEmail)
                        .studentPhoto(studentPhoto)
                        .status(enrollment.getStatus() != null ? enrollment.getStatus().name() : "ACTIVE")
                        .enrolledAt(enrollment.getEnrolledAt())
                        .paymentId(enrollment.getPaymentId())
                        .build());
            }

            // 3. Fetch course reviews
            List<ReviewResponseDTO> reviews = new ArrayList<>();
            try {
                List<ReviewResponseDTO> fetchedReviews = courseClient.getReviewsByCourse(courseId);
                if (fetchedReviews != null) {
                    reviews = fetchedReviews;
                }
            } catch (Exception ex) {
                log.warn("Unable to fetch reviews for courseId {}: {}", courseId, ex.getMessage());
            }

            int reviewCount = reviews.size();
            double avgRating = 0.0;
            if (reviewCount > 0) {
                double total = 0;
                int validRatings = 0;
                for (ReviewResponseDTO r : reviews) {
                    if (r != null && r.getRating() != null) {
                        total += r.getRating();
                        validRatings++;
                    }
                }
                if (validRatings > 0) {
                    avgRating = Math.round((total / validRatings) * 10.0) / 10.0;
                }
            }

            responseList.add(TrainerCourseStudentsResponse.builder()
                    .courseId(courseId)
                    .courseTitle(courseTitle)
                    .targetType(targetTypeStr)
                    .price(price)
                    .totalEnrolledStudents(studentDetailsList.size())
                    .enrolledStudents(studentDetailsList)
                    .totalReviews(reviewCount)
                    .averageRating(avgRating)
                    .reviews(reviews)
                    .build());
        }

        return responseList;
    }
}
