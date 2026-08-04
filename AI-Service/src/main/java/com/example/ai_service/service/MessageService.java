package com.example.ai_service.service;

import com.example.ai_service.dto.request.MessageRequest;
import com.example.ai_service.dto.response.MessageResponse;
import com.example.ai_service.exceptions.*;
import com.example.ai_service.model.Messages;
import com.example.ai_service.repository.MessageRepository;
import com.example.ai_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class MessageService {
    private static final String MESSAGE_NULL_MSG = "Message Cannot be Null";
    private static final String CREATE_FAILED_MSG = "Failed to create Message. Please try again.";
    private static final String GET_FAILED_MSG = "Cannot get Message.";
    private static final String UNAUTHORIZED_MSG = "You're Not Authorized to Access the Conversation";

    private final JwtUtil jwtUtil;
    private final MessageRepository messageRepository;

    public MessageService(JwtUtil jwtUtil, MessageRepository messageRepository) {
        this.jwtUtil = jwtUtil;
        this.messageRepository = messageRepository;
    }
    private Messages buildMessage(MessageRequest request) {

        Messages messages = new Messages();

        messages.setMessageId(UUID.randomUUID().toString());
        // Store as ISO-8601 String — required because @DynamoDBTypeConverted
        // conflicts with @DynamoDBIndexRangeKey and causes a DynamoDB
        // ValidationException ("HASH key attributes createdAt must have equality conditions").
        messages.setCreatedAt(
                LocalDateTime.now(java.time.ZoneId.systemDefault()).toString()
        );

        toEntity(messages, request);

        try {
            ObjectMapper mapper = new ObjectMapper();

            String jsonContent =
                    mapper.writeValueAsString(request.getContent());

            messages.setContent(jsonContent);

        } catch (Exception e) {
            throw new MessageServiceException(CREATE_FAILED_MSG);
        }

        return messages;
    }

    public void createMessage(MessageRequest request, String token) {

        Objects.requireNonNull(request, MESSAGE_NULL_MSG);

        if(!Objects.equals(
                jwtUtil.extractUserId(token),
                request.getUserId())) {

            throw new UnAuthorizedRequestException(UNAUTHORIZED_MSG);
        }

        messageRepository.save(buildMessage(request));
    }


    public void createDBMessage(MessageRequest request) {

        Objects.requireNonNull(request, MESSAGE_NULL_MSG);

        messageRepository.save(buildMessage(request));
    }

    public MessageResponse getMessageById(String messageId, String token){
        String userId = jwtUtil.extractUserId(token);
        try {
            Messages messages = messageRepository.findById(messageId);
            if(messages == null){
                throw new MessagesNotFound(GET_FAILED_MSG + "With messages_id " + messageId);
            }
            if(!Objects.equals(messages.getUserId(), userId)){
                throw new InvalidRequest(UNAUTHORIZED_MSG);
            }
            MessageResponse response = new MessageResponse();
            toResponse(response, messages);
            return response;
        }catch (Exception e){
            throw new MessageServiceException(GET_FAILED_MSG);
        }
    }
    public List<MessageResponse> getAllMessage(String conversationId, String token){
        String userId = jwtUtil.extractUserId(token);
        List<MessageResponse> responses = new ArrayList<>();
        for(Messages messages : messageRepository.findAllMessagesByConversation(conversationId)){
            MessageResponse response = new MessageResponse();
            if(!Objects.equals(messages.getUserId(), userId)){
                throw new InvalidRequest(UNAUTHORIZED_MSG);
            }
            toResponse(response, messages);
            responses.add(response);
        }
        return responses;
    }
    public void deleteMessage(String messageId, String token){
        String userId = jwtUtil.extractUserId(token);
        Messages messages = messageRepository.findById(messageId);
        if(messages == null){
            throw new ConversationNotFound(GET_FAILED_MSG + "With messageId " + messageId);
        }
        if(!Objects.equals(messages.getUserId(), userId)){
            throw new InvalidRequest(UNAUTHORIZED_MSG);
        }
        messageRepository.deleteMessage(messages);
        log.info("Message with id {} is Deleted Successfully.", messageId);
    }

    public void toEntity(Messages message, MessageRequest request) {
        message.setConversationId(request.getConversationId());
        message.setContent(request.getContent());
        message.setBotType(request.getBotType());
        message.setUserId(request.getUserId());
    }

    // Entity → Response
    public void toResponse(MessageResponse response, Messages entity) {
        response.setMessageId(entity.getMessageId());
        response.setUserId(entity.getUserId());
        response.setConversationId(entity.getConversationId());
        response.setContent(entity.getContent());
        response.setBotType(entity.getBotType());
        // Parse the ISO-8601 String back to LocalDateTime for the response
        if (entity.getCreatedAt() != null) {
            try {
                response.setCreatedAt(LocalDateTime.parse(entity.getCreatedAt()));
            } catch (Exception e) {
                log.warn("Failed to parse createdAt '{}' for messageId {}", entity.getCreatedAt(), entity.getMessageId());
            }
        }
    }
}
