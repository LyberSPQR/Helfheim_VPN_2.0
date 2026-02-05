package com.sloptech.helfheim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelfheimApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelfheimApplication.class, args);
	}

}
