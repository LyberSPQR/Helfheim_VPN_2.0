package com.sloptech.helfheim.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ValidationErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationErrorResponse.FieldError(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());
        ValidationErrorResponse body = new ValidationErrorResponse("Ошибка валидации данных", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ValidationErrorResponse.FieldError(
                        StreamSupport.stream(v.getPropertyPath().spliterator(), false)
                                .reduce((first, second) -> second)
                                .map(Object::toString)
                                .orElse(""),
                        v.getMessage()))
                .collect(Collectors.toList());
        ValidationErrorResponse body = new ValidationErrorResponse("Ошибка валидации параметров", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
