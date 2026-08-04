package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConversationRequest {
    @NotBlank(message = "Conversation Id is required")
    private String conversationId;
    @NotBlank(message = "Bot Type is required")
    private String botType;
    @NotBlank(message = "Session Id is required")
    private String sessionId;
    @NotBlank(message = "Conversation name is required")
    private String conversationName;
    @NotNull(message = "Created At is required")
    private LocalDateTime createdAt;
    @NotNull(message = "Updated At is required")
    private LocalDateTime updatedAt;
    @NotNull(message = "Last Message is required")
    private LocalDateTime lastMessageAt;
}
