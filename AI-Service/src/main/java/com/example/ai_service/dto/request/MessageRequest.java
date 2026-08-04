package com.example.ai_service.dto.request;

import com.example.ai_service.enums.BotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRequest {

    @NotBlank(message = "Conversation Id is required")
    private String conversationId;

    @NotBlank(message = "UserID Id is required")
    private String userId;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Bot Type is required")
    private BotType botType;

}