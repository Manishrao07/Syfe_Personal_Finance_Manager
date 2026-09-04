package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Generic {@code { "message": "..." } } response used by login/logout/delete endpoints. */
@Getter
@AllArgsConstructor
public class MessageResponse {
    private final String message;
}
