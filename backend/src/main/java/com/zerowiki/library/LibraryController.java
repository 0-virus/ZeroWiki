package com.zerowiki.library;

import com.zerowiki.auth.AuthService;
import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries")
public class LibraryController {
    private final LibraryService libraryService;
    private final AuthService authService;
    private final LibraryHomeService homeService;

    public LibraryController(LibraryService libraryService, AuthService authService, LibraryHomeService homeService) {
        this.libraryService = libraryService;
        this.authService = authService;
        this.homeService = homeService;
    }

    @PostMapping
    ResponseEntity<ApiResponse<LibraryDtos.LibraryResponse>> create(Authentication authentication,
                                                                      @Valid @RequestBody LibraryDtos.CreateLibraryRequest request) {
        LibraryEntity library = libraryService.create(userId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(view(library)));
    }

    @GetMapping
    ApiResponse<List<LibraryDtos.LibraryResponse>> list(Authentication authentication) {
        return new ApiResponse<>(libraryService.list(userId(authentication)).stream().map(this::view).toList());
    }

    @GetMapping("/{libraryId}")
    ApiResponse<LibraryDtos.LibraryResponse> get(Authentication authentication, @PathVariable UUID libraryId) {
        return new ApiResponse<>(view(libraryService.get(userId(authentication), libraryId)));
    }

    @PatchMapping("/{libraryId}")
    ApiResponse<LibraryDtos.LibraryResponse> update(Authentication authentication, @PathVariable UUID libraryId,
                                                     @Valid @RequestBody LibraryDtos.UpdateLibraryRequest request) {
        return new ApiResponse<>(view(libraryService.update(userId(authentication), libraryId, request)));
    }

    @PostMapping("/{libraryId}/archive")
    ApiResponse<LibraryDtos.LibraryResponse> archive(Authentication authentication, @PathVariable UUID libraryId) {
        return new ApiResponse<>(view(libraryService.archive(userId(authentication), libraryId)));
    }

    @PostMapping("/{libraryId}/restore")
    ApiResponse<LibraryDtos.LibraryResponse> restore(Authentication authentication, @PathVariable UUID libraryId) {
        return new ApiResponse<>(view(libraryService.restore(userId(authentication), libraryId)));
    }

    @GetMapping("/{libraryId}/constitution")
    ApiResponse<LibraryDtos.ConstitutionResponse> constitution(Authentication authentication, @PathVariable UUID libraryId) {
        return new ApiResponse<>(LibraryDtos.ConstitutionResponse.from(
                libraryService.currentConstitution(userId(authentication), libraryId)));
    }

    @PutMapping("/{libraryId}/constitution")
    ApiResponse<LibraryDtos.ConstitutionResponse> updateConstitution(Authentication authentication,
                                                                      @PathVariable UUID libraryId,
                                                                      @Valid @RequestBody LibraryDtos.ConstitutionRequest request) {
        return new ApiResponse<>(LibraryDtos.ConstitutionResponse.from(
                libraryService.updateConstitution(userId(authentication), libraryId, request)));
    }

    @GetMapping("/{libraryId}/constitution/versions")
    ApiResponse<List<LibraryDtos.ConstitutionResponse>> constitutionVersions(Authentication authentication,
                                                                              @PathVariable UUID libraryId) {
        return new ApiResponse<>(libraryService.constitutionVersions(userId(authentication), libraryId).stream()
                .map(LibraryDtos.ConstitutionResponse::from).toList());
    }

    @GetMapping("/{libraryId}/constitution/versions/{versionNo}")
    ApiResponse<LibraryDtos.ConstitutionResponse> constitutionVersion(Authentication authentication,
                                                                       @PathVariable UUID libraryId,
                                                                       @PathVariable int versionNo) {
        return new ApiResponse<>(LibraryDtos.ConstitutionResponse.from(
                libraryService.constitutionVersion(userId(authentication), libraryId, versionNo)));
    }

    @GetMapping("/{libraryId}/references")
    ApiResponse<List<LibraryDtos.ReferenceResponse>> references(Authentication authentication,
                                                                  @PathVariable UUID libraryId) {
        return new ApiResponse<>(libraryService.references(userId(authentication), libraryId).stream()
                .map(LibraryDtos.ReferenceResponse::from).toList());
    }

    @PostMapping("/{libraryId}/references")
    ResponseEntity<ApiResponse<LibraryDtos.ReferenceResponse>> addReference(Authentication authentication,
                                                                              @PathVariable UUID libraryId,
                                                                              @Valid @RequestBody LibraryDtos.ReferenceRequest request) {
        LibraryReferenceEntity reference = libraryService.addReference(userId(authentication), libraryId,
                request.targetLibraryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(LibraryDtos.ReferenceResponse.from(reference)));
    }

    @DeleteMapping("/{libraryId}/references/{targetLibraryId}")
    ResponseEntity<Void> removeReference(Authentication authentication, @PathVariable UUID libraryId,
                                         @PathVariable UUID targetLibraryId) {
        libraryService.removeReference(userId(authentication), libraryId, targetLibraryId);
        return ResponseEntity.noContent().build();
    }

    private LibraryDtos.LibraryResponse view(LibraryEntity library) {
        Integer constitutionVersion = library.getCurrentConstitutionVersionId() == null ? null
                : libraryService.currentConstitution(library.getOwnerId(), library.getId()).getVersionNo();
        LibraryHomeService.Snapshot snapshot = homeService.snapshot(library);
        return LibraryDtos.LibraryResponse.from(library, constitutionVersion, snapshot.currentVersion(),
                snapshot.counts(), snapshot.home());
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
