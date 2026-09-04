package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Response body for a successful registration. */
@Getter
@AllArgsConstructor
public class RegisterResponse {
    private final String message;
    private final Long userId;
}
