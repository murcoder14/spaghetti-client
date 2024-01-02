package org.muralis.spaghetti.client;

import org.muralis.spaghetti.client.security.RSAKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RSAKeyProperties.class)
public class SpaghettiClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpaghettiClientApplication.class, args);
	}

}
