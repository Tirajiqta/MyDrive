package org.example.mydrive.controllers;

import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ShareInfoResponse;
import org.example.mydrive.services.ShareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/shares")
@RequiredArgsConstructor
public class PublicShareController {
    private final ShareService shareService;

    @GetMapping("/{token}")
    public ShareInfoResponse resolve(@PathVariable String token) {
        return shareService.resolvePublic(token);
    }
}
