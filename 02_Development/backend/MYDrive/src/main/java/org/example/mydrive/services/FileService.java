package org.example.mydrive.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FileCreateRequest;
import org.example.mydrive.dto.FileResponse;
import org.example.mydrive.dto.FileUpdateRequest;
import org.example.mydrive.entities.FileEntity;
import org.example.mydrive.entities.FolderEntity;
import org.example.mydrive.repositories.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {
    private final FileRepository fileRepository;

    @Transactional(readOnly = true)
    public FileResponse getById(Long id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));
        return FileResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listByFolder(Long folderId) {
        // Change this to match your repository method:
        // e.g. findAllByFolderIdAndDeletedAtIsNullOrderByCreatedAtDesc(...)
        return fileRepository.findAllByParentId(folderId).stream()
                .map(FileResponse::from)
                .toList();
    }

    public FileResponse create(FileCreateRequest req) {
        FileEntity entity = new FileEntity();

        //TODO: Map fields (adjust to your entity):
//        entity.setName(req.name());
//        entity.setFolderId(req.folderId());
//        entity.setMimeType(req.mimeType());
//        entity.setSizeBytes(req.sizeBytes());
//        entity.setStorageKey(req.storageKey());
//        entity.setCreatedAt(Instant.now());
//        entity.setUpdatedAt(Instant.now());

        FileEntity saved = fileRepository.save(entity);
        return FileResponse.from(saved);
    }

    public FileResponse update(Long id, FileUpdateRequest req) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));

        // Allow rename, etc. (adjust):
        if (req.name() != null && !req.name().isBlank()) {
            entity.setUniqueName(req.name());
        }
        entity.setLastModifiedDate(LocalDateTime.from(Instant.now()));

        return FileResponse.from(fileRepository.save(entity));
    }

    public FileResponse move(Long id, Long targetFolderId) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));
        Optional<FileEntity> targetFolder = fileRepository.findById(targetFolderId);
        if (targetFolder.isEmpty()) {
            throw new EntityNotFoundException("Folder not found: " + targetFolderId);
        }
        entity.setParent(targetFolder.get().getParent());
        entity.setLastModifiedDate(LocalDateTime.from(Instant.now()));

        return FileResponse.from(fileRepository.save(entity));
    }

    public void delete(Long id) {
        // Choose: hard delete OR soft delete
        // HARD:
        // fileRepository.deleteById(id);

        // SOFT (recommended):
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));
//        entity.set(Instant.now());
        entity.setLastModifiedDate(LocalDateTime.from(Instant.now()));
        fileRepository.save(entity);
    }
}
