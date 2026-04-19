package com.appgestion.api;

import com.appgestion.api.config.RailwayJdbcUrlEnvironmentListener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppGestionApiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppGestionApiApplication.class);
        app.addListeners(new RailwayJdbcUrlEnvironmentListener());
        app.run(args);
    }
}

