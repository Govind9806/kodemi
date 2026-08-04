package com.example.ai_service.service;

import com.example.ai_service.dto.request.MessageRequest;
import com.example.ai_service.dto.response.MessageResponse;
import com.example.ai_service.enums.BotType;
import com.example.ai_service.exceptions.*;
import com.example.ai_service.model.Messages;
import com.example.ai_service.repository.MessageRepository;
import com.example.ai_service.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class MessageServiceTest {


    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MessageRepository messageRepository;


    @InjectMocks
    private MessageService messageService;


    private MessageRequest request;
    private Messages message;


    @BeforeEach
    void setup(){

        MockitoAnnotations.openMocks(this);


        request = new MessageRequest();

        request.setUserId("user1");
        request.setConversationId("conv1");
        request.setBotType(BotType.TRAINER);
        request.setContent("Hello");


        message = new Messages();

        message.setMessageId("msg1");
        message.setUserId("user1");
        message.setConversationId("conv1");
        message.setBotType(BotType.LEARNER);
        message.setContent("Hello");
        message.setCreatedAt("2025-07-22T12:00:00");

    }


    @Test
    void createMessage_success(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        assertDoesNotThrow(() ->
                messageService.createMessage(
                        request,
                        "token"
                )
        );


        verify(messageRepository)
                .save(any(Messages.class));
    }


    @Test
    void createMessage_nullRequest(){

        assertThrows(
                NullPointerException.class,
                () -> messageService.createMessage(null,"token")
        );
    }


    @Test
    void createMessage_unauthorized() {

        when(jwtUtil.extractUserId("token"))
                .thenReturn("wrong-user");

        assertThrows(
                UnAuthorizedRequestException.class,
                () -> messageService.createMessage(request, "token")
        );
    }


    @Test
    void createDBMessage_success(){

        assertDoesNotThrow(() ->
                messageService.createDBMessage(request)
        );


        verify(messageRepository)
                .save(any(Messages.class));
    }


    @Test
    void createDBMessage_null(){

        assertThrows(
                NullPointerException.class,
                () ->
                        messageService.createDBMessage(null)
        );
    }



    @Test
    void getMessageById_success(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        when(messageRepository.findById("msg1"))
                .thenReturn(message);


        MessageResponse response =
                messageService.getMessageById(
                        "msg1",
                        "token"
                );


        assertNotNull(response);
        assertEquals(
                "msg1",
                response.getMessageId()
        );
    }



    @Test
    void getMessageById_notFound(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        when(messageRepository.findById("msg1"))
                .thenReturn(null);


        assertThrows(
                MessageServiceException.class,
                () ->
                        messageService.getMessageById(
                                "msg1",
                                "token"
                        )
        );
    }



    @Test
    void getMessageById_invalidUser(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user2");


        when(messageRepository.findById("msg1"))
                .thenReturn(message);


        assertThrows(
                MessageServiceException.class,
                () ->
                        messageService.getMessageById(
                                "msg1",
                                "token"
                        )
        );
    }



    @Test
    void getAllMessage_success(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        when(messageRepository
                .findAllMessagesByConversation("conv1"))
                .thenReturn(List.of(message));


        List<MessageResponse> result =
                messageService.getAllMessage(
                        "conv1",
                        "token"
                );


        assertEquals(1,result.size());
    }



    @Test
    void getAllMessage_invalidUser(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("wrong");


        when(messageRepository
                .findAllMessagesByConversation("conv1"))
                .thenReturn(List.of(message));


        assertThrows(
                InvalidRequest.class,
                () ->
                        messageService.getAllMessage(
                                "conv1",
                                "token"
                        )
        );
    }



    @Test
    void deleteMessage_success(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        when(messageRepository.findById("msg1"))
                .thenReturn(message);


        assertDoesNotThrow(() ->
                messageService.deleteMessage(
                        "msg1",
                        "token"
                )
        );


        verify(messageRepository)
                .deleteMessage(message);
    }



    @Test
    void deleteMessage_notFound(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user1");


        when(messageRepository.findById("msg1"))
                .thenReturn(null);


        assertThrows(
                ConversationNotFound.class,
                () ->
                        messageService.deleteMessage(
                                "msg1",
                                "token"
                        )
        );
    }



    @Test
    void deleteMessage_invalidUser(){

        when(jwtUtil.extractUserId("token"))
                .thenReturn("user2");


        when(messageRepository.findById("msg1"))
                .thenReturn(message);


        assertThrows(
                InvalidRequest.class,
                () ->
                        messageService.deleteMessage(
                                "msg1",
                                "token"
                        )
        );
    }



    @Test
    void toEntity_success(){

        Messages entity = new Messages();

        messageService.toEntity(entity,request);


        assertEquals(
                request.getUserId(),
                entity.getUserId()
        );

        assertEquals(
                request.getConversationId(),
                entity.getConversationId()
        );
    }



    @Test
    void toResponse_success(){

        MessageResponse response =
                new MessageResponse();


        messageService.toResponse(
                response,
                message
        );


        assertEquals(
                "msg1",
                response.getMessageId()
        );
    }
}