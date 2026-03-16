package com.mysociety.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MysocietyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MysocietyBackendApplication.class, args);
	}

}
