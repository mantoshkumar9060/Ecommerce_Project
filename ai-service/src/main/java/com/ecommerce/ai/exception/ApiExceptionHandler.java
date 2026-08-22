package com.ecommerce.ai.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AiServiceException.class)
    ResponseEntity<Map<String, Object>> ai(AiServiceException error) { return ResponseEntity.status(error.status()).body(Map.of("status", error.status().value(), "message", error.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException error) { return ResponseEntity.badRequest().body(Map.of("status", 400, "message", error.getBindingResult().getFieldError() == null ? "Invalid request" : error.getBindingResult().getFieldError().getDefaultMessage())); }
}
