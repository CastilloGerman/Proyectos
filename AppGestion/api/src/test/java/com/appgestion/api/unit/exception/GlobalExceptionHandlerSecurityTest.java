package com.appgestion.api.unit.exception;

import com.appgestion.api.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** SEC-18 / SEC-19: errores inesperados no filtran detalles internos y los 4xx de Spring/Security no acaban en 500. */
class GlobalExceptionHandlerSecurityTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(false);

    @Test
    void accessDenied_devuelve403() {
        ResponseEntity<Map<String, Object>> r = handler.handleAccessDenied(new AccessDeniedException("x"));
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
    }

    @Test
    void authenticationException_devuelve401() {
        ResponseEntity<Map<String, Object>> r = handler.handleAuthentication(new BadCredentialsException("x"));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }

    @Test
    void metodoNoPermitido_devuelve405_noEsUn500() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleException(new HttpRequestMethodNotSupportedException(HttpMethod.TRACE.name()));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, r.getStatusCode());
    }

    @Test
    void errorInesperado_noExponeElMensajeInternal_peroDaUnErrorId() {
        String secreto = "jdbc:postgresql://db-interna:5432/prod password=hunter2";
        ResponseEntity<Map<String, Object>> r = handler.handleException(new RuntimeException(secreto));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        Map<String, Object> body = r.getBody();
        assertNotNull(body);
        assertNotNull(body.get("errorId"));
        assertFalse(String.valueOf(body.get("message")).contains("hunter2"));
        assertFalse(String.valueOf(body.get("detail")).contains("db-interna"));
    }
}
