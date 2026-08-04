package com.example.admin_service.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AdminBroadcastRequest {
    private String title;
    private String message;
    private String targetRole; // ALL, LEARNER, TRAINER, ADMIN
    private List<String> channels;
    private boolean urgent;
}
