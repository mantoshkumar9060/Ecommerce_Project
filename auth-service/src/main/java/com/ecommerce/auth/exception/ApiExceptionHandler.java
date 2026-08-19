package com.ecommerce.auth.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<P> bad(IllegalArgumentException e) {
        return r(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<P> conflict(IllegalStateException e) {
        return r(HttpStatus.CONFLICT, e.getMessage());
    }

    private ResponseEntity<P> r(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(new P(Instant.now(), s.value(), m));
    }

    record P(Instant timestamp, int status, String message) {
    }
}
