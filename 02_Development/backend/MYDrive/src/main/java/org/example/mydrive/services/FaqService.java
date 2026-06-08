package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.*;
import org.example.mydrive.entities.FaqEntity;
import org.example.mydrive.entities.FaqTranslationEntity;
import org.example.mydrive.entities.LanguageEntity;
import org.example.mydrive.repositories.FaqRepository;
import org.example.mydrive.repositories.LanguageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FaqService {

    private static final String DEFAULT_LANG = "en";

    private final FaqRepository faqRepository;
    private final LanguageRepository languageRepository;

    /** Returns every FAQ with its text resolved for the requested locale. */
    public List<FaqResponse> list(String langCode) {
        String locale = (langCode == null || langCode.isBlank()) ? DEFAULT_LANG : langCode;
        return faqRepository.findAll().stream()
                .sorted(Comparator.comparing(FaqEntity::getId))
                .map(faq -> toResponse(faq, locale))
                .toList();
    }

    @Transactional
    public FaqResponse create(FaqCreateRequest req) {
        if (faqRepository.findByInternalQuestionKey(req.internalQuestionKey()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "FAQ key already exists: " + req.internalQuestionKey());
        }

        FaqEntity faq = FaqEntity.builder()
                .internalQuestionKey(req.internalQuestionKey())
                .build();

        Set<FaqTranslationEntity> translations = new HashSet<>();
        for (FaqTranslationRequest tr : req.translations()) {
            LanguageEntity lang = languageRepository.findById(tr.languageId())
                    .orElseThrow(() -> new EntityNotFoundException("Language not found: " + tr.languageId()));
            translations.add(FaqTranslationEntity.builder()
                    .faq(faq)
                    .languageEntity(lang)
                    .question(tr.question())
                    .answer(tr.answer())
                    .category(tr.category())
                    .keywords(tr.keywords())
                    .build());
        }
        faq.setTranslations(translations);

        return toResponse(faqRepository.save(faq), DEFAULT_LANG);
    }

    private FaqResponse toResponse(FaqEntity faq, String locale) {
        Set<FaqTranslationEntity> translations = faq.getTranslations();
        FaqTranslationEntity chosen = pickTranslation(translations, locale);

        List<FaqTranslationResponse> all = translations == null ? List.of()
                : translations.stream().map(this::toTranslationResponse).toList();

        return new FaqResponse(
                faq.getId(),
                faq.getInternalQuestionKey(),
                chosen != null ? chosen.getQuestion() : null,
                chosen != null ? chosen.getAnswer() : null,
                chosen != null ? chosen.getCategory() : null,
                chosen != null ? chosen.getKeywords() : null,
                faq.getCreatedAt(),
                faq.getUpdatedAt(),
                all
        );
    }

    /** Prefer the requested locale, fall back to English, then any translation. */
    private FaqTranslationEntity pickTranslation(Set<FaqTranslationEntity> translations, String locale) {
        if (translations == null || translations.isEmpty()) return null;
        return translations.stream()
                .filter(t -> locale.equalsIgnoreCase(langCode(t)))
                .findFirst()
                .or(() -> translations.stream()
                        .filter(t -> DEFAULT_LANG.equalsIgnoreCase(langCode(t)))
                        .findFirst())
                .orElse(translations.iterator().next());
    }

    private String langCode(FaqTranslationEntity t) {
        return t.getLanguageEntity() != null ? t.getLanguageEntity().getCode() : null;
    }

    private FaqTranslationResponse toTranslationResponse(FaqTranslationEntity t) {
        LanguageEntity lang = t.getLanguageEntity();
        LanguageResponse langResponse = lang == null ? null
                : new LanguageResponse(lang.getId(), lang.getCode(), lang.getName(), lang.getIsActive());
        return new FaqTranslationResponse(
                t.getId(),
                langResponse,
                t.getQuestion(),
                t.getAnswer(),
                t.getCategory(),
                t.getKeywords()
        );
    }
}
