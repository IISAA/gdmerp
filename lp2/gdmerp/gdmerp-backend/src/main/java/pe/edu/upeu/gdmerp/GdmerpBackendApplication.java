package pe.edu.upeu.gdmerp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching 
@SpringBootApplication
public class GdmerpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GdmerpBackendApplication.class, args);
	}

}
