package org.example.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FileUpdateRequest(
        Long parentId // For moving file
) {}