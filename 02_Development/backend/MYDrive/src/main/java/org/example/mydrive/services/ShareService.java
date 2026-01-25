package org.example.mydrive.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ShareCreateRequest;
import org.example.mydrive.dto.ShareCreateResponse;
import org.example.mydrive.dto.ShareInfoResponse;
import org.example.mydrive.dto.ShareListItemResponse;
import org.example.mydrive.entities.SharedItemEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.FileRepository;
import org.example.mydrive.repositories.FolderRepository;
import org.example.mydrive.repositories.SharedItemRepository;
import org.example.mydrive.repositories.UserRepository;
import org.example.mydrive.utils.TokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareService {
    private final SharedItemRepository sharedItemRepository;
    private final UserRepository userRepository;

     private final FileRepository fileRepository;
     private final FolderRepository folderRepository;

    @Transactional
    public ShareCreateResponse create(Long ownerId, ShareCreateRequest req) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        SharedItemEntity.ItemType itemType = parseItemType(req.itemType());
        SharedItemEntity.PermissionLevel perm = parsePermission(req.permissionLevel());

        // Ownership check (IMPORTANT)
        // TODO: check what was the idea behind this
//        assertOwnerOwnsItem(ownerId, itemType, req.itemId());

        // token generation (raw token returned once)
        String rawToken = TokenUtil.newOpaqueToken();
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        SharedItemEntity share = SharedItemEntity.builder()
                .owner(owner)
                .itemId(req.itemId())
                .itemType(itemType)
                .permissionLevel(perm)
                .tokenHash(tokenHash)
                .expiresAt(req.expiresAt())
                .revokedAt(null)
                .build();

        SharedItemEntity saved = sharedItemRepository.save(share);

        return new ShareCreateResponse(
                saved.getId(),
                rawToken,
                saved.getItemType().name(),
                saved.getItemId(),
                saved.getPermissionLevel().name(),
                saved.getExpiresAt(),
                saved.getSharedDate()
        );
    }

    public ShareInfoResponse resolvePublic(String rawToken) {
        SharedItemEntity share = loadActiveShareByRawToken(rawToken);

        return new ShareInfoResponse(
                share.getId(),
                share.getItemType().name(),
                share.getItemId(),
                share.getPermissionLevel().name(),
                share.getExpiresAt(),
                share.getSharedDate()
        );
    }

    public List<ShareListItemResponse> listMine(Long ownerId) {
        return sharedItemRepository.findAllByOwnerId(ownerId).stream()
                .map(s -> new ShareListItemResponse(
                        s.getId(),
                        s.getItemType().name(),
                        s.getItemId(),
                        s.getPermissionLevel().name(),
                        s.getExpiresAt(),
                        s.getSharedDate(),
                        s.getRevokedAt() != null
                ))
                .toList();
    }

    @Transactional
    public void revoke(Long ownerId, Long shareId) {
        SharedItemEntity share = sharedItemRepository.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));

        if (!share.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        if (share.getRevokedAt() == null) {
            share.setRevokedAt(LocalDateTime.from(Instant.now()));
            sharedItemRepository.save(share);
        }
    }

    // --------------------
    // Internal helpers
    // --------------------

    private SharedItemEntity loadActiveShareByRawToken(String rawToken) {
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        SharedItemEntity share = sharedItemRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid share link"));

        if (share.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Share link revoked");
        }
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Share link expired");
        }

        return share;
    }

    private SharedItemEntity.ItemType parseItemType(String raw) {
        try {
            return SharedItemEntity.ItemType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemType (FILE/FOLDER)");
        }
    }

    private SharedItemEntity.PermissionLevel parsePermission(String raw) {
        try {
            return SharedItemEntity.PermissionLevel.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid permissionLevel (VIEW/EDIT)");
        }
    }
}
