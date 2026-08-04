package com.example.payment_service.controller;

import com.example.payment_service.service.AwsInitializationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AWS Health Check Controller
 * 
 * WHAT: Provides endpoints to check AWS DynamoDB health and status
 * WHY: Enables monitoring and debugging of AWS connectivity
 * HOW: Calls AwsInitializationService to get real-time status
 * WHERE: Exposed as HTTP endpoints for monitoring tools
 * 
 * ENDPOINTS:
 * - GET /api/v1/health/aws - AWS connection status
 * - GET /api/v1/health/dynamodb - DynamoDB table status
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class AwsHealthController {

    private final AwsInitializationService awsInitializationService;

    public AwsHealthController(AwsInitializationService awsInitializationService) {
        this.awsInitializationService = awsInitializationService;
    }

    /**
     * Check AWS DynamoDB health
     * 
     * ENDPOINT: GET /api/v1/health/aws
     * 
     * RESPONSE:
     * {
     *   "status": "UP" or "DOWN",
     *   "service": "AWS DynamoDB",
     *   "isConnected": true/false,
     *   "activeTableCount": 3,
     *   "responseTime": 45,
     *   "lastChecked": 1720000000000
     * }
     * 
     * USAGE:
     * - Kubernetes/Docker: Liveness probe
     * - Monitoring tools: Health metrics
     * - Load balancers: Endpoint availability check
     */
    @GetMapping("/aws")
    public ResponseEntity<?> checkAwsHealth() {
        try {
            AwsInitializationService.AwsHealthStatus status = awsInitializationService.getHealthStatus();

            return ResponseEntity.ok(new HealthResponse(
                    status.isConnected ? "UP" : "DOWN",
                    "AWS DynamoDB",
                    status.isConnected,
                    status.activeTableCount,
                    status.responseTime,
                    status.lastChecked
            ));

        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("DOWN", "AWS connection error: " + e.getMessage()));
        }
    }

    /**
     * Check DynamoDB table status
     * 
     * ENDPOINT: GET /api/v1/health/dynamodb
     * 
     * RESPONSE:
     * {
     *   "status": "UP" or "DEGRADED",
     *   "tables": {
     *     "Wallet": "ACTIVE",
     *     "WalletTransaction": "ACTIVE",
     *     "Payment": "ACTIVE"
     *   },
     *   "responseTimeMs": 125
     * }
     * 
     * USAGE:
     * - Dashboard widgets: Table status display
     * - Alert systems: Table issue detection
     * - Performance monitoring: Response time tracking
     */
    @GetMapping("/dynamodb")
    public ResponseEntity<?> checkDynamoDbTables() {
        try {
            long startTime = System.currentTimeMillis();

            AwsInitializationService.AwsHealthStatus status = awsInitializationService.getHealthStatus();

            long responseTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new DynamoDbHealthResponse(
                    status.isConnected ? "UP" : "DEGRADED",
                    status.activeTableCount,
                    responseTime
            ));

        } catch (Exception e) {
            log.error("DynamoDB health check failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("DEGRADED", "DynamoDB check failed: " + e.getMessage()));
        }
    }

    /**
     * Health Check Response DTO
     * Returns AWS connection status
     */
    public static class HealthResponse {
        public String status;
        public String service;
        public boolean isConnected;
        public int activeTableCount;
        public long responseTime;
        public long lastChecked;

        public HealthResponse(String status, String service, boolean isConnected,
                            int activeTableCount, long responseTime, long lastChecked) {
            this.status = status;
            this.service = service;
            this.isConnected = isConnected;
            this.activeTableCount = activeTableCount;
            this.responseTime = responseTime;
            this.lastChecked = lastChecked;
        }
    }

    /**
     * DynamoDB Health Response DTO
     * Returns table status information
     */
    public static class DynamoDbHealthResponse {
        public String status;
        public int activeTables;
        public long responseTimeMs;

        public DynamoDbHealthResponse(String status, int activeTables, long responseTimeMs) {
            this.status = status;
            this.activeTables = activeTables;
            this.responseTimeMs = responseTimeMs;
        }
    }

    /**
     * Error Response DTO
     * Returns error information
     */
    public static class ErrorResponse {
        public String status;
        public String error;

        public ErrorResponse(String status, String error) {
            this.status = status;
            this.error = error;
        }
    }
}
