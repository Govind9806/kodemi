package com.example.admin_service.controller;

import com.example.admin_service.component.RequiresRole;
import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.TrainerResponseDTO;
import com.example.admin_service.dto.response.TransactionHistoryResponse;
import com.example.admin_service.service.AdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @RequiresRole("SUPER_ADMIN")
    @PostMapping("/create")
    public ResponseEntity<String> subAdmin(@RequestHeader("Authorization") String token, @RequestBody SubAdminRequest request){
        return ResponseEntity.ok(adminService.subAdminCreate(token, request));
    }

    @PostMapping("/update")
    public ResponseEntity<Object> setSubAdmin(@RequestBody SubAdminDetailsDTO request, @RequestHeader("Authorization") String token){
        return ResponseEntity.ok(adminService.setSubAdmin(request, token));
    }

    @PostMapping("/change/password")
    public ResponseEntity<String> updatePassword(@RequestBody PasswordChange passwordChange, @RequestHeader("Authorization") String token){
        return  ResponseEntity.ok(adminService.changePassword(passwordChange.getPassword(), token));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> adminLogin(@Valid @RequestBody AdminLoginDTO request){
        log.info("Admin Login Controller hit");
        return ResponseEntity.ok(adminService.login(request));
    }

    @GetMapping("/user/{id}")
    @RequiresRole("USER_ADMIN")
    public ResponseEntity<Object> getTrainer(@RequestHeader("Authorization") String token,
            @PathVariable("id") String id
    ) {
        log.info("Role Accepted");
        Object user = adminService.getUser(token, id);
        return ResponseEntity.ok(user);
    }



    @GetMapping("all/trainers")
    @RequiresRole("USER_ADMIN")
    public ResponseEntity<List<TrainerResponseDTO>> getAllTrainer(@RequestHeader("Authorization") String token){
        return ResponseEntity.ok(adminService.getAllTrainer(token));
    }




    @GetMapping("/all/payouts")
    @RequiresRole({"SUPER_ADMIN", "PAYMENT_ADMIN"})
    public ResponseEntity<List<PayoutRequest>> getAllPayouts(@RequestHeader("Authorization") String token){
        log.info("Getting All payouts.. ");
        return ResponseEntity.ok(adminService.getAllPayouts(token));
    }

    @PostMapping("/payouts/process")
    @RequiresRole({"SUPER_ADMIN", "PAYMENT_ADMIN"})
    public ResponseEntity<String> processPayoutRequest(@RequestHeader("Authorization") String token, @RequestBody ProcessPayoutRequest request){
        return ResponseEntity.ok(adminService.processPayoutRequest(token, request));
    }

    @PostMapping("/payouts/process/{payoutId}")
    @RequiresRole({"SUPER_ADMIN", "PAYMENT_ADMIN"})
    public ResponseEntity<String> processPayoutRequestByPath(@PathVariable String payoutId,
                                                             @RequestParam String action, // APPROVE or REJECT
                                                             @RequestParam(required = false) String remarks,
                                                             @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(adminService.processPayoutRequestByPath(token,action,payoutId,remarks));
    }
    @GetMapping("/all/transactions")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(){
                TransactionHistoryResponse response = adminService.getAllTransactionHistory();
            return ResponseEntity.ok(response);
    }

    @PostMapping("/broadcast")
    @RequiresRole("SUPER_ADMIN")
    public ResponseEntity<String> broadcastAnnouncement(
            @RequestHeader("Authorization") String token,
            @RequestBody com.example.admin_service.dto.request.AdminBroadcastRequest request) {
        return ResponseEntity.ok(adminService.broadcastAnnouncement(token, request));
    }

    @PostMapping("/suspend/{userId}")
    @RequiresRole("USER_ADMIN")
    public ResponseEntity<String> suspendUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String userId,
            @RequestParam String reason) {
        return ResponseEntity.ok(adminService.suspendUser(token, userId, reason));
    }

    // --- Backwards Compatibility Endpoints ---

    @RequestMapping(value = "/course/review/{courseId}", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRole("COURSE_ADMIN")
    public ResponseEntity<java.util.Map<String, Object>> courseReviewCompat(
            @RequestHeader("Authorization") String token,
            @PathVariable String courseId,
            @RequestBody(required = false) CourseModerationRequest request,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String remarks) {
        
        String finalAction = action;
        String finalRemarks = remarks;
        
        if (request != null) {
            if (finalAction == null) finalAction = request.getAction();
            if (finalRemarks == null) finalRemarks = request.getRemarks();
        }
        
        if (finalAction == null || finalAction.isBlank()) {
            finalAction = "APPROVE";
        }
        
        log.info("Legacy course review API hit: courseId={}, action={}", courseId, finalAction);
        return ResponseEntity.ok(adminService.courseModeration(token, courseId, finalAction, finalRemarks));
    }

    @RequestMapping(value = "/trainer/review/{trainerId}", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRole("USER_ADMIN")
    public ResponseEntity<Object> trainerReviewCompat(
            @RequestHeader("Authorization") String token,
            @PathVariable String trainerId,
            @RequestBody(required = false) TrainerReviewRequest request,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String remarks) {
        
        String finalAction = action;
        String finalRemarks = remarks;
        
        if (request != null) {
            if (finalAction == null) finalAction = request.getAction();
            if (finalRemarks == null) finalRemarks = request.getRemarks();
        }
        
        if (finalAction == null || finalAction.isBlank()) {
            finalAction = "APPROVE";
        }
        
        log.info("Legacy trainer review API hit: trainerId={}, action={}", trainerId, finalAction);
        return ResponseEntity.ok(adminService.trainerModeration(token, trainerId, finalAction, finalRemarks));
    }

}

