package com.example.admin_service.dto.response;

import com.example.admin_service.model.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {
    private List<WalletTransaction> transactions;
    private int totalCount;
}