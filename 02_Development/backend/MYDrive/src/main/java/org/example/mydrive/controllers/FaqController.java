package org.example.mydrive.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FaqCreateRequest;
import org.example.mydrive.dto.FaqResponse;
import org.example.mydrive.services.FaqService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {
    private final FaqService faqService;

    @GetMapping
    public List<FaqResponse> list(@RequestParam(required = false) String lang) {
        return faqService.list(lang);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaqResponse create(@Valid @RequestBody FaqCreateRequest req) {
        return faqService.create(req);
    }
}
