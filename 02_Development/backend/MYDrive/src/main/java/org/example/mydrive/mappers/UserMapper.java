package org.example.mydrive.mappers;

import org.example.mydrive.dto.UserResponse;
import org.example.mydrive.entities.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements GenericMapper<UserEntity, UserResponse> {
    @Override
    public UserResponse toDto(UserEntity entity) {
        Integer storageLimitGb = (entity.getActiveSubscription() != null && entity.getActiveSubscription().getPlanEntity() != null)
                ? entity.getActiveSubscription().getPlanEntity().getStorageLimitGB()
                : null;
        String langCode = entity.getPreferredLanguageEntity() != null
                ? entity.getPreferredLanguageEntity().getCode()
                : null;

        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getLastLogin(),
                entity.getCurrentStorageUsed(),
                storageLimitGb,
                langCode
        );
    }

    @Override
    public UserEntity toEntity(UserResponse dto) {
        UserEntity entity = new UserEntity();

        entity.setId(dto.id());
        entity.setUsername(dto.username());
        entity.setEmail(dto.email());
        entity.setCreatedAt(dto.createdAt());
        entity.setLastLogin(dto.lastLogin());
        entity.setCurrentStorageUsed(dto.currentStorageUsedBytes());

        return entity;
    }
}
