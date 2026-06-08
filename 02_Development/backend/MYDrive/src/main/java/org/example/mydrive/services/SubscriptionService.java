package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.PlanResponse;
import org.example.mydrive.dto.UserSubscriptionResponse;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.entities.UserSubscriptionEntity;
import org.example.mydrive.mappers.UserMapper;
import org.example.mydrive.repositories.PlanRepository;
import org.example.mydrive.repositories.UserRepository;
import org.example.mydrive.repositories.UserSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserMapper userMapper;

    /** Maps frontend plan ids (lib/plans.ts) to backend internal plan names. */
    private static final Map<String, String> PLAN_ALIASES = Map.of(
            "free", "FREE_PLAN",
            "pro", "PRO_PLAN",
            "business", "BUSINESS_PLAN"
    );

    public List<PlanResponse> listActivePlans() {
        return planRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(this::toPlanResponse)
                .toList();
    }

    public UserSubscriptionResponse getCurrentSubscription(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        UserSubscriptionEntity sub = user.getActiveSubscription();
        if (sub == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active subscription");
        }
        return toSubscriptionResponse(sub, user);
    }

    /**
     * Activates (or upgrades) the user's subscription to the requested plan.
     * Called after Stripe Checkout reports a completed payment. The existing
     * one-to-one subscription row is reused so the user never ends up with more
     * than one active subscription.
     */
    @Transactional
    public UserSubscriptionResponse activatePlan(Long userId, String planRef, String stripeSessionId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String internalName = PLAN_ALIASES.getOrDefault(planRef.toLowerCase(), planRef);
        PlanEntity plan = planRepository.findByInternalNameAndIsActiveTrue(internalName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown or inactive plan: " + planRef));

        UserSubscriptionEntity sub = user.getActiveSubscription();
        if (sub == null) {
            sub = new UserSubscriptionEntity();
            sub.setUser(user);
        }
        sub.setPlanEntity(plan);
        sub.setStatus("ACTIVE");
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(null);
        sub.setRenewalDate(LocalDateTime.now().plusMonths(1));
        sub.setPaymentMethodId(stripeSessionId);

        UserSubscriptionEntity saved = userSubscriptionRepository.save(sub);
        user.setActiveSubscription(saved);
        userRepository.save(user);

        return toSubscriptionResponse(saved, user);
    }

    private UserSubscriptionResponse toSubscriptionResponse(UserSubscriptionEntity sub, UserEntity user) {
        return new UserSubscriptionResponse(
                sub.getId(),
                userMapper.toDto(user),
                toPlanResponse(sub.getPlanEntity()),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.getStatus(),
                sub.getRenewalDate(),
                sub.getCreatedAt(),
                sub.getUpdatedAt()
        );
    }

    private PlanResponse toPlanResponse(PlanEntity plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getInternalName(),
                plan.getStorageLimitGB(),
                plan.getPricePerMonth(),
                plan.getCurrency(),
                plan.getIsActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                null,
                null
        );
    }
}
