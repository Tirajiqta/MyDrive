package org.example.mydrive.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FolderCreateRequest;
import org.example.mydrive.dto.FolderResponse;
import org.example.mydrive.dto.FolderUpdateRequest;
import org.example.mydrive.entities.FolderEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.FolderRepository;
import org.example.mydrive.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @Transactional
    public FolderResponse create(Long userId, FolderCreateRequest req) {
        UserEntity owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        FolderEntity parent = null;
        if (req.parentId() != null) {
            parent = folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(req.parentId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
        }

        String name = req.canonicalName().trim();

        boolean exists = (parent == null)
                ? folderRepository.existsByOwnerIdAndParentIsNullAndNameIgnoreCaseAndIsDeletedFalse(userId, name)
                : folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCaseAndIsDeletedFalse(userId, parent.getId(), name);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Folder name already exists");
        }

        FolderEntity folder = new FolderEntity();
        folder.setOwner(owner);
        folder.setParent(parent);
        folder.setCanonicalName(name);
        folder.setIsDeleted(false);

        return toResponse(folderRepository.save(folder));
    }

    public List<FolderResponse> list(Long userId, Long parentId) {
        List<FolderEntity> folders = (parentId == null)
                ? folderRepository.findAllByOwnerIdAndParentIsNullAndIsDeletedFalse(userId)
                : folderRepository.findAllByOwnerIdAndParentIdAndIsDeletedFalse(userId, parentId);

        return folders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FolderResponse update(Long userId, Long folderId, FolderUpdateRequest req) {
        FolderEntity folder = folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(folderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        // rename
        if (req.canonicalName() != null && !req.canonicalName().isBlank()) {
            String newName = req.canonicalName().trim();

            Long parentId = folder.getParent() == null ? null : folder.getParent().getId();
            boolean exists = (parentId == null)
                    ? folderRepository.existsByOwnerIdAndParentIsNullAndNameIgnoreCaseAndIsDeletedFalse(userId, newName)
                    : folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCaseAndIsDeletedFalse(userId, parentId, newName);

            if (exists && !newName.equalsIgnoreCase(folder.getCanonicalName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Folder name already exists");
            }

            folder.setCanonicalName(newName);
        }

        // move
        if (req.parentId() != null) {
            FolderEntity newParent = folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(req.parentId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New parent folder not found"));

            if (newParent.getId().equals(folder.getId()) || isDescendant(newParent, folder)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a folder into itself/descendant");
            }

            folder.setParent(newParent);
        }

        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public void delete(Long userId, Long folderId) {
        FolderEntity folder = folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(folderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        // Soft delete
        folder.setIsDeleted(true);
        folderRepository.save(folder);
    }

    private boolean isDescendant(FolderEntity candidateParent, FolderEntity folder) {
        FolderEntity cur = candidateParent;
        while (cur != null) {
            FolderEntity p = cur.getParent();
            if (p == null) return false;
            if (p.getId().equals(folder.getId())) return true;
            cur = p;
        }
        return false;
    }

    private FolderResponse toResponse(FolderEntity f) {
        return new FolderResponse(
                f.getId(),
                f.getParent() == null ? null : f.getParent().getId(),
                f.getCanonicalName(),
                f.getCreationDate(),
                f.getLastModifiedDate(),
                f.getIsDeleted()
        );
    }
}
