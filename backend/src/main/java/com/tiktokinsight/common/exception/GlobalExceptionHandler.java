package com.tiktokinsight.common.exception;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException exception) {
        var fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst();
        Map<String, String> details = fieldError
                .map(error -> Map.of(
                        "field", error.getField(),
                        "reason", error.getDefaultMessage() == null ? "参数无效" : error.getDefaultMessage()
                ))
                .orElseGet(() -> Map.of("reason", "参数无效"));
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.JSON_FORMAT_ERROR, null);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        return error(exception.status(), exception.errorCode(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleFileTooLarge() {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.FILE_TOO_LARGE, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        LOGGER.error("event=unhandled_exception message=Unexpected request failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, null);
    }

    private <T> ResponseEntity<ApiResponse<T>> error(
            HttpStatus status,
            ApiErrorCode errorCode,
            T details
    ) {
        return ResponseEntity.status(status).body(ApiResponse.failure(
                errorCode.code(),
                errorCode.message(),
                details,
                RequestIdFilter.currentRequestId()
        ));
    }
}
