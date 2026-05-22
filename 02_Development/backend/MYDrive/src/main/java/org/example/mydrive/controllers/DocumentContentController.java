package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.DocumentContentResponse;
import org.example.mydrive.entities.DocumentContentEntity;
import org.example.mydrive.repositories.DocumentContentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentContentController {

    private final DocumentContentRepository contentRepository;

    /** Fetch the current saved content for a document (called when opening the editor). */
    @GetMapping("/{fileId}/content")
    public DocumentContentResponse getContent(@PathVariable Long fileId) {
        return contentRepository.findById(fileId)
                .map(e -> new DocumentContentResponse(
                        e.getFileId(),
                        e.getContent(),
                        e.getLastUpdated(),
                        e.getLastEditorUsername()))
                .orElse(new DocumentContentResponse(fileId, "", null, null));
    }
}
