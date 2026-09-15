package pe.edu.upeu.gdmerp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Lee la variable que definiste en application.yml con fallback seguro por defecto
    @Value("${gdmerp.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Divide el string por comas y elimina los espacios en blanco accidentales (trim)
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // Aplica CORS a todos los endpoints (ajusta "/api/**" si tu prefijo es "/api/v1/**")
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // Fundamental si a futuro implementarás JWT con HTTP-Only Cookies
    }
}