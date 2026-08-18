package com.zerowiki.ingest;

import com.zerowiki.auth.AuthService;
import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}/ingest-jobs")
public class IngestController {
    private final IngestService ingestService;
    private final AuthService authService;

    public IngestController(IngestService ingestService, AuthService authService) {
        this.ingestService = ingestService;
        this.authService = authService;
    }

    @PostMapping
    ResponseEntity<ApiResponse<IngestDtos.IngestResponse>> create(Authentication authentication,
                                                                   @PathVariable UUID libraryId,
                                                                   @Valid @RequestBody IngestDtos.CreateIngestRequest request) {
        IngestJobEntity job = ingestService.create(userId(authentication), libraryId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(IngestDtos.IngestResponse.from(job)));
    }

    @GetMapping
    ApiResponse<List<IngestDtos.IngestResponse>> list(Authentication authentication, @PathVariable UUID libraryId) {
        return new ApiResponse<>(ingestService.list(userId(authentication), libraryId).stream()
                .map(IngestDtos.IngestResponse::from).toList());
    }

    @GetMapping("/{jobId}")
    ApiResponse<IngestDtos.IngestResponse> get(Authentication authentication, @PathVariable UUID libraryId,
                                               @PathVariable UUID jobId) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.get(userId(authentication), libraryId, jobId)));
    }

    @GetMapping("/{jobId}/items")
    ApiResponse<List<IngestDtos.IngestItemResponse>> items(Authentication authentication,
                                                            @PathVariable UUID libraryId,
                                                            @PathVariable UUID jobId) {
        return new ApiResponse<>(ingestService.items(userId(authentication), libraryId, jobId).stream()
                .map(IngestDtos.IngestItemResponse::from).toList());
    }

    @PostMapping("/{jobId}/plan-approval")
    ApiResponse<IngestDtos.IngestResponse> approve(Authentication authentication, @PathVariable UUID libraryId,
                                                   @PathVariable UUID jobId,
                                                   @Valid @RequestBody IngestDtos.PlanApprovalRequest request) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.approvePlan(userId(authentication), libraryId, jobId, request)));
    }

    @GetMapping("/{jobId}/questions")
    ApiResponse<List<IngestDtos.QuestionResponse>> questions(Authentication authentication,
                                                              @PathVariable UUID libraryId,
                                                              @PathVariable UUID jobId) {
        return new ApiResponse<>(ingestService.questions(userId(authentication), libraryId, jobId).stream()
                .map(IngestDtos.QuestionResponse::from).toList());
    }

    @PostMapping("/{jobId}/questions/{questionId}/answer")
    ApiResponse<IngestDtos.IngestResponse> answer(Authentication authentication, @PathVariable UUID libraryId,
                                                  @PathVariable UUID jobId, @PathVariable UUID questionId,
                                                  @Valid @RequestBody IngestDtos.AnswerRequest request) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.answer(userId(authentication), libraryId, jobId, questionId, request)));
    }

    @PostMapping("/{jobId}/answers")
    ApiResponse<IngestDtos.IngestResponse> answerBulk(Authentication authentication,
                                                      @PathVariable UUID libraryId,
                                                      @PathVariable UUID jobId,
                                                      @Valid @RequestBody IngestDtos.BulkAnswerRequest request) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.answerBulk(userId(authentication), libraryId, jobId, request)));
    }

    @PostMapping("/{jobId}/cancel")
    ApiResponse<IngestDtos.IngestResponse> cancel(Authentication authentication, @PathVariable UUID libraryId,
                                                  @PathVariable UUID jobId) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.cancel(userId(authentication), libraryId, jobId)));
    }

    @PostMapping("/{jobId}/retry")
    ApiResponse<IngestDtos.IngestResponse> retry(Authentication authentication, @PathVariable UUID libraryId,
                                                 @PathVariable UUID jobId) {
        return new ApiResponse<>(IngestDtos.IngestResponse.from(
                ingestService.retry(userId(authentication), libraryId, jobId)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
