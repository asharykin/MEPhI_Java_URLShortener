package ru.mephi.url.exception;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.mephi.url.dto.ErrorResponseDto;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingRequestHeaderException(HttpMethod method) {
        String error = "Для %s коротких ссылок в запросе обязательно должен быть заголовок UUID";
        if (method == HttpMethod.PUT) {
            error = String.format(error, "редактирования параметров");
        } else {
            error = String.format(error, "удаления");
        }

        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setError(error);
        return new ResponseEntity<>(responseDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(Exception ex) {
        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setError(ex.getMessage());
        return new ResponseEntity<>(responseDto, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setErrors(errors);
        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFoundExceptions(Exception ex) {
        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setError(ex.getMessage());
        return new ResponseEntity<>(responseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityExistsExceptions(Exception ex) {
        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setError(ex.getMessage());
        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleArgumentNotValidException(Exception ex) {
        ErrorResponseDto responseDto = new ErrorResponseDto();
        responseDto.setError(ex.getMessage());
        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }
}
