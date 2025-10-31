package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_translations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_id", "language_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTranslationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity fileEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity languageEntity;

    @Column(nullable = false)
    private String displayName;
}
