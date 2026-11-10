package pe.edu.upeu.gdmerp;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${gdmerp.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.Set<String> rolesCrudos = new java.util.LinkedHashSet<>();

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> listaRealm) {
                listaRealm.stream().filter(java.util.Objects::nonNull).map(Object::toString).forEach(rolesCrudos::add);
            }

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                for (Object clienteObj : resourceAccess.values()) {
                    if (clienteObj instanceof Map<?, ?> clienteMap && clienteMap.get("roles") instanceof Collection<?> listaCliente) {
                        listaCliente.stream().filter(java.util.Objects::nonNull).map(Object::toString).forEach(rolesCrudos::add);
                    }
                }
            }

            return rolesCrudos.stream()
                    .map(rol -> {
                        String normalizado = rol.trim().toUpperCase();
                        if (normalizado.startsWith("ROLE_")) {
                            return (GrantedAuthority) new SimpleGrantedAuthority(normalizado);
                        }
                        return (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + normalizado);
                    })
                    .toList();
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Spring convierte automáticamente la cadena separada por comas del YML en una Lista
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
                                "/v3/api-docs/**", "/api/v1/personas/**")
                        .permitAll()

                        // Reglas de negocio: Ventas y Delivery
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ventas", "/api/v1/ventas/**").denyAll()
                        .requestMatchers("/api/v1/ventas/auditoria", "/api/v1/ventas/reporte-omnicanal").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/delivery/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ventas/resumen").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/*/solicitar-anulacion").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ventas/*/anular", "/api/v1/ventas/*/rechazar-anulacion").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ventas/*/anular", "/api/v1/ventas/*/rechazar-anulacion").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas/*/rechazar-anulacion").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/**").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")

                        // Reglas de negocio: Inventario
                        .requestMatchers(HttpMethod.GET, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**", "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO", "JEFE_PRODUCCION", "OPERARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**", "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**", "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/inventario/unidades-medida/**", "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/almacenes/**",
                                "/api/v1/categorias/**", "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**", "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")

                        // Reglas de negocio: Lista de Materiales (BOM / Recetas)
                        .requestMatchers(HttpMethod.GET, "/api/v1/listas-materiales/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "ALMACENERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/listas-materiales/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/listas-materiales/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/listas-materiales/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")

                        // Reglas de negocio: Producción
                        .requestMatchers(HttpMethod.GET, "/api/v1/centros-trabajo/**")
                        .hasAnyRole("ADMIN", "ALMACENERO", "JEFE_PRODUCCION", "OPERARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/centros-trabajo/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/centros-trabajo/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/centros-trabajo/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ordenes-produccion/**", "/api/v1/produccion/ordenes/reportes/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO", "ALMACENERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes-produccion")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/ordenes-produccion/*/iniciar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/ordenes-produccion/*/cancelar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/ordenes-produccion/*/completar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO")

                        // Reglas de negocio: Finanzas
                        .requestMatchers("/api/v1/finanzas/sesiones", "/api/v1/finanzas/sesiones/**", "/api/v1/finanzas/denominaciones").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS")

                        // Reglas de negocio: Compras
                        .requestMatchers("/api/v1/compras/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // Todo lo demás requiere estar autenticado
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter)));

        return http.build();
    }
}
