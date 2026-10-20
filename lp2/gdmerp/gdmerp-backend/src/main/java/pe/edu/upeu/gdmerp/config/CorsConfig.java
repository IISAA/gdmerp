package pe.edu.upeu.gdmerp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Origen explícito de la aplicación Angular
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        
        // Todos los métodos HTTP requeridos (incluyendo OPTIONS para preflight)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Permitir todas las cabeceras requeridas por el cliente y Keycloak
        config.setAllowedHeaders(List.of("*"));
        
        // Permitir envío de credenciales / Authorization
        config.setAllowCredentials(true);
        
        // Cabeceras expuestas para el cliente
        config.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
