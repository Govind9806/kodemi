package com.example.enrollment_progress_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EnrollmentProgressServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentProgressServiceApplication.class, args);
    }

}
