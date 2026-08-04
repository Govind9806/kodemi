package com.example.payment_service.controller;
import com.example.payment_service.dto.request.PayoutRequestDto;
import com.example.payment_service.dto.response.TransactionHistoryResponse;
import com.example.payment_service.dto.response.TrainerRevenueResponse;
import com.example.payment_service.dto.response.WalletResponse;
import com.example.payment_service.service.WalletService;
import com.example.payment_service.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

/**
 * CHANGE COMMENT: CORS AND NEW ENDPOINT ADDED TO TRAINER WALLET CONTROLLER
 * 
 * This controller now supports:
 * 1. Cross-Origin Resource Sharing (CORS) configuration
 * 2. New endpoint for trainer revenue analytics
 * 
 * Allowed Origins (Development): localhost:3000, 4200, 5173, 8000
 * Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
 * Credentials: Enabled
 */
@RestController
@RequestMapping("/api/v1/trainer-wallet")
@CrossOrigin(
        origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:8000", "http://localhost:5173", "https://yourdomain.com"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = "*",
        allowCredentials = "true",
        maxAge = 3600
)
@RateLimiter(name = "standardEndpoint")
public class TrainerWalletController {
    private final WalletService walletService;
    private final JwtUtil jwtUtil;
    
    public TrainerWalletController(WalletService walletService, JwtUtil jwtUtil) {
        this.walletService = walletService;
        this.jwtUtil = jwtUtil;
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<WalletResponse> getTrainerWallet(@RequestHeader("Authorization") String token) {
        String trainerId = jwtUtil.extractUserId(token);
        WalletResponse response = walletService.getTrainerWallet(trainerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<TransactionHistoryResponse> getTrainerTransactions(
            @RequestHeader("Authorization") String token) {
        String trainerId = jwtUtil.extractUserId(token);
        TransactionHistoryResponse response = walletService.getTrainerTransactions(trainerId);
        return ResponseEntity.ok(response);
    }

    /**
     * CHANGE COMMENT: NEW ENDPOINT FOR TRAINER REVENUE ANALYTICS
     * 
     * Endpoint: GET /api/v1/trainer-wallet/revenue-analytics
     * 
     * Purpose: Return comprehensive revenue analytics for a trainer including:
     *   - Total courses sold
     *   - Total resources sold
     *   - Revenue breakdown by course
     *   - Revenue breakdown by resource
     *   - Monthly revenue trends
     *   - Current balance and pending payouts
     * 
     * Authentication: Required (JWT token in Authorization header)
     * Rate Limiting: Applied via @RateLimiter(name = "standardEndpoint")
     * CORS: Enabled for specified origins
     * 
     * @param token - JWT token containing trainer ID
     * @return TrainerRevenueResponse with complete revenue analytics
     */
    @GetMapping("/revenue-analytics")
    public ResponseEntity<TrainerRevenueResponse> getTrainerRevenueAnalytics(@RequestHeader("Authorization") String token) {
        // CHANGE: Extract trainer ID from JWT token
        String trainerId = jwtUtil.extractUserId(token);
        // CHANGE: Call new service method to get revenue analytics
        TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-payout")
    public ResponseEntity<String> requestPayout(@RequestBody PayoutRequestDto request, @RequestHeader("Authorization") String token) {
        String payoutId = walletService.requestPayout(request, token);
        return ResponseEntity.ok("Payout request created with ID: " + payoutId);
    }
}