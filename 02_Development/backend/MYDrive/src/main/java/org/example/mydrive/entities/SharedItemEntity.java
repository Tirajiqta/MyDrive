package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_items",
        indexes = {
                @Index(name = "idx_shared_owner", columnList = "owner_id"),
                @Index(name = "idx_shared_token_hash", columnList = "token_hash"),
                @Index(name = "idx_shared_target", columnList = "item_type,item_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedItemEntity {
    public enum ItemType { FILE, FOLDER }
    public enum PermissionLevel { VIEW, EDIT, DELETE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long itemId; // ID of the shared file or folder

    @Column(nullable = false, length = 10)
    private ItemType itemType; // "FILE" or "FOLDER"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner; // The user who initiated the sharing

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id") // Null for public links
    private UserEntity sharedWithUser;

    @Column(nullable = false, length = 50)
    private PermissionLevel permissionLevel; // "VIEW", "EDIT"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sharedDate;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
