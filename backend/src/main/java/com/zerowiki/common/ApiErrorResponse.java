package com.zerowiki.common;

import java.util.List;

public record ApiErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, String requestId, List<FieldError> details) {
    }

    public record FieldError(String field, String reason) {
    }
}
