package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.DocumentContentResponse;
import org.example.mydrive.dto.DocumentEditMessage;
import org.example.mydrive.entities.DocumentContentEntity;
import org.example.mydrive.repositories.DocumentContentRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final DocumentContentRepository contentRepository;

    /** Broadcast an edit to all subscribers of a document room. */
    @MessageMapping("/document/{fileId}/edit")
    @SendTo("/topic/document/{fileId}")
    public DocumentEditMessage broadcastEdit(
            @DestinationVariable Long fileId,
            DocumentEditMessage message) {

        DocumentContentEntity entity = contentRepository.findById(fileId)
                .orElseGet(() -> {
                    DocumentContentEntity e = new DocumentContentEntity();
                    e.setFileId(fileId);
                    return e;
                });

        entity.setContent(message.content());
        entity.setLastUpdated(LocalDateTime.now());
        entity.setLastEditorUsername(message.editorUsername());
        contentRepository.save(entity);

        return message;
    }
}
