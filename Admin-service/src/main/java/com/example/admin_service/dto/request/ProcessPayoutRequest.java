package com.example.admin_service.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPayoutRequest {
    private String payoutId;
    private String action;
    private String remarks;
}