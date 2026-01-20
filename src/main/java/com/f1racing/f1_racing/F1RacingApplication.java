package com.f1racing.f1_racing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class F1RacingApplication {

	public static void main(String[] args) {
		SpringApplication.run(F1RacingApplication.class, args);
	}

}
