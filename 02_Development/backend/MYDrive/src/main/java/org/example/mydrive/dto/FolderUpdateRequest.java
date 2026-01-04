package org.example.mydrive.dto;


public record FolderUpdateRequest(
        Long parentId, // For moving folders

        String canonicalName // Optional update
) {}
