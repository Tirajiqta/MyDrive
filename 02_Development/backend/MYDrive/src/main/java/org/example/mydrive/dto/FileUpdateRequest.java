package org.example.mydrive.dto;


public record FileUpdateRequest(
        Long parentId, // For moving file
        String name
) {}