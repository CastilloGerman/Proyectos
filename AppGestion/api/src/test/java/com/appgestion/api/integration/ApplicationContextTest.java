package com.appgestion.api.integration;

import com.appgestion.api.AppGestionApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
class ApplicationContextTest {

    @Test
    void contextLoads() {
    }
}
