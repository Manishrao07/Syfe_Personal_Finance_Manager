package com.syfe.financemanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Uniform error body returned by {@link com.syfe.financemanager.exception.GlobalExceptionHandler}. */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final int status;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;
}
