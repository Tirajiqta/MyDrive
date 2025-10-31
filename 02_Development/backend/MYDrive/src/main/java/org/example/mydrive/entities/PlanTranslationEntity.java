package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_translations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"plan_id", "language_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTranslationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity planEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity languageEntity;

    @Column(nullable = false)
    private String name; // Translated name

    @Column(columnDefinition = "TEXT")
    private String description;
}
