package org.example.mydrive.dto;

public record DocumentEditMessage(
        String editorUsername,
        String content,
        long version
) {}
