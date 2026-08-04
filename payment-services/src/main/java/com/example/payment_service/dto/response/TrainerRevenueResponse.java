package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ═════════════════════════════════════════════════════════════════════════════
 * CHANGE COMMENT: NEW DTO CLASS - TRAINER REVENUE RESPONSE
 * ═════════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Response DTO for trainer revenue analytics API endpoint
 * 
 * This DTO encapsulates all revenue-related metrics for a trainer including:
 * - Total courses and resources sold
 * - Revenue breakdown by course and resource
 * - Monthly revenue trends
 * - Current wallet metrics (balance, pending, withdrawn)
 * 
 * Structure:
 * - Main class: TrainerRevenueResponse (root response object)
 * - Nested: RevenueByCourse (breakdown by individual courses)
 * - Nested: RevenueByResource (breakdown by individual resources)
 * - Nested: MonthlyRevenue (monthly revenue trends)
 * 
 * Used by: GET /api/v1/trainer-wallet/revenue-analytics
 * 
 * Example Usage:
 *   TrainerRevenueResponse response = walletService.getTrainerRevenueAnalytics(trainerId);
 *   // response contains all revenue analytics for the trainer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerRevenueResponse {
    
    // CHANGE: Main Response Fields
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * CHANGE: Trainer identifier - the user ID of the trainer
     */
    private String trainerId;
    
    /**
     * CHANGE: Total number of courses sold by this trainer
     * Calculated by counting successful RECORDED_COURSE and LIVE_COURSE payments
     */
    private Long totalCoursesSold;
    
    /**
     * CHANGE: Total number of resources sold by this trainer
     * Calculated by counting successful RESOURCE and RECORDING payments
     */
    private Long totalResourcesSold;
    
    /**
     * CHANGE: Total number of revenue transactions (includes EARNING, BUY_COURSE, BUY_RESOURCE)
     */
    private Long totalTransactions;
    
    /**
     * CHANGE: Total revenue earned by trainer
     * Calculated as sum of all EARNING transactions with SUCCESS status
     * Note: Trainer gets 80% of each sale (platform takes 20%)
     */
    private BigDecimal totalRevenue;
    
    /**
     * CHANGE: Current available balance in trainer's wallet
     * Available for withdrawal or spending
     */
    private BigDecimal balance;
    
    /**
     * CHANGE: Amount in pending payout requests
     * This amount is locked and waiting for admin approval/processing
     */
    private BigDecimal pendingPayout;
    
    /**
     * CHANGE: Total amount already withdrawn/paid out to trainer
     * Calculated as: totalRevenue - balance - pendingPayout
     */
    private BigDecimal totalWithdrawn;
    
    /**
     * CHANGE: Last time the wallet information was updated
     * Used to indicate freshness of the data
     */
    private LocalDateTime lastUpdated;
    
    // CHANGE: Breakdown Arrays
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * CHANGE: Breakdown of revenue by individual courses
     * Each entry contains courseId, courseName, unitsSold, and revenue
     * Used for understanding which courses generate most revenue
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByCourse {
        // CHANGE: Course identifier
        private String courseId;
        
        // CHANGE: Course name (currently generated, can be enhanced with actual course data)
        private String courseName;
        
        // CHANGE: How many times this course was sold
        private Long unitsSold;
        
        // CHANGE: Total revenue from this specific course
        private BigDecimal revenue;
    }
    
    /**
     * CHANGE: Breakdown of revenue by individual resources
     * Each entry contains resourceId, resourceName, unitsSold, and revenue
     * Used for understanding which resources generate most revenue
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByResource {
        // CHANGE: Resource identifier
        private String resourceId;
        
        // CHANGE: Resource name (currently generated, can be enhanced with actual resource data)
        private String resourceName;
        
        // CHANGE: How many times this resource was sold
        private Long unitsSold;
        
        // CHANGE: Total revenue from this specific resource
        private BigDecimal revenue;
    }
    
    /**
     * CHANGE: List of course-wise revenue breakdowns
     * Aggregated from payment records grouped by courseId
     */
    private List<RevenueByCourse> courseRevenue;
    
    /**
     * CHANGE: List of resource-wise revenue breakdowns
     * Aggregated from payment records grouped by resourceId
     */
    private List<RevenueByResource> resourceRevenue;
    
    // CHANGE: Monthly Trends
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * CHANGE: Monthly revenue breakdown for trend analysis
     * Each entry contains month (YYYY-MM format), amount, and transaction count
     * Sorted by month in descending order (most recent first)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyRevenue {
        // CHANGE: Month in YYYY-MM format (e.g., "2026-07" for July 2026)
        private String month;
        
        // CHANGE: Total revenue earned in that month
        private BigDecimal amount;
        
        // CHANGE: Number of earning transactions in that month
        // Helps understand sales frequency patterns
        private Long transactionCount;
    }
    
    /**
     * CHANGE: List of monthly revenue breakdowns for trend analysis
     * Sorted by month descending (newest months first)
     * Used to identify revenue patterns and seasonal trends
     */
    private List<MonthlyRevenue> monthlyBreakdown;
}
