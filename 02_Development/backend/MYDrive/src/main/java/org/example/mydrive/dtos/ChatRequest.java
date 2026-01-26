package org.example.mydrive.dtos;

import java.util.List;

public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;

    public ChatRequest() {}

    public ChatRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() { return model; }
    public List<ChatMessage> getMessages() { return messages; }
}
