package com.example.ai_service.controller;

import com.example.ai_service.dto.request.ConversationRequest;
import com.example.ai_service.dto.response.ConversationResponse;
import com.example.ai_service.exceptions.ConversationServiceException;
import com.example.ai_service.service.ConversationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createConversation(@Valid @RequestBody ConversationRequest request,
                                                     @RequestHeader("Authorization") String token){
        try {
            conversationService.createConversation(request, token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Conversation Created Successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for conversation creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ConversationServiceException e) {
            log.error("Service error while creating conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create conversation");
        }
    }

    @GetMapping("/get")
    public ResponseEntity<ConversationResponse> getConversation(@RequestParam String conversationId,
                                                                @RequestHeader("Authorization") String token){
        log.info("Getting Conversation with id: {}", conversationId);
        return ResponseEntity.ok(conversationService.getConversationById(conversationId, token));
    }

    @GetMapping("/get/all")
    public ResponseEntity<List<ConversationResponse>> getAllConversation(@RequestHeader("Authorization") String token){
        log.info("Getting all conversation of user...");
        return ResponseEntity.ok(conversationService.getAllConversation(token));
    }

    @DeleteMapping("/delete/{conversation_id}")
    public ResponseEntity<String> deleteConversation(@PathVariable("conversation_id") String conversationId,
                                                     @RequestHeader("Authorization") String token){
        log.info("Deleting conversation...");
        try {
            conversationService.deleteConversation(conversationId, token);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Conversation Deleted Successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for conversation deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ConversationServiceException e) {
            log.error("Service error while deletion conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete conversation");
        }
    }



}
