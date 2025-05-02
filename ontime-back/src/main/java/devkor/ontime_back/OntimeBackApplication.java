package devkor.ontime_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class OntimeBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(OntimeBackApplication.class, args);
	}

}