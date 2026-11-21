package com.ibank.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "com.ibank")
@EnableJpaAuditing
public class IbankApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbankApiApplication.class, args);
    }
}