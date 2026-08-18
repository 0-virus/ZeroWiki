package com.zerowiki.source;

import com.zerowiki.auth.AuthService;
import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sources")
public class SourceController {
    private final SourceService sourceService;
    private final AuthService authService;

    public SourceController(SourceService sourceService, AuthService authService) {
        this.sourceService = sourceService;
        this.authService = authService;
    }

    @GetMapping
    ApiResponse<List<SourceDtos.SourceResponse>> list(Authentication authentication,
                                                       @RequestParam(required = false) SourceType type,
                                                       @RequestParam(required = false) UUID libraryId,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "20") int limit) {
        UUID ownerId = userId(authentication);
        return new ApiResponse<>(sourceService.list(ownerId, type, libraryId, q, limit).stream()
                .map(source -> SourceDtos.SourceResponse.from(source,
                        sourceService.versions(ownerId, source.getId()).stream().findFirst().orElse(null)))
                .toList());
    }

    @GetMapping("/{sourceId}")
    ApiResponse<SourceDtos.SourceResponse> get(Authentication authentication, @PathVariable UUID sourceId) {
        UUID ownerId = userId(authentication);
        SourceEntity source = sourceService.get(ownerId, sourceId);
        return new ApiResponse<>(SourceDtos.SourceResponse.from(source,
                sourceService.versions(ownerId, sourceId).stream().findFirst().orElse(null)));
    }

    @GetMapping("/{sourceId}/versions")
    ApiResponse<List<SourceDtos.SourceVersionResponse>> versions(Authentication authentication,
                                                                  @PathVariable UUID sourceId) {
        return new ApiResponse<>(sourceService.versions(userId(authentication), sourceId).stream()
                .map(SourceDtos.SourceVersionResponse::from).toList());
    }

    @GetMapping("/{sourceId}/versions/{versionNo}")
    ApiResponse<SourceDtos.SourceVersionResponse> version(Authentication authentication,
                                                           @PathVariable UUID sourceId,
                                                           @PathVariable int versionNo) {
        return new ApiResponse<>(SourceDtos.SourceVersionResponse.from(
                sourceService.version(userId(authentication), sourceId, versionNo)));
    }

    @GetMapping("/{sourceId}/versions/{versionNo}/content")
    ResponseEntity<InputStreamResource> content(Authentication authentication,
                                                @PathVariable UUID sourceId,
                                                @PathVariable int versionNo) {
        SourceVersionEntity version = sourceService.version(userId(authentication), sourceId, versionNo);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(version.getMediaType());
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(version.getByteSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"source-" + versionNo + "\"")
                .body(new InputStreamResource(sourceService.open(version)));
    }

    @PatchMapping("/{sourceId}")
    ApiResponse<SourceDtos.SourceResponse> rename(Authentication authentication,
                                                   @PathVariable UUID sourceId,
                                                   @Valid @RequestBody SourceDtos.UpdateSourceRequest request) {
        UUID ownerId = userId(authentication);
        SourceEntity source = sourceService.rename(ownerId, sourceId, request.title());
        return new ApiResponse<>(SourceDtos.SourceResponse.from(source,
                sourceService.versions(ownerId, sourceId).stream().findFirst().orElse(null)));
    }

    @DeleteMapping("/{sourceId}")
    ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID sourceId) {
        sourceService.delete(userId(authentication), sourceId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
