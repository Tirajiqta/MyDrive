package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.FileCreateRequest;
import org.example.mydrive.dto.FileResponse;
import org.example.mydrive.dto.FileUpdateRequest;
import org.example.mydrive.dto.MoveFileRequest;
import org.example.mydrive.services.FileService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/{id}")
    public FileResponse getById(@PathVariable Long id) {
        return fileService.getById(id);
    }

    @GetMapping
    public List<FileResponse> listByFolder(@RequestParam Long folderId) {
        return fileService.listByFolder(folderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse create(@RequestBody FileCreateRequest req) {
        return fileService.create(req);
    }

    @PatchMapping("/{id}")
    public FileResponse update(@PathVariable Long id, @RequestBody FileUpdateRequest req) {
        return fileService.update(id, req);
    }

    @PostMapping("/{id}/move")
    public FileResponse move(@PathVariable Long id, @RequestBody MoveFileRequest req) {
        return fileService.move(id, req.targetFolderId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        fileService.delete(id);
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", required = false) Long parentId) throws Exception {
        return fileService.upload(file, parentId, GeneralUtils.getIdFromToken());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<PathResource> download(@PathVariable Long id) throws Exception {
        FileService.DownloadResult result = fileService.getDownloadResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.originalName() + "\"")
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .body(result.resource());
    }
}
