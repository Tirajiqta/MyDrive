package org.example.mydrive.services;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FileCreateRequest;
import org.example.mydrive.dto.FileResponse;
import org.example.mydrive.dto.FileUpdateRequest;
import org.example.mydrive.entities.FileEntity;
import org.example.mydrive.entities.FolderEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.FileRepository;
import org.example.mydrive.repositories.FolderRepository;
import org.example.mydrive.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    @Value("${app.storage.path}")
    private String storagePath;

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(storagePath));
    }

    @Transactional(readOnly = true)
    public FileResponse getById(Long id) {
        return FileResponse.from(findFile(id));
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listByFolder(Long folderId) {
        return fileRepository.findAllByParentId(folderId).stream()
                .map(FileResponse::from)
                .toList();
    }

    public FileResponse upload(MultipartFile file, Long parentFolderId, Long ownerId) throws IOException {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + ownerId));

        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = Path.of(storagePath, uniqueName);
        Files.copy(file.getInputStream(), target);

        FolderEntity parent = null;
        if (parentFolderId != null) {
            parent = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new EntityNotFoundException("Folder not found: " + parentFolderId));
        }

        FileEntity entity = FileEntity.builder()
                .originalFileName(file.getOriginalFilename())
                .uniqueName(uniqueName)
                .type(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .owner(owner)
                .parent(parent)
                .isDeleted(false)
                .build();

        return FileResponse.from(fileRepository.save(entity));
    }

    public record DownloadResult(PathResource resource, String originalName, String mimeType) {}

    @Transactional(readOnly = true)
    public DownloadResult getDownloadResource(Long id) {
        FileEntity entity = findFile(id);
        Path filePath = Path.of(storagePath, entity.getUniqueName());
        return new DownloadResult(
                new PathResource(filePath),
                entity.getOriginalFileName(),
                entity.getType());
    }

    public FileResponse create(FileCreateRequest req) {
        FileEntity entity = new FileEntity();
        entity.setIsDeleted(false);
        return FileResponse.from(fileRepository.save(entity));
    }

    public FileResponse update(Long id, FileUpdateRequest req) {
        FileEntity entity = findFile(id);
        if (req.name() != null && !req.name().isBlank()) {
            entity.setOriginalFileName(req.name());
        }
        return FileResponse.from(fileRepository.save(entity));
    }

    public FileResponse move(Long id, Long targetFolderId) {
        FileEntity entity = findFile(id);
        FolderEntity folder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found: " + targetFolderId));
        entity.setParent(folder);
        return FileResponse.from(fileRepository.save(entity));
    }

    public void delete(Long id) {
        FileEntity entity = findFile(id);
        entity.setIsDeleted(true);
        fileRepository.save(entity);

        try {
            Files.deleteIfExists(Path.of(storagePath, entity.getUniqueName()));
        } catch (IOException ignored) {}
    }

    private FileEntity findFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));
    }
}
