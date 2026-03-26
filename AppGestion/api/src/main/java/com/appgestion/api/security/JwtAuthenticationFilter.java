package com.appgestion.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appgestion.api.service.SessionService;
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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            if (!StringUtils.hasText(token)) {
                log.warn("JWT: SIN TOKEN - {} {} (¿proxy no reenvía Authorization?)", request.getMethod(), request.getRequestURI());
            } else if (!jwtService.validateToken(token)) {
                log.warn("JWT: token inválido o expirado en {} {} - Authorization presente pero no válido", request.getMethod(), request.getRequestURI());
            }

            if (StringUtils.hasText(token) && jwtService.validateToken(token)) {
                String email = jwtService.extractEmail(token);
                boolean sessionOk = jwtService.extractSessionId(token)
                        .map(sid -> sessionService.validateAndTouchSession(sid, email).isPresent())
                        .orElse(true);
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
                    log.warn("JWT: sesión revocada o inválida (sid) en {} {}", request.getMethod(), request.getRequestURI());
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT: token EXPIRADO en {} {} - exp: {}", request.getMethod(), request.getRequestURI(), e.getClaims().getExpiration());
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.warn("JWT: token INVÁLIDO (firma/secret) en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        } catch (UsernameNotFoundException e) {
            log.warn("JWT: usuario no encontrado en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
