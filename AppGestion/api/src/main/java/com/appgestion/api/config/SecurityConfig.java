package com.appgestion.api.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SubscriptionCheckFilter subscriptionCheckFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SubscriptionCheckFilter subscriptionCheckFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.subscriptionCheckFilter = subscriptionCheckFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/webhook/**").permitAll()
                        .requestMatchers("/auth/me").authenticated()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/presupuestos/**", "/facturas/**", "/clientes/**", "/subscription/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(subscriptionCheckFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
