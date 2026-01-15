package com.sunt.NetworkTester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NetworkTesterApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(NetworkTesterApplication.class, args);
		System.out.println("Hola mundo!");	
	}

}
