package com.example.payment_service.controller;
import com.example.payment_service.component.RequiresRole;
import com.example.payment_service.dto.response.TransactionHistoryResponse;
import com.example.payment_service.model.PayoutRequest;
import com.example.payment_service.service.WalletService;
import com.example.payment_service.util.JwtUtil;
import com.example.payment_service.dto.response.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class AdminController {
    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    @GetMapping("/all/payouts")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<List<PayoutRequest>> getAllPayouts(@RequestHeader("Authorization") String token) {
        log.info("hit ");
        try {
            List<PayoutRequest> payouts = walletService.getAllPayouts();
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            log.error("Error fetching pending payouts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/payouts/process")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<String> processPayoutRequest(
            @RequestBody ProcessPayoutRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            // Extract admin ID from token for audit trail
            String adminIdFromToken = jwtUtil.extractUserId(token);
            
            // Use admin ID from token instead of request body for security
            walletService.processPayoutRequest(request.getPayoutId(), adminIdFromToken, 
                                             request.getAction(), request.getRemarks());
            
            log.info("Payout {} processed by admin {}: {}", 
                    request.getPayoutId(), adminIdFromToken, request.getAction());
            
            return ResponseEntity.ok("Payout request " + request.getAction().toLowerCase() + "d successfully");
        } catch (Exception e) {
            log.error("Error processing payout request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process payout: " + e.getMessage());
        }
    }

    @PostMapping("/payouts/process/{payoutId}")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<String> processPayoutRequest(
            @PathVariable("payoutId") String payoutId,
            @RequestParam("action") String action,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestHeader("Authorization") String token
    ) {

        try {
            String adminIdFromToken = jwtUtil.extractUserId(token);
            
            walletService.processPayoutRequest(payoutId, adminIdFromToken, action, remarks);
            
            log.info("Payout {} processed by admin {}: {}", payoutId, adminIdFromToken, action);
            
            return ResponseEntity.ok("Payout request " + action.toLowerCase() + "d successfully");
        } catch (Exception e) {
            log.error("Error processing payout request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process payout: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(@RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(walletService.getAdminDashboard());
        } catch (Exception e) {
            log.error("Error fetching admin dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    @GetMapping("/all/transactions")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(){
                TransactionHistoryResponse response = walletService.getAllTransactionHistory();
            return ResponseEntity.ok(response);
    }

    public static class ProcessPayoutRequest {
        private String payoutId;
        private String action; 
        private String remarks;
        
        public String getPayoutId() { return payoutId; }
        public void setPayoutId(String payoutId) { this.payoutId = payoutId; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}