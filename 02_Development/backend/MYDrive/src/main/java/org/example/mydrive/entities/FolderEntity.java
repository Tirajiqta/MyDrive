package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "folders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String canonicalName; // A non-translated base name or identifier

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // Null for root folders
    private FolderEntity parent;

    @Column(nullable = false)
    private Boolean isDeleted;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FolderTranslationEntity> translations;

    // Optional: For easy navigation within the entity graph
     @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
     private Set<FolderEntity> childrenFolderEntities;

     @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
     private Set<FileEntity> fileEntities;
}
