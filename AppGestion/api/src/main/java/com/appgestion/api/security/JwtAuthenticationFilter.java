package com.appgestion.api.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appgestion.api.service.SessionService;
import com.appgestion.api.util.LogSanitizer;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsServiceImpl userDetailsService,
                                   SessionService sessionService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // El dispatcher ERROR no debe volver a validar la sesión: si la base acaba de fallar,
        // reentrar aquí convierte ese fallo en 401 y el front cierra la sesión.
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            return true;
        }
        return ("GET".equals(request.getMethod())
                && request.getRequestURI().equals(request.getContextPath() + "/actuator/health"))
                || request.getRequestURI().startsWith(request.getContextPath() + "/publico/presupuestos/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            boolean tokenValido = false;
            if (!StringUtils.hasText(token)) {
                // Las peticiones anónimas son normales (login, registro, recursos públicos): no es un evento de seguridad.
                log.debug("JWT: sin token en {} {}", request.getMethod(), LogSanitizer.clean(request.getRequestURI()));
            } else {
                tokenValido = jwtService.validateToken(token);
                if (!tokenValido) {
                    log.warn("JWT: token inválido o expirado en {} {}", request.getMethod(),
                            LogSanitizer.clean(request.getRequestURI()));
                }
            }

            if (tokenValido) {
                String email = jwtService.extractEmail(token);
                // SEC-10: el claim "sid" es obligatorio. Un JWT sin sesión persistida no se podría revocar.
                boolean sessionOk = jwtService.extractSessionId(token)
                        .map(sid -> sessionService.validateAndTouchSession(sid, email).isPresent())
                        .orElse(false);
                if (sessionOk) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("JWT: sesión revocada o inválida (sid) en {} {}", request.getMethod(), LogSanitizer.clean(request.getRequestURI()));
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT: token EXPIRADO en {} {}", request.getMethod(), LogSanitizer.clean(request.getRequestURI()));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            // No se registra e.getMessage(): puede incluir fragmentos de claims o del token.
            log.warn("JWT: token INVÁLIDO en {} {} ({})", request.getMethod(), LogSanitizer.clean(request.getRequestURI()),
                    e.getClass().getSimpleName());
        } catch (UsernameNotFoundException e) {
            log.warn("JWT: usuario del token no encontrado en {} {}", request.getMethod(), LogSanitizer.clean(request.getRequestURI()));
        } catch (DataAccessException | TransactionException e) {
            // Neon dormido o conexión cortada. Si esto escapa del filtro, el dispatcher ERROR
            // vuelve a pasar por Spring Security sin autenticación y el cliente recibe 401.
            log.warn("JWT: base de datos no disponible en {} {} ({})", request.getMethod(),
                    LogSanitizer.clean(request.getRequestURI()), e.getClass().getSimpleName());
            writeDatabaseUnavailable(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void writeDatabaseUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"No se pudo conectar con la base de datos. Inténtalo de nuevo en unos segundos.\","
                        + "\"message\":\"No se pudo conectar con la base de datos. Inténtalo de nuevo en unos segundos.\"}");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
