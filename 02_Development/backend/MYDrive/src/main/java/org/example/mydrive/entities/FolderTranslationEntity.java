package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "folder_translations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"folder_id", "language_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderTranslationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private FolderEntity folderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity languageEntity;

    @Column(nullable = false)
    private String name;
}
