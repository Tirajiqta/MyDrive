package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq_translations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"faq_id", "language_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqTranslationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private FaqEntity faq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity languageEntity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    private String category;
    private String keywords;
}
