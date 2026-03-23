package br.com.magnumbank.presentation.rest.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String message, String code, LocalDateTime timestamp) {
    public ErrorResponse(String message, String code) {
        this(message, code, LocalDateTime.now());
    }
}
