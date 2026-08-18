package com.zerowiki.source;

import com.zerowiki.auth.AuthService;
import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {
    private final UploadService uploadService;
    private final AuthService authService;

    public UploadController(UploadService uploadService, AuthService authService) {
        this.uploadService = uploadService;
        this.authService = authService;
    }

    @PostMapping
    ResponseEntity<ApiResponse<SourceDtos.UploadPlanResponse>> plan(Authentication authentication,
                                                                     @Valid @RequestBody SourceDtos.UploadPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                uploadService.plan(userId(authentication), request)));
    }

    /** Local adapter equivalent of the signed PUT URL until the production object store is selected. */
    @PutMapping("/{uploadId}/content")
    ResponseEntity<Void> upload(@PathVariable UUID uploadId, @RequestParam String token,
                                @RequestBody byte[] content) {
        uploadService.write(uploadId, token, content);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uploadId}/complete")
    ResponseEntity<ApiResponse<SourceDtos.UploadCompleteResponse>> complete(Authentication authentication,
                                                                             @PathVariable UUID uploadId,
                                                                             @Valid @RequestBody SourceDtos.UploadCompleteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                uploadService.complete(userId(authentication), uploadId, request)));
    }

    @DeleteMapping("/{uploadId}")
    ResponseEntity<Void> cancel(Authentication authentication, @PathVariable UUID uploadId) {
        uploadService.cancel(userId(authentication), uploadId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
