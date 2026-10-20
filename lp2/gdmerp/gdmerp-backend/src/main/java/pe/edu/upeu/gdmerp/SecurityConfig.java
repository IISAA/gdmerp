package pe.edu.upeu.gdmerp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Object roles = realmAccess == null ? null : realmAccess.get("roles");
            if (!(roles instanceof Collection<?> lista)) {
                return List.<GrantedAuthority>of();
            }
            return lista.stream()
                    .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                    .toList();
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // Enlace explícito de CORS con CorsConfigurationSource bean antes de las reglas de autorización
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Endpoints Públicos
                        .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Reglas de negocio: Inventario
                        .requestMatchers(HttpMethod.GET, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAnyRole("ADMIN", "ALMACENERO", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.POST, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasRole("ADMIN")

                        // Reglas de negocio: Producción
                        .requestMatchers(HttpMethod.GET, "/api/v1/ordenes-produccion/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes-produccion")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/ordenes-produccion/*/cancelar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/ordenes-produccion/*/completar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO")

                        // Todo lo demás requiere estar autenticado
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter)));

        return http.build();
    }
}
