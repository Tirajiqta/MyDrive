package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import org.example.mydrive.dto.SupportTicketCreateRequest;
import org.example.mydrive.dto.SupportTicketResponse;
import org.example.mydrive.dto.UserResponse;
import org.example.mydrive.entities.SupportTicketEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.mappers.UserMapper;
import org.example.mydrive.repositories.SupportTicketRepository;
import org.example.mydrive.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock private SupportTicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private SupportTicketService supportTicketService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("u@example.com");
        lenient().when(userMapper.toDto(any(UserEntity.class)))
                .thenReturn(new UserResponse(1L, "u", "u@example.com", null, null, 0L, null, null));
    }

    @Test
    void create_usesProvidedValidPriorityUppercased() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportTicketResponse res = supportTicketService.create(1L,
                new SupportTicketCreateRequest("Subject", "Body", "high"));

        ArgumentCaptor<SupportTicketEntity> captor = ArgumentCaptor.forClass(SupportTicketEntity.class);
        verify(ticketRepository).save(captor.capture());
        SupportTicketEntity saved = captor.getValue();
        assertThat(saved.getPriority()).isEqualTo("HIGH");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.getSubject()).isEqualTo("Subject");
        assertThat(res.status()).isEqualTo("OPEN");
    }

    @Test
    void create_defaultsToMedium_whenPriorityNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        supportTicketService.create(1L, new SupportTicketCreateRequest("S", "D", null));

        ArgumentCaptor<SupportTicketEntity> captor = ArgumentCaptor.forClass(SupportTicketEntity.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    void create_defaultsToMedium_whenPriorityInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        supportTicketService.create(1L, new SupportTicketCreateRequest("S", "D", "URGENT"));

        ArgumentCaptor<SupportTicketEntity> captor = ArgumentCaptor.forClass(SupportTicketEntity.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    void create_whenUserMissing_throwsEntityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportTicketService.create(1L,
                new SupportTicketCreateRequest("S", "D", "LOW")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listForUser_mapsAllTickets() {
        SupportTicketEntity t1 = SupportTicketEntity.builder().id(1L).user(user).subject("a").status("OPEN").priority("LOW").build();
        SupportTicketEntity t2 = SupportTicketEntity.builder().id(2L).user(user).subject("b").status("OPEN").priority("HIGH").build();
        when(ticketRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(t1, t2));

        List<SupportTicketResponse> res = supportTicketService.listForUser(1L);

        assertThat(res).extracting(SupportTicketResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void get_returnsTicket_whenOwnedByUser() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(3L).user(user).subject("s").status("OPEN").priority("LOW").build();
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));

        SupportTicketResponse res = supportTicketService.get(1L, 3L);

        assertThat(res.id()).isEqualTo(3L);
    }

    @Test
    void get_whenTicketMissing_throwsNotFound() {
        when(ticketRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportTicketService.get(1L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void get_whenTicketBelongsToAnotherUser_throwsForbidden() {
        UserEntity other = new UserEntity();
        other.setId(2L);
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(3L).user(other).subject("s").status("OPEN").priority("LOW").build();
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> supportTicketService.get(1L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
