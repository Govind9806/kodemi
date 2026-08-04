package com.example.payment_service.controller;
import com.example.payment_service.dto.request.*;
import com.example.payment_service.dto.response.PaymentOrderResponse;
import com.example.payment_service.dto.response.TransactionHistoryResponse;
import com.example.payment_service.dto.response.WalletResponse;
import com.example.payment_service.model.Wallet;
import com.example.payment_service.service.WalletService;
import com.example.payment_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RateLimiter(name = "standardEndpoint")
public class WalletController {
    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    public WalletController(WalletService walletService, JwtUtil jwtUtil) {
        this.walletService = walletService;
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/create")
    public ResponseEntity<Wallet> createWallet(
            @RequestHeader("Authorization") String token) {
        log.info("Creating wallet with token");
        String userId = jwtUtil.extractUserId(token);
        String userType = jwtUtil.extractRole(token);

        log.info("Creating wallet for userId: {} with userType: {}", userId, userType);

        Wallet response = walletService.createWallet(userId, userType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-funds")
    public ResponseEntity<PaymentOrderResponse> addFunds(
            @RequestHeader("Authorization") String token,
            @RequestBody AddFundsRequest request) {
        String userId = jwtUtil.extractUserId(token);
        String userType = jwtUtil.extractRole(token);
        request.setUserId(userId);
        
        PaymentOrderResponse response = walletService.addFunds(request, userType);
        log.info("Hitt {} {} for userId: {} as {}", request.getAmount(), request.getCurrency(), userId, userType);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/add-funds/confirm")
    public ResponseEntity<String> confirmAddFunds(@RequestBody AddFundsConfirmRequest request) {
        walletService.confirmAddFunds(request);
        return ResponseEntity.ok("Wallet credited successfully");
    }
    @PostMapping("/add-funds/mobile/confirm")
    public ResponseEntity<String> confirmRecharge(
            @RequestParam String paymentLinkId,
            @RequestParam Long amount,
            @RequestParam String orderId) {

        walletService.verifyPaymentLink(paymentLinkId, amount, orderId);

        return ResponseEntity.ok("Wallet credited successfully");
    }
    @PostMapping("/verify-add-funds")
    public ResponseEntity<String> verifyAddFunds(@RequestBody PaymentVerifyRequest request) {
        walletService.verifyAddFundsPayment(request);
        return ResponseEntity.ok("Payment verified successfully");
    }
    @PostMapping("/buy-course")
    public ResponseEntity<PaymentOrderResponse> buyCourse(@RequestBody BuyCourseRequest request) {
        PaymentOrderResponse response = walletService.buyCourse(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/buy-resource")
    public ResponseEntity<PaymentOrderResponse> buyResource(@RequestBody BuyResourceRequest request) {
        PaymentOrderResponse response = walletService.buyResource(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getWalletBalance(@RequestHeader("Authorization") String token) {
        String  userId =jwtUtil.extractUserId(token);
        WalletResponse response = walletService.getWalletBalance(userId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/transactions")
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        TransactionHistoryResponse response = walletService.getTransactionHistory(userId);
        return ResponseEntity.ok(response);
    }
}