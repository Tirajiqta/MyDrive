package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_contents")
@Data
@NoArgsConstructor
public class DocumentContentEntity {

    @Id
    private Long fileId;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private LocalDateTime lastUpdated;

    private String lastEditorUsername;
}
