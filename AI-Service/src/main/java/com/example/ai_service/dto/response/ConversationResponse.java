package com.example.ai_service.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConversationResponse {

    private String conversationId;
    private String userId;
    private String botType;
    private String sessionId;
    private String conversationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;
}