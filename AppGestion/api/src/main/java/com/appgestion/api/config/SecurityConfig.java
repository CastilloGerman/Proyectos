package com.appgestion.api.config;

import com.appgestion.api.security.AuthRateLimitFilter;
import com.appgestion.api.security.JwtAuthenticationFilter;
import com.appgestion.api.security.SubscriptionCheckFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SubscriptionCheckFilter subscriptionCheckFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final boolean authRateLimitEnabled;
    private final int authRateLimitCapacity;
    private final int authRateLimitRefillMinutes;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SubscriptionCheckFilter subscriptionCheckFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          @Value("${app.security.auth-rate-limit.enabled:true}") boolean authRateLimitEnabled,
                          @Value("${app.security.auth-rate-limit.capacity:30}") int authRateLimitCapacity,
                          @Value("${app.security.auth-rate-limit.refill-minutes:15}") int authRateLimitRefillMinutes) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.subscriptionCheckFilter = subscriptionCheckFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.authRateLimitEnabled = authRateLimitEnabled;
        this.authRateLimitCapacity = authRateLimitCapacity;
        this.authRateLimitRefillMinutes = authRateLimitRefillMinutes;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var appEndpoints = new String[]{"/presupuestos/**", "/facturas/**", "/clientes/**", "/materiales/**", "/subscription/**", "/config/**", "/dev/**"};

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/auth/register", "/auth/login", "/auth/google", "/auth/forgot-password", "/auth/reset-password",
                                "/auth/invite/**").permitAll()
                        .requestMatchers("/webhook/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/me", "/auth/profile", "/auth/account-settings", "/auth/preferences", "/auth/change-password", "/auth/totp/**", "/auth/support/**", "/auth/notifications/**").authenticated()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN");
                    // Siempre exigir autenticación para app endpoints.
                    // skipSubscriptionCheck solo afecta al SubscriptionCheckFilter (omitir Stripe), no a Spring Security.
                    auth.requestMatchers(appEndpoints).authenticated();
                    auth.anyRequest().authenticated();
                })
                // JWT debe registrarse antes que AuthRateLimitFilter use JwtAuthenticationFilter.class como ancla
                // (Spring Security 6.5+: el filtro de referencia ha de tener orden en cadena).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new AuthRateLimitFilter(authRateLimitEnabled, authRateLimitCapacity, authRateLimitRefillMinutes),
                        JwtAuthenticationFilter.class)
                .addFilterAfter(subscriptionCheckFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("401 Unauthorized: token inválido o expirado - {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Token inválido o expirado. Inicia sesión de nuevo.\","
                            + "\"message\":\"Token inválido o expirado. Inicia sesión de nuevo.\"}");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            log.warn("403 Forbidden: usuario {} sin permisos para {} {}", user, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Acceso denegado. No tienes permisos para este recurso.\","
                            + "\"message\":\"Acceso denegado. No tienes permisos para este recurso.\"}");
        };
    }
}
