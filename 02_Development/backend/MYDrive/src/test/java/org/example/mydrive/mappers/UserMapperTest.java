package org.example.mydrive.mappers;

import org.example.mydrive.dto.UserResponse;
import org.example.mydrive.entities.LanguageEntity;
import org.example.mydrive.entities.PlanEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.entities.UserSubscriptionEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toDto_mapsBasicFields() {
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime lastLogin = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setCreatedAt(created);
        user.setLastLogin(lastLogin);
        user.setCurrentStorageUsed(1234L);

        UserResponse dto = mapper.toDto(user);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.username()).isEqualTo("john");
        assertThat(dto.email()).isEqualTo("john@example.com");
        assertThat(dto.createdAt()).isEqualTo(created);
        assertThat(dto.lastLogin()).isEqualTo(lastLogin);
        assertThat(dto.currentStorageUsedBytes()).isEqualTo(1234L);
    }

    @Test
    void toDto_nullStorageLimitAndLanguage_whenNoSubscriptionOrLanguage() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setCurrentStorageUsed(0L);

        UserResponse dto = mapper.toDto(user);

        assertThat(dto.storageLimitGB()).isNull();
        assertThat(dto.preferredLanguageCode()).isNull();
    }

    @Test
    void toDto_resolvesStorageLimitFromActivePlan() {
        PlanEntity plan = PlanEntity.builder().id(1L).internalName("PRO").storageLimitGB(100).isActive(true).build();
        UserSubscriptionEntity sub = UserSubscriptionEntity.builder().id(1L).planEntity(plan).build();
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setCurrentStorageUsed(0L);
        user.setActiveSubscription(sub);

        UserResponse dto = mapper.toDto(user);

        assertThat(dto.storageLimitGB()).isEqualTo(100);
    }

    @Test
    void toDto_resolvesPreferredLanguageCode() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setCurrentStorageUsed(0L);
        user.setPreferredLanguageEntity(LanguageEntity.builder().id(2L).code("bg").name("Bulgarian").isActive(true).build());

        UserResponse dto = mapper.toDto(user);

        assertThat(dto.preferredLanguageCode()).isEqualTo("bg");
    }

    @Test
    void toEntity_mapsFieldsBack() {
        UserResponse dto = new UserResponse(5L, "jane", "jane@example.com",
                LocalDateTime.now().minusDays(2), LocalDateTime.now(), 999L, 50, "en");

        UserEntity entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getUsername()).isEqualTo("jane");
        assertThat(entity.getEmail()).isEqualTo("jane@example.com");
        assertThat(entity.getCurrentStorageUsed()).isEqualTo(999L);
    }
}
