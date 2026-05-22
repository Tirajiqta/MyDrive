package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.SignatureResponse;
import org.example.mydrive.services.DocumentSigningService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class DocumentSigningController {

    private final DocumentSigningService signingService;

    /** Sign a PDF file – produces a PAdES-B-B signature (eIDAS Advanced Electronic Signature). */
    @PostMapping("/files/{fileId}/sign")
    public SignatureResponse signFile(@PathVariable Long fileId) throws Exception {
        return signingService.signPdf(fileId, GeneralUtils.getIdFromToken());
    }

    /** List all signatures for a given file. */
    @GetMapping("/files/{fileId}")
    public List<SignatureResponse> getSignatures(@PathVariable Long fileId) {
        return signingService.getSignatures(fileId);
    }

    /** Download a signed PDF. */
    @GetMapping("/{signatureId}/download")
    public ResponseEntity<PathResource> downloadSigned(@PathVariable Long signatureId) {
        Path path = signingService.getSignedFilePath(signatureId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new PathResource(path));
    }
}
