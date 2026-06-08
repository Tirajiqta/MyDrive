package org.example.mydrive.config;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.entities.FaqEntity;
import org.example.mydrive.entities.FaqTranslationEntity;
import org.example.mydrive.entities.LanguageEntity;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.repositories.FaqRepository;
import org.example.mydrive.repositories.LanguageRepository;
import org.example.mydrive.repositories.PlanRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final LanguageRepository languageRepository;
    private final FaqRepository faqRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Plans must stay in sync with the frontend pricing table (lib/plans.ts).
        seedPlan("FREE_PLAN", PlanEntity.PlanType.FREE, 15, BigDecimal.ZERO, "EUR");
        seedPlan("PRO_PLAN", PlanEntity.PlanType.PAID, 100, new BigDecimal("4.99"), "EUR");
        seedPlan("BUSINESS_PLAN", PlanEntity.PlanType.PAID, 1024, new BigDecimal("9.99"), "EUR");

        LanguageEntity en = seedLanguage("en", "English");
        LanguageEntity bg = seedLanguage("bg", "Български");

        seedFaqs(en, bg);
    }

    private void seedPlan(String internalName, PlanEntity.PlanType type, int storageGb,
                          BigDecimal pricePerMonth, String currency) {
        if (planRepository.findByInternalNameAndIsActiveTrue(internalName).isPresent()) {
            return;
        }
        planRepository.save(PlanEntity.builder()
                .internalName(internalName)
                .type(type)
                .storageLimitGB(storageGb)
                .pricePerMonth(pricePerMonth)
                .currency(currency)
                .isActive(true)
                .build());
    }

    private LanguageEntity seedLanguage(String code, String name) {
        return languageRepository.findByCode(code).orElseGet(() ->
                languageRepository.save(LanguageEntity.builder()
                        .code(code)
                        .name(name)
                        .isActive(true)
                        .build()));
    }

    private void seedFaqs(LanguageEntity en, LanguageEntity bg) {
        seedFaq("HOW_TO_UPLOAD", "General",
                en, "How do I upload a file?",
                "Open a folder in My Drive and click the Upload button, or drag and drop files onto the page.",
                bg, "Как да кача файл?",
                "Отворете папка в My Drive и натиснете бутона за качване или плъзнете файловете върху страницата.");
        seedFaq("HOW_TO_SHARE", "Sharing",
                en, "How do I share a file or folder?",
                "Select an item, choose Share, pick a permission level and copy the generated link.",
                bg, "Как да споделя файл или папка?",
                "Изберете елемент, натиснете Споделяне, изберете ниво на достъп и копирайте генерирания линк.");
        seedFaq("STORAGE_LIMIT", "Billing",
                en, "How much storage do I get?",
                "The Free plan includes 15 GB. Upgrade to Pro (100 GB) or Business (1 TB) from the Subscription page.",
                bg, "Колко място за съхранение получавам?",
                "Безплатният план включва 15 GB. Надстройте до Pro (100 GB) или Business (1 TB) от страницата Абонамент.");
    }

    private void seedFaq(String key, String category,
                         LanguageEntity en, String enQ, String enA,
                         LanguageEntity bg, String bgQ, String bgA) {
        if (faqRepository.findByInternalQuestionKey(key).isPresent()) {
            return;
        }
        FaqEntity faq = FaqEntity.builder().internalQuestionKey(key).build();
        faq.setTranslations(Set.of(
                FaqTranslationEntity.builder()
                        .faq(faq).languageEntity(en).category(category)
                        .question(enQ).answer(enA).build(),
                FaqTranslationEntity.builder()
                        .faq(faq).languageEntity(bg).category(category)
                        .question(bgQ).answer(bgA).build()
        ));
        faqRepository.save(faq);
    }
}
