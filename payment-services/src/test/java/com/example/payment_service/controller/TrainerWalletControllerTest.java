package com.example.payment_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.payment_service.dto.response.TrainerRevenueResponse;
import com.example.payment_service.service.WalletService;
import com.example.payment_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@WebMvcTest(TrainerWalletController.class)
public class TrainerWalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String validToken = "Bearer valid_jwt_token";
    private String trainerId = "trainer-001";

    @BeforeEach
    public void setUp() {
        // Mock JWT extraction
        when(jwtUtil.extractUserId(validToken)).thenReturn(trainerId);
    }

    // ============ TEST: Get Trainer Revenue Analytics ============

    @Test
    public void testGetTrainerRevenueAnalytics_Success() throws Exception {
        // Arrange
        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(45L)
                .totalResourcesSold(28L)
                .totalTransactions(73L)
                .totalRevenue(new BigDecimal("45000.00"))
                .balance(new BigDecimal("25000.00"))
                .pendingPayout(new BigDecimal("10000.00"))
                .totalWithdrawn(new BigDecimal("10000.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").value(trainerId))
                .andExpect(jsonPath("$.totalCoursesSold").value(45))
                .andExpect(jsonPath("$.totalResourcesSold").value(28))
                .andExpect(jsonPath("$.totalTransactions").value(73))
                .andExpect(jsonPath("$.totalRevenue").value(45000.00))
                .andExpect(jsonPath("$.balance").value(25000.00))
                .andExpect(jsonPath("$.pendingPayout").value(10000.00));

        verify(walletService, times(1)).getTrainerRevenueAnalytics(trainerId);
    }

    @Test
    public void testGetTrainerRevenueAnalytics_WithCourseBreakdown() throws Exception {
        // Arrange
        List<TrainerRevenueResponse.RevenueByCourse> courseRevenue = new ArrayList<>();
        courseRevenue.add(TrainerRevenueResponse.RevenueByCourse.builder()
                .courseId("course-001")
                .courseName("Course course-001")
                .unitsSold(15L)
                .revenue(new BigDecimal("15000.00"))
                .build());

        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(15L)
                .totalResourcesSold(0L)
                .totalTransactions(15L)
                .totalRevenue(new BigDecimal("15000.00"))
                .balance(new BigDecimal("15000.00"))
                .pendingPayout(new BigDecimal("0.00"))
                .totalWithdrawn(new BigDecimal("0.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(courseRevenue)
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseRevenue[0].courseId").value("course-001"))
                .andExpect(jsonPath("$.courseRevenue[0].unitsSold").value(15))
                .andExpect(jsonPath("$.courseRevenue[0].revenue").value(15000.00));
    }

    @Test
    public void testGetTrainerRevenueAnalytics_WithResourceBreakdown() throws Exception {
        // Arrange
        List<TrainerRevenueResponse.RevenueByResource> resourceRevenue = new ArrayList<>();
        resourceRevenue.add(TrainerRevenueResponse.RevenueByResource.builder()
                .resourceId("resource-001")
                .resourceName("Resource resource-001")
                .unitsSold(20L)
                .revenue(new BigDecimal("8000.00"))
                .build());

        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(0L)
                .totalResourcesSold(20L)
                .totalTransactions(20L)
                .totalRevenue(new BigDecimal("8000.00"))
                .balance(new BigDecimal("8000.00"))
                .pendingPayout(new BigDecimal("0.00"))
                .totalWithdrawn(new BigDecimal("0.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(resourceRevenue)
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceRevenue[0].resourceId").value("resource-001"))
                .andExpect(jsonPath("$.resourceRevenue[0].unitsSold").value(20))
                .andExpect(jsonPath("$.resourceRevenue[0].revenue").value(8000.00));
    }

    @Test
    public void testGetTrainerRevenueAnalytics_WithMonthlyBreakdown() throws Exception {
        // Arrange
        List<TrainerRevenueResponse.MonthlyRevenue> monthlyBreakdown = new ArrayList<>();
        monthlyBreakdown.add(TrainerRevenueResponse.MonthlyRevenue.builder()
                .month("2026-07")
                .amount(new BigDecimal("12000.00"))
                .transactionCount(15L)
                .build());
        monthlyBreakdown.add(TrainerRevenueResponse.MonthlyRevenue.builder()
                .month("2026-06")
                .amount(new BigDecimal("8000.00"))
                .transactionCount(10L)
                .build());

        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(25L)
                .totalResourcesSold(0L)
                .totalTransactions(25L)
                .totalRevenue(new BigDecimal("20000.00"))
                .balance(new BigDecimal("20000.00"))
                .pendingPayout(new BigDecimal("0.00"))
                .totalWithdrawn(new BigDecimal("0.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(monthlyBreakdown)
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value("2026-07"))
                .andExpect(jsonPath("$.monthlyBreakdown[0].amount").value(12000.00))
                .andExpect(jsonPath("$.monthlyBreakdown[0].transactionCount").value(15));
    }

    @Test
    public void testGetTrainerRevenueAnalytics_NoSales() throws Exception {
        // Arrange - Trainer with no sales
        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(0L)
                .totalResourcesSold(0L)
                .totalTransactions(0L)
                .totalRevenue(new BigDecimal("0.00"))
                .balance(new BigDecimal("0.00"))
                .pendingPayout(new BigDecimal("0.00"))
                .totalWithdrawn(new BigDecimal("0.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCoursesSold").value(0))
                .andExpect(jsonPath("$.totalResourcesSold").value(0))
                .andExpect(jsonPath("$.totalRevenue").value(0.00));
    }

    @Test
    public void testGetTrainerRevenueAnalytics_MissingToken() throws Exception {
        // Act & Assert - No Authorization header
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetTrainerRevenueAnalytics_InvalidToken() throws Exception {
        // Arrange
        String invalidToken = "Bearer invalid_token";
        when(jwtUtil.extractUserId(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetTrainerRevenueAnalytics_DataConsistency() throws Exception {
        // Arrange - Verify formula: balance + pendingPayout + totalWithdrawn = totalRevenue
        BigDecimal totalRevenue = new BigDecimal("50000.00");
        BigDecimal balance = new BigDecimal("30000.00");
        BigDecimal pendingPayout = new BigDecimal("10000.00");
        BigDecimal totalWithdrawn = new BigDecimal("10000.00");

        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(100L)
                .totalResourcesSold(50L)
                .totalTransactions(150L)
                .totalRevenue(totalRevenue)
                .balance(balance)
                .pendingPayout(pendingPayout)
                .totalWithdrawn(totalWithdrawn)
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(50000.00))
                .andExpect(jsonPath("$.balance").value(30000.00))
                .andExpect(jsonPath("$.pendingPayout").value(10000.00))
                .andExpect(jsonPath("$.totalWithdrawn").value(10000.00));

        // Verify formula: balance + pending + withdrawn = totalRevenue
        BigDecimal sum = balance.add(pendingPayout).add(totalWithdrawn);
        assert sum.compareTo(totalRevenue) == 0 : "Data consistency check failed!";
    }

    @Test
    public void testGetTrainerRevenueAnalytics_LargeNumbers() throws Exception {
        // Arrange - Test with large numbers
        TrainerRevenueResponse response = TrainerRevenueResponse.builder()
                .trainerId(trainerId)
                .totalCoursesSold(10000L)
                .totalResourcesSold(5000L)
                .totalTransactions(15000L)
                .totalRevenue(new BigDecimal("500000000.00")) // 5 crore
                .balance(new BigDecimal("300000000.00"))
                .pendingPayout(new BigDecimal("100000000.00"))
                .totalWithdrawn(new BigDecimal("100000000.00"))
                .lastUpdated(LocalDateTime.now())
                .courseRevenue(new ArrayList<>())
                .resourceRevenue(new ArrayList<>())
                .monthlyBreakdown(new ArrayList<>())
                .build();

        when(walletService.getTrainerRevenueAnalytics(trainerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/trainer-wallet/revenue-analytics")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCoursesSold").value(10000))
                .andExpect(jsonPath("$.totalResourcesSold").value(5000))
                .andExpect(jsonPath("$.totalRevenue").value(500000000.00));
    }
}
