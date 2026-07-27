package com.ashraf.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserExists(UserAlreadyExistsException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCreds(InvalidCredentialsException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String, String>> handleInactive(AccountNotActiveException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserDoesNotExistException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserDoesNotExistException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccountAlreadyActivatedException.class)
    public ResponseEntity<Map<String, String>> handleAccount(AccountAlreadyActivatedException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePermissionNotFound(PermissionNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoleNotFound(RoleNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SuspendedAccountException.class)
    public ResponseEntity<Map<String, String>> handleSuspendedAccount(SuspendedAccountException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthFailure(AuthenticationException ex) {
        return buildResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    // Handles @Valid/@NotBlank/@Pattern etc. failures on @RequestBody DTOs
    // (this is the one that was missing — caused the raw stack trace on /login)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if(!errors.isEmpty()) {
                errors.append(" ");
            }
            errors.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage());
        }

        return buildResponse(errors.toString(), HttpStatus.BAD_REQUEST);
    }

    // Handles malformed/unparseable JSON bodies (e.g. missing quotes, trailing comma,
    // wrong type for a field) — without this, a broken JSON body also falls through
    // to Spring's default error page with a stack trace, same problem as above.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse("Malformed request body", HttpStatus.BAD_REQUEST);
    }

    // Catch-all safety net: any exception not explicitly handled above still gets
    // a clean JSON response instead of leaking a stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return buildResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, String>> buildResponse(String message, HttpStatus status) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(RateLimitExceedException.class)
    public ResponseEntity<Map<String, String>> handleRateLimitExceeded(RateLimitExceedException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }
}