package com.finantech.transacciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinanTechApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanTechApplication.class, args);
    }
}
