package org.example.mydrive.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.SupportTicketCreateRequest;
import org.example.mydrive.dto.SupportTicketResponse;
import org.example.mydrive.services.SupportTicketService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {
    private final SupportTicketService supportTicketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse create(@Valid @RequestBody SupportTicketCreateRequest req) {
        return supportTicketService.create(GeneralUtils.getIdFromToken(), req);
    }

    @GetMapping
    public List<SupportTicketResponse> list() {
        return supportTicketService.listForUser(GeneralUtils.getIdFromToken());
    }

    @GetMapping("/{id}")
    public SupportTicketResponse get(@PathVariable Long id) {
        return supportTicketService.get(GeneralUtils.getIdFromToken(), id);
    }
}
