package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import org.example.mydrive.dto.FaqCreateRequest;
import org.example.mydrive.dto.FaqResponse;
import org.example.mydrive.dto.FaqTranslationRequest;
import org.example.mydrive.entities.FaqEntity;
import org.example.mydrive.entities.FaqTranslationEntity;
import org.example.mydrive.entities.LanguageEntity;
import org.example.mydrive.repositories.FaqRepository;
import org.example.mydrive.repositories.LanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock private FaqRepository faqRepository;
    @Mock private LanguageRepository languageRepository;

    @InjectMocks private FaqService faqService;

    private LanguageEntity lang(Long id, String code) {
        return LanguageEntity.builder().id(id).code(code).name(code).isActive(true).build();
    }

    private FaqEntity faqWithTranslations(Long id, FaqTranslationEntity... translations) {
        Set<FaqTranslationEntity> set = new LinkedHashSet<>(List.of(translations));
        return FaqEntity.builder().id(id).internalQuestionKey("KEY_" + id).translations(set).build();
    }

    private FaqTranslationEntity translation(String code, String question) {
        return FaqTranslationEntity.builder()
                .languageEntity(lang(code.equals("en") ? 1L : 2L, code))
                .question(question)
                .answer("ans-" + question)
                .build();
    }

    @Test
    void list_resolvesRequestedLocale() {
        FaqEntity faq = faqWithTranslations(1L,
                translation("en", "Hello"),
                translation("bg", "Здравей"));
        when(faqRepository.findAll()).thenReturn(List.of(faq));

        List<FaqResponse> res = faqService.list("bg");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).translatedQuestion()).isEqualTo("Здравей");
        assertThat(res.get(0).allTranslations()).hasSize(2);
    }

    @Test
    void list_fallsBackToEnglish_whenLocaleMissing() {
        FaqEntity faq = faqWithTranslations(1L,
                translation("en", "Hello"),
                translation("bg", "Здравей"));
        when(faqRepository.findAll()).thenReturn(List.of(faq));

        List<FaqResponse> res = faqService.list("fr"); // no French translation

        assertThat(res.get(0).translatedQuestion()).isEqualTo("Hello");
    }

    @Test
    void list_nullLangCode_defaultsToEnglish() {
        FaqEntity faq = faqWithTranslations(1L, translation("en", "Hello"));
        when(faqRepository.findAll()).thenReturn(List.of(faq));

        List<FaqResponse> res = faqService.list(null);

        assertThat(res.get(0).translatedQuestion()).isEqualTo("Hello");
    }

    @Test
    void list_sortsById() {
        FaqEntity f2 = faqWithTranslations(2L, translation("en", "Two"));
        FaqEntity f1 = faqWithTranslations(1L, translation("en", "One"));
        when(faqRepository.findAll()).thenReturn(List.of(f2, f1));

        List<FaqResponse> res = faqService.list("en");

        assertThat(res).extracting(FaqResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void create_persistsFaqWithTranslations() {
        when(faqRepository.findByInternalQuestionKey("NEW_KEY")).thenReturn(Optional.empty());
        when(languageRepository.findById(1L)).thenReturn(Optional.of(lang(1L, "en")));
        when(faqRepository.save(any(FaqEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FaqResponse res = faqService.create(new FaqCreateRequest("NEW_KEY",
                List.of(new FaqTranslationRequest(1L, "Q", "A", "cat", "kw"))));

        assertThat(res.internalQuestionKey()).isEqualTo("NEW_KEY");
        assertThat(res.translatedQuestion()).isEqualTo("Q");
        verify(faqRepository).save(any(FaqEntity.class));
    }

    @Test
    void create_whenKeyExists_throwsConflict() {
        when(faqRepository.findByInternalQuestionKey("DUP"))
                .thenReturn(Optional.of(FaqEntity.builder().id(1L).internalQuestionKey("DUP").build()));

        assertThatThrownBy(() -> faqService.create(new FaqCreateRequest("DUP",
                List.of(new FaqTranslationRequest(1L, "Q", "A", null, null)))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void create_whenLanguageUnknown_throwsEntityNotFound() {
        when(faqRepository.findByInternalQuestionKey("KEY")).thenReturn(Optional.empty());
        when(languageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faqService.create(new FaqCreateRequest("KEY",
                List.of(new FaqTranslationRequest(99L, "Q", "A", null, null)))))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
