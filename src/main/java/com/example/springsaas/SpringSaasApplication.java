package com.example.springsaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.springsaas")
@EnableJpaRepositories("com.example.springsaas")
public class SpringSaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSaasApplication.class, args);
	}

}
