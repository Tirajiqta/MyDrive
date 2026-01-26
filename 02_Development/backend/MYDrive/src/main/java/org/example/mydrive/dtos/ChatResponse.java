package org.example.mydrive.dtos;

import java.util.List;

public class ChatResponse {

    public static class Choice {
        private int index;
        private ChatMessage message;

        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }
    }

    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
}
