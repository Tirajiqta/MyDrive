package org.example.mydrive.controllers;

import org.example.mydrive.dto.ChatMessage;
import org.example.mydrive.dto.ChatRequest;
import org.example.mydrive.services.ChatService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Mono<String> chat(@RequestBody String userMessage) {
        // Capture the user id on the request thread; it is not available on the
        // reactive threads where the response (and persistence) is handled.
        Long userId = GeneralUtils.getIdFromToken();

        ChatRequest request = new ChatRequest(
                "gpt-4o-mini",
                List.of(
                        new ChatMessage("system", "You are a helpful assistant for MyDrive, a cloud storage app."),
                        new ChatMessage("user", userMessage)
                )
        );

        return chatService.chat(userId, userMessage, request);
    }
}
