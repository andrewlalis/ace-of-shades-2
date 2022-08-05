package nl.andrewl.aos2registryapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Aos2RegistryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(Aos2RegistryApiApplication.class, args);
	}

}
