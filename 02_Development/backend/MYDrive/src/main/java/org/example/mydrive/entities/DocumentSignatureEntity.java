package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_signatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSignatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_id", nullable = false)
    private UserEntity signedBy;

    @CreationTimestamp
    private LocalDateTime signedAt;

    private String signerName;

    private String certificateSubject;
    private String certificateIssuer;
    private LocalDateTime certificateValidFrom;
    private LocalDateTime certificateValidTo;

    /** Signature format level: PAdES-B-B, PAdES-B-T, etc. */
    private String signatureLevel;

    /** Name of the signed file stored on disk (uniqueName of the signed copy). */
    private String signedUniqueName;
}
