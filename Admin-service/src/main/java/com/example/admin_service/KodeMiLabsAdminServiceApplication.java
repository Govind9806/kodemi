package com.example.admin_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class KodeMiLabsAdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KodeMiLabsAdminServiceApplication.class, args);
	}

}
