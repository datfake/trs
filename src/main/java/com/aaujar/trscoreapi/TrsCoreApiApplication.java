package com.aaujar.trscoreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrsCoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrsCoreApiApplication.class, args);
	}

}
