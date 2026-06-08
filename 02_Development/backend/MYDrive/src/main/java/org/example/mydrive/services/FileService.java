package org.example.mydrive.services;

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
import org.example.mydrive.services.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

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

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "unnamed_file";
        }
        String uniqueName = UUID.randomUUID() + "_" + originalName;
        storageService.store(uniqueName, file.getInputStream(), file.getSize());

        FolderEntity parent = null;
        if (parentFolderId != null) {
            parent = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new EntityNotFoundException("Folder not found: " + parentFolderId));
        }

        FileEntity entity = FileEntity.builder()
                .originalFileName(originalName)
                .uniqueName(uniqueName)
                .type(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .owner(owner)
                .parent(parent)
                .isDeleted(false)
                .build();

        return FileResponse.from(fileRepository.save(entity));
    }

    public record DownloadResult(Resource resource, String originalName, String mimeType) {}

    @Transactional(readOnly = true)
    public DownloadResult getDownloadResource(Long id) {
        FileEntity entity = findFile(id);
        return new DownloadResult(
                storageService.load(entity.getUniqueName()),
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

        if (entity.getUniqueName() != null) {
            storageService.delete(entity.getUniqueName());
        }
    }

    private FileEntity findFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + id));
    }
}
