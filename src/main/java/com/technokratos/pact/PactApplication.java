package com.technokratos.pact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PactApplication {

    public static void main(String[] args) {
        SpringApplication.run(PactApplication.class, args);
    }

}
