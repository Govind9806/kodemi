package com.example.ai_service.dto.response;

import com.example.ai_service.enums.BotType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class MessageResponse {

    private String messageId;
    private String userId;
    private String conversationId;
    private String content;
    private BotType botType;
    private LocalDateTime createdAt;
}