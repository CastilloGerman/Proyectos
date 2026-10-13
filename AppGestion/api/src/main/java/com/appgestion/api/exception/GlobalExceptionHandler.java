package com.appgestion.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC_INTERNAL =
            "Se produjo un error interno. Inténtalo más tarde o contacta con soporte si persiste.";

    private final boolean exposeInternalErrorMessage;

    public GlobalExceptionHandler(
            @Value("${app.api.expose-internal-error-message:false}") boolean exposeInternalErrorMessage) {
        this.exposeInternalErrorMessage = exposeInternalErrorMessage;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("NoResourceFound: {}", ex.getMessage());
        // SEC-18: no se devuelve el detalle interno de la excepción (rutas y recursos del servidor).
        String message = "Ruta no disponible en la API.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        log.debug("ResponseStatusException: {} - {}", ex.getStatusCode(), message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", ex.getStatusCode().value());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceException(AiServiceException ex) {
        int status = ex.getStatus().value();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("detail", ex.getMessage());
        body.put("status", status);
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        String message = "El archivo supera el tamaño máximo permitido.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Error de solicitud";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", msg, "detail", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Operación no disponible";
        log.debug("IllegalStateException: {}", msg);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", msg, "detail", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(". "));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining(". "));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * SEC-19: {@code @PreAuthorize} denegado lanza {@link AccessDeniedException} dentro del controlador; sin este
     * handler caería en {@link #handleException(Exception)} y respondería 500 en lugar de 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("403 Forbidden (método protegido): {}", ex.getMessage());
        String message = "Acceso denegado. No tienes permisos para este recurso.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("error", message);
        body.put("status", HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /** Autenticación ausente/inválida detectada dentro de un método protegido: 401, nunca 500. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        log.warn("401 Unauthorized (método protegido): {}", ex.getMessage());
        String message = "Token inválido o expirado. Inicia sesión de nuevo.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("error", message);
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /** JSON mal formado o con tipos incorrectos: 400 (antes caía en el handler genérico como 500). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Cuerpo de petición ilegible: {}", ex.getMessage());
        String message = "El cuerpo de la petición no es válido.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("detail", message);
        body.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        // Excepciones propias de Spring MVC (método no permitido, parámetro ausente, tipo incorrecto...) ya traen
        // su código HTTP: se respeta en lugar de convertirlas en un 500.
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            log.debug("Petición rechazada: {}", ex.getMessage());
            String message = "Petición no válida.";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", message);
            body.put("detail", message);
            body.put("status", errorResponse.getStatusCode().value());
            return ResponseEntity.status(errorResponse.getStatusCode()).body(body);
        }
        // SEC-18: el detalle va solo al log, correlacionado con un errorId que el usuario puede citar a soporte.
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.warn("Error no controlado [{}]: {}", errorId, ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("errorId", errorId);
        if (exposeInternalErrorMessage && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            body.put("message", ex.getMessage());
            body.put("detail", ex.getMessage());
        } else {
            body.put("message", GENERIC_INTERNAL);
            body.put("detail", GENERIC_INTERNAL);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
