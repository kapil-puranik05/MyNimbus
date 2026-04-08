package com.infra.mynimbus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = "com.infra.mynimbus.models")
public class MynimbusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MynimbusApplication.class, args);
	}

}
