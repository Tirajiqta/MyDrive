package org.example.mydrive;

import org.example.mydrive.dtos.ChatMessage;
import org.example.mydrive.dtos.ChatRequest;
import org.example.mydrive.services.OpenAIService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatCоntroller {
    private final OpenAIService openAIService;

    public ChatCоntroller(OpenAIService service) {
        this.openAIService = service;
    }

    @PostMapping
    public Mono<String> chat(@RequestBody String userMessage) {

        ChatRequest request = new ChatRequest(
                "gpt-4o-mini",
                List.of(
                        new ChatMessage("system", "You are a helpful assistant."),
                        new ChatMessage("user", userMessage)
                )
        );

        return openAIService.sendMessage(request)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }
}
