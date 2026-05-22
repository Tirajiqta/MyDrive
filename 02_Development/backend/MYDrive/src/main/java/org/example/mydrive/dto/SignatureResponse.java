package org.example.mydrive.dto;

import org.example.mydrive.entities.DocumentSignatureEntity;

import java.time.LocalDateTime;

public record SignatureResponse(
        Long id,
        Long fileId,
        String signerName,
        String certificateSubject,
        String certificateIssuer,
        LocalDateTime certificateValidFrom,
        LocalDateTime certificateValidTo,
        LocalDateTime signedAt,
        String signatureLevel,
        String signedFileDownloadPath
) {
    public static SignatureResponse from(DocumentSignatureEntity e) {
        return new SignatureResponse(
                e.getId(),
                e.getFileId(),
                e.getSignerName(),
                e.getCertificateSubject(),
                e.getCertificateIssuer(),
                e.getCertificateValidFrom(),
                e.getCertificateValidTo(),
                e.getSignedAt(),
                e.getSignatureLevel(),
                "/api/signatures/" + e.getId() + "/download"
        );
    }
}
