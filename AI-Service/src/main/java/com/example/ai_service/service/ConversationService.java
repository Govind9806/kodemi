package com.example.ai_service.service;

import com.example.ai_service.dto.request.ConversationRequest;
import com.example.ai_service.dto.response.ConversationResponse;
import com.example.ai_service.exceptions.ConversationNotFound;
import com.example.ai_service.exceptions.ConversationServiceException;
import com.example.ai_service.exceptions.InvalidRequest;
import com.example.ai_service.exceptions.UnAuthorizedRequestException;
import com.example.ai_service.model.Conversations;
import com.example.ai_service.repository.ConversationRepository;
import com.example.ai_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ConversationService {
    private static final String CONVERSATION_NULL_MSG = "Conversation Cannot be Null";
    private static final String CREATE_FAILED_MSG = "Failed to create Conversation. Please try again.";
    private static final String GET_FAILED_MSG = "Cannot get Conversation.";
    private static final String UNAUTHORIZED_MSG = "You're Not Authorized to Access the Conversation";

    private final JwtUtil jwtUtil;
    private final ConversationRepository conversationRepository;

    public ConversationService(JwtUtil jwtUtil, ConversationRepository conversationRepository) {
        this.jwtUtil = jwtUtil;
        this.conversationRepository = conversationRepository;
    }

    public void createConversation(ConversationRequest request, String token) {
        Objects.requireNonNull(request, CONVERSATION_NULL_MSG);

        String userId = jwtUtil.extractUserId(token);
        try {
            Conversations conversations = new Conversations();
            conversations.setUserId(userId);
            toEntity(conversations, request);
            conversationRepository.save(conversations);
            log.info("Conversation Created Successfully for conversation_id: {}", request.getConversationId());
        } catch (Exception e) {
            throw new ConversationServiceException(CREATE_FAILED_MSG);
        }
    }
    public ConversationResponse getConversationById(String conversationId, String token){
        String userId = jwtUtil.extractUserId(token);
        try {
            Conversations conversations = conversationRepository.getConversationById(conversationId);
            if(conversations == null){
                throw new ConversationNotFound(GET_FAILED_MSG + "With conversation_id " + conversationId);
            }
            if(!Objects.equals(conversations.getUserId(), userId)){
                throw new InvalidRequest(UNAUTHORIZED_MSG);
            }
            ConversationResponse response = new ConversationResponse();
            toResponse(response, conversations);
            return response;
        }catch (Exception e){
            throw new ConversationServiceException(GET_FAILED_MSG);
        }
    }
    public List<ConversationResponse> getAllConversation(String token) {
        try {
            String userId = jwtUtil.extractUserId(token);
            if (userId == null || userId.isBlank()) {
                throw new InvalidRequest("Invalid token: userId is missing");
            }
            List<ConversationResponse> responses = new ArrayList<>();
            List<Conversations> userConversations = conversationRepository.getUserConversations(userId);
            if (userConversations != null) {
                for (Conversations conversations : userConversations) {
                    ConversationResponse response = new ConversationResponse();
                    toResponse(response, conversations);
                    responses.add(response);
                }
            }
            return responses;
        } catch (InvalidRequest | UnAuthorizedRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving conversations for user: {}", e.getMessage(), e);
            throw new ConversationServiceException("Failed to retrieve conversations: " + e.getMessage());
        }
    }
    public void deleteConversation(String conversationId, String token){
        String userId = jwtUtil.extractUserId(token);
        Conversations conversations = conversationRepository.getConversationById(conversationId);
        if(conversations == null){
            throw new ConversationNotFound(GET_FAILED_MSG + "With conversationId " + conversationId);
        }
        if(!Objects.equals(conversations.getUserId(), userId)){
            throw new InvalidRequest(UNAUTHORIZED_MSG);
        }
        conversationRepository.delete(conversations);
        log.info("conversation with id {} is Deleted Successfully.", conversationId);
    }
    public void toEntity(Conversations conversation, ConversationRequest request) {
        conversation.setConversationId(request.getConversationId());
        conversation.setBotType(request.getBotType());
        conversation.setSessionId(request.getSessionId());
        conversation.setConversationName(request.getConversationName());
        conversation.setCreatedAt(request.getCreatedAt());
        conversation.setUpdatedAt(request.getUpdatedAt());
        conversation.setLastMessageAt(request.getLastMessageAt());

    }

    // Entity → Response
    public void toResponse(ConversationResponse response, Conversations entity) {
        response.setConversationId(entity.getConversationId());
        response.setUserId(entity.getUserId());
        response.setBotType(entity.getBotType());
        response.setSessionId(entity.getSessionId());
        response.setConversationName(entity.getConversationName());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setLastMessageAt(entity.getLastMessageAt());

    }

}
