package org.example.mydrive.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ShareCreateRequest;
import org.example.mydrive.dto.ShareCreateResponse;
import org.example.mydrive.dto.ShareListItemResponse;
import org.example.mydrive.services.ShareService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShareCreateResponse create(@Valid @RequestBody ShareCreateRequest req) {
        return shareService.create(GeneralUtils.getIdFromToken(), req);
    }

    @GetMapping
    public List<ShareListItemResponse> listMine() {
        return shareService.listMine(GeneralUtils.getIdFromToken());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id) {
        shareService.revoke(GeneralUtils.getIdFromToken(), id);
    }
}
