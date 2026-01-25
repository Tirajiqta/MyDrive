package org.example.mydrive.dto;

import org.example.mydrive.entities.FileEntity;
import org.example.mydrive.mappers.UserMapper;

import java.time.LocalDateTime;
import java.util.List;

public record FileResponse(
        Long id,
        String originalFileName,
        String type,
        Long size, // In bytes
        Long parentId,
        UserResponse owner, // Could be just owner ID
        LocalDateTime uploadDate,
        LocalDateTime lastModifiedDate,
        Boolean isDeleted
) {
    public static FileResponse from(FileEntity e) {
        UserResponse owner = new UserMapper().toDto(e.getOwner());
        return new FileResponse(
                e.getId(),
                e.getOriginalFileName(),
                e.getType(),
                e.getSize(),
                e.getParent().getId(),
                owner,
                e.getUploadDate(),
                e.getLastModifiedDate(),
                e.getIsDeleted()
//                e.getFolderId(),
//                e.getMimeType(),
//                e.getSizeBytes(),
//                e.getStorageKey(),
//                e.getCreatedAt(),
//                e.getUpdatedAt(),
//                e.getDeletedAt()
        );
    }
}