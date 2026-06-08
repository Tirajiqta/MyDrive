package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.SupportTicketCreateRequest;
import org.example.mydrive.dto.SupportTicketResponse;
import org.example.mydrive.entities.SupportTicketEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.mappers.UserMapper;
import org.example.mydrive.repositories.SupportTicketRepository;
import org.example.mydrive.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private static final Set<String> VALID_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public SupportTicketResponse create(Long userId, SupportTicketCreateRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String priority = (req.priority() != null && VALID_PRIORITIES.contains(req.priority().toUpperCase()))
                ? req.priority().toUpperCase()
                : "MEDIUM";

        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .user(user)
                .subject(req.subject())
                .description(req.description())
                .status("OPEN")
                .priority(priority)
                .build();

        return toResponse(ticketRepository.save(ticket));
    }

    public List<SupportTicketResponse> listForUser(Long userId) {
        return ticketRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SupportTicketResponse get(Long userId, Long ticketId) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.getUser() == null || !ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ticket");
        }
        return toResponse(ticket);
    }

    private SupportTicketResponse toResponse(SupportTicketEntity t) {
        return new SupportTicketResponse(
                t.getId(),
                t.getUser() != null ? userMapper.toDto(t.getUser()) : null,
                t.getSubject(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getAssignedAgent() != null ? userMapper.toDto(t.getAssignedAgent()) : null,
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getGithubIssueUrl(),
                t.getGithubIssueId()
        );
    }
}
