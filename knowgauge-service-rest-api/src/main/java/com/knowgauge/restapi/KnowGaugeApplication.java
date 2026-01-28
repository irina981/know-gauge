package com.knowgauge.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for KnowGauge Service.
 * This is the entry point for the REST API module.
 */
@SpringBootApplication(scanBasePackages = "com.knowgauge")
public class KnowGaugeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowGaugeApplication.class, args);
    }
}
