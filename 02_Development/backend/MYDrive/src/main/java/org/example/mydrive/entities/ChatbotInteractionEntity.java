package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotInteractionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id") // Null if interaction is not part of a formal ticket
    private SupportTicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message; // User's message

    @Column(columnDefinition = "TEXT", nullable = false)
    private String response; // AI's response

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean isUserMessage;
}
