package org.example.mydrive.services;

import org.example.mydrive.dtos.ChatRequest;
import org.example.mydrive.dtos.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenAIService {
    private final WebClient webClient;

    public OpenAIService(WebClient openAIWebClient) {
        this.webClient = openAIWebClient;
    }

    public Mono<ChatResponse> sendMessage(ChatRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class);
    }
}
