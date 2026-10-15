package com.appgestion.api.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring Boot 4 no abre el puerto hasta el final del arranque, después de crear el pool,
 * Hibernate y el resto de beans. Vercel corta el proceso a los ~27 s si todavía no hay
 * ningún socket en escucha ({@code could not connect to $PORT}). El bind se adelanta al
 * momento en que Tomcat ya existe, antes de ese trabajo. {@code WebServer.start()} es
 * idempotente: el ciclo de vida posterior no vuelve a abrir el puerto.
 */
@Configuration
@Profile("serverless")
public class EarlyPortBindConfiguration {

    @Bean
    TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            public WebServer getWebServer(ServletContextInitializer... initializers) {
                WebServer server = super.getWebServer(initializers);
                server.start();
                return server;
            }
        };
    }
}
