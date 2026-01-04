package org.example.mydrive.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FolderCreateRequest;
import org.example.mydrive.dto.FolderResponse;
import org.example.mydrive.dto.FolderUpdateRequest;
import org.example.mydrive.services.FolderService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody FolderCreateRequest req) {
        return folderService.create(GeneralUtils.getIdFromToken(), req);
    }

    @GetMapping
    public List<FolderResponse> list(@RequestParam(required = false) Long parentId) {
        return folderService.list(GeneralUtils.getIdFromToken(), parentId);
    }

    @PatchMapping("/{id}")
    public FolderResponse update(@PathVariable Long id, @RequestBody FolderUpdateRequest req) {
        return folderService.update(GeneralUtils.getIdFromToken(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        folderService.delete(GeneralUtils.getIdFromToken(), id);
    }
}
