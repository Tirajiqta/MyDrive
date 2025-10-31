package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long itemId; // ID of the shared file or folder

    @Column(nullable = false, length = 10)
    private String itemType; // "FILE" or "FOLDER"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner; // The user who initiated the sharing

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id") // Null for public links
    private UserEntity sharedWithUser;

    @Column(nullable = false, length = 50)
    private String permissionLevel; // "VIEW", "EDIT"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sharedDate;

    @Column(unique = true)
    private String shareLinkToken; // For public shareable links

    @Column(length = 50)
    private String linkAccessType;
}
