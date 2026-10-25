package com.finantech.transacciones.exception;

import com.finantech.transacciones.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Solicitud invalida");
        ErrorResponse error = ErrorResponse.of(400, "VAL-001", mensaje, request.getRequestURI(), UUID.randomUUID().toString());
        return ResponseEntity.status(error.getStatus()).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(400, "VAL-002", "JSON no valido", request.getRequestURI(), UUID.randomUUID().toString());
        return ResponseEntity.status(error.getStatus()).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        String codigo = status == 401 ? "AUTH-001" : "SYS-001";
        ErrorResponse error = ErrorResponse.of(status, codigo,
                ex.getReason() != null ? ex.getReason() : "Solicitud no procesada",
                request.getRequestURI(), UUID.randomUUID().toString());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en la API: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.of(500, "SYS-001", "Error interno del servidor", request.getRequestURI(), UUID.randomUUID().toString());
        return ResponseEntity.status(error.getStatus()).body(error);
    }
}
