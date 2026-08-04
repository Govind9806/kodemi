package com.example.ai_service.controller;

import com.example.ai_service.dto.request.MessageRequest;
import com.example.ai_service.dto.response.MessageResponse;
import com.example.ai_service.exceptions.MessageServiceException;
import com.example.ai_service.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createMessage(@Valid @RequestBody MessageRequest request,
                                                @RequestHeader("Authorization") String token){
        try {
            messageService.createMessage(request, token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Messages Created Successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for messages creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (MessageServiceException e) {
            log.error("Service error while messages conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create messages");
        }
    }
    @PostMapping("/db/create")
    public ResponseEntity<String> createDBMessage(@Valid @RequestBody MessageRequest request){
        try {
            messageService.createDBMessage(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Messages Created Successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for messages creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (MessageServiceException e) {
            log.error("Service error while messages conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create messages");
        }
    }

    @GetMapping("/get")
    public ResponseEntity<MessageResponse> getMessage(@RequestParam String messageId,
                                                      @RequestHeader("Authorization") String token){
        log.info("Messages loading for messageId: {}", messageId);
        return ResponseEntity.ok(messageService.getMessageById(messageId, token));
    }

    @GetMapping("/get/all")
    public  ResponseEntity<List<MessageResponse>> getAllChats(@RequestParam String conversationId,
                                                              @RequestHeader("Authorization") String token){
        log.info("Messages for a ConversationId: {}", conversationId);
        return ResponseEntity.ok(messageService.getAllMessage(conversationId, token));
    }
}
