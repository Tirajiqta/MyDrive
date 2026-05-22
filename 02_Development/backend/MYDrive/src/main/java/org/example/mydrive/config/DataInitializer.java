package org.example.mydrive.config;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.repositories.PlanRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.findByInternalNameAndIsActiveTrue("FREE_PLAN").isEmpty()) {
            PlanEntity freePlan = PlanEntity.builder()
                    .internalName("FREE_PLAN")
                    .type(PlanEntity.PlanType.FREE)
                    .storageLimitGB(15)
                    .pricePerMonth(BigDecimal.ZERO)
                    .currency("USD")
                    .isActive(true)
                    .build();
            planRepository.save(freePlan);
        }
    }
}
