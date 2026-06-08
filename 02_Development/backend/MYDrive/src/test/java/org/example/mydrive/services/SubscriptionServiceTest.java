package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import org.example.mydrive.dto.PlanResponse;
import org.example.mydrive.dto.UserResponse;
import org.example.mydrive.dto.UserSubscriptionResponse;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.entities.UserSubscriptionEntity;
import org.example.mydrive.mappers.UserMapper;
import org.example.mydrive.repositories.PlanRepository;
import org.example.mydrive.repositories.UserRepository;
import org.example.mydrive.repositories.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private PlanRepository planRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserSubscriptionRepository userSubscriptionRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private SubscriptionService subscriptionService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("u@example.com");
        lenient().when(userMapper.toDto(any(UserEntity.class)))
                .thenReturn(new UserResponse(1L, "u", "u@example.com", null, null, 0L, null, null));
    }

    private PlanEntity plan(Long id, String name, boolean active) {
        return PlanEntity.builder()
                .id(id).internalName(name).storageLimitGB(10)
                .pricePerMonth(BigDecimal.ONE).currency("USD").isActive(active)
                .build();
    }

    @Test
    void listActivePlans_returnsOnlyActivePlans() {
        when(planRepository.findAll()).thenReturn(List.of(
                plan(1L, "FREE_PLAN", true),
                plan(2L, "OLD_PLAN", false),
                plan(3L, "PRO_PLAN", true)
        ));

        List<PlanResponse> result = subscriptionService.listActivePlans();

        assertThat(result).extracting(PlanResponse::internalName)
                .containsExactly("FREE_PLAN", "PRO_PLAN");
    }

    @Test
    void getCurrentSubscription_returnsActiveSubscription() {
        UserSubscriptionEntity sub = UserSubscriptionEntity.builder()
                .id(5L).user(user).planEntity(plan(1L, "FREE_PLAN", true)).status("ACTIVE")
                .build();
        user.setActiveSubscription(sub);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSubscriptionResponse res = subscriptionService.getCurrentSubscription(1L);

        assertThat(res.id()).isEqualTo(5L);
        assertThat(res.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getCurrentSubscription_whenUserMissing_throwsEntityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getCurrentSubscription(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCurrentSubscription_whenNoActiveSub_throwsNotFound() {
        user.setActiveSubscription(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> subscriptionService.getCurrentSubscription(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void activatePlan_mapsAliasAndCreatesSubscriptionWhenNoneExists() {
        PlanEntity pro = plan(2L, "PRO_PLAN", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findByInternalNameAndIsActiveTrue("PRO_PLAN")).thenReturn(Optional.of(pro));
        when(userSubscriptionRepository.save(any(UserSubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionResponse res = subscriptionService.activatePlan(1L, "pro", "sess_123");

        assertThat(res.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<UserSubscriptionEntity> captor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(captor.capture());
        UserSubscriptionEntity saved = captor.getValue();
        assertThat(saved.getPlanEntity()).isEqualTo(pro);
        assertThat(saved.getPaymentMethodId()).isEqualTo("sess_123");
        assertThat(saved.getRenewalDate()).isNotNull();
        assertThat(saved.getEndDate()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void activatePlan_reusesExistingSubscriptionRow() {
        PlanEntity pro = plan(2L, "PRO_PLAN", true);
        UserSubscriptionEntity existing = UserSubscriptionEntity.builder()
                .id(9L).user(user).planEntity(plan(1L, "FREE_PLAN", true)).status("ACTIVE")
                .build();
        user.setActiveSubscription(existing);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findByInternalNameAndIsActiveTrue("PRO_PLAN")).thenReturn(Optional.of(pro));
        when(userSubscriptionRepository.save(any(UserSubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.activatePlan(1L, "pro", "sess_456");

        ArgumentCaptor<UserSubscriptionEntity> captor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(captor.capture());
        // same row reused, plan switched
        assertThat(captor.getValue().getId()).isEqualTo(9L);
        assertThat(captor.getValue().getPlanEntity()).isEqualTo(pro);
    }

    @Test
    void activatePlan_passesThroughUnknownAliasAsLiteralInternalName() {
        PlanEntity custom = plan(7L, "CUSTOM_PLAN", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findByInternalNameAndIsActiveTrue("CUSTOM_PLAN")).thenReturn(Optional.of(custom));
        when(userSubscriptionRepository.save(any(UserSubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.activatePlan(1L, "CUSTOM_PLAN", "sess");

        verify(planRepository).findByInternalNameAndIsActiveTrue("CUSTOM_PLAN");
    }

    @Test
    void activatePlan_whenPlanUnknown_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findByInternalNameAndIsActiveTrue("FREE_PLAN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.activatePlan(1L, "free", "sess"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void activatePlan_whenUserMissing_throwsEntityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.activatePlan(1L, "pro", "sess"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
