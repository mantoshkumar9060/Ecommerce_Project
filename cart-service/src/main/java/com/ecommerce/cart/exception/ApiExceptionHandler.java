package com.ecommerce.cart.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Problem> badRequest(IllegalArgumentException e) { return response(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<Problem> conflict(IllegalStateException e) { return response(HttpStatus.CONFLICT, e.getMessage()); }
    private ResponseEntity<Problem> response(HttpStatus status, String detail) { return ResponseEntity.status(status).body(new Problem(Instant.now(), status.value(), detail)); }
    record Problem(Instant timestamp, int status, String message) { }
}
