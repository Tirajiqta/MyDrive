package org.example.mydrive.services;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ChatRequest;
import org.example.mydrive.entities.ChatbotInteractionEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.ChatbotInteractionRepository;
import org.example.mydrive.repositories.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAIService openAIService;
    private final ChatbotInteractionRepository interactionRepository;
    private final UserRepository userRepository;

    /**
     * Sends the user's message to the model and persists the exchange as a
     * {@link ChatbotInteractionEntity} once a reply is received. Persistence runs
     * on a bounded-elastic scheduler so the blocking JPA call never touches a
     * Netty event-loop thread.
     */
    public Mono<String> chat(Long userId, String userMessage, ChatRequest request) {
        return openAIService.sendMessage(request)
                .map(response -> response.getChoices().get(0).getMessage().getContent())
                .flatMap(reply -> Mono.fromCallable(() -> {
                            saveInteraction(userId, userMessage, reply);
                            return reply;
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private void saveInteraction(Long userId, String message, String reply) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return; // Don't fail the reply if the user vanished mid-request.

        ChatbotInteractionEntity interaction = ChatbotInteractionEntity.builder()
                .user(user)
                .message(message)
                .response(reply)
                .isUserMessage(true)
                .build();
        interactionRepository.save(interaction);
    }
}
