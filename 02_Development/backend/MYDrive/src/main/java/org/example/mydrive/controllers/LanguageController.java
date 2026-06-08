package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.LanguageResponse;
import org.example.mydrive.repositories.LanguageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
public class LanguageController {
    private final LanguageRepository languageRepository;

    @GetMapping
    public List<LanguageResponse> list() {
        return languageRepository.findAll().stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .map(l -> new LanguageResponse(l.getId(), l.getCode(), l.getName(), l.getIsActive()))
                .toList();
    }
}
