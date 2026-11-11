package pe.edu.upeu.gdmerp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuración de seguridad OAuth2 Resource Server (Keycloak) y autorización por módulo/rol.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final JsonMapper PROBLEM_JSON_MAPPER = JsonMapper.builder().build();

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extraerAuthoritiesDesdeKeycloak);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler(jsonAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de monitoreo básico y documentación
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/personas/**"
                        ).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Reglas de negocio: Ventas y Delivery
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ventas", "/api/v1/ventas/**").denyAll()
                        .requestMatchers("/api/v1/ventas/auditoria", "/api/v1/ventas/reporte-omnicanal")
                        .hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/delivery/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ventas/resumen")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/*/solicitar-anulacion")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ventas/*/anular", "/api/v1/ventas/*/rechazar-anulacion")
                        .hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ventas/*/anular", "/api/v1/ventas/*/rechazar-anulacion")
                        .hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas/*/rechazar-anulacion")
                        .hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/**").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")

                        // Reglas de negocio: Inventario
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/almacenes/**",
                                "/api/v1/categorias/**",
                                "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**",
                                "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO", "JEFE_PRODUCCION", "OPERARIO")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/almacenes/**",
                                "/api/v1/categorias/**",
                                "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**",
                                "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/almacenes/**",
                                "/api/v1/categorias/**",
                                "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**",
                                "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/inventario/unidades-medida/**",
                                "/api/v1/unidades-medida/**")
                        .hasAnyRole("ADMIN", "ALMACENERO")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/almacenes/**",
                                "/api/v1/categorias/**",
                                "/api/v1/productos/**",
                                "/api/v1/inventario/unidades-medida/**",
                                "/api/v1/unidades-medida/**")
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
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/ordenes-produccion/**",
                                "/api/v1/produccion/ordenes/reportes/**")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO", "ALMACENERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes-produccion")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes-produccion/*/iniciar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes-produccion/*/cancelar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes-produccion/*/completar")
                        .hasAnyRole("ADMIN", "JEFE_PRODUCCION", "OPERARIO")

                        // Reglas de negocio: Finanzas
                        .requestMatchers(
                                "/api/v1/finanzas/sesiones",
                                "/api/v1/finanzas/sesiones/**",
                                "/api/v1/finanzas/denominaciones")
                        .hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**")
                        .hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**")
                        .hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**")
                        .hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**")
                        .hasAnyRole("ADMIN", "ADMIN_FINANZAS")

                        // Reglas de negocio: Compras
                        .requestMatchers("/api/v1/compras/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // Reglas de negocio: Catálogo
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**")
                        .hasAnyRole("ADMIN", "ALMACENERO", "JEFE_PRODUCCION", "OPERARIO", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalogo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/catalogo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/catalogo/**").hasRole("ADMIN")

                        // Reglas de negocio: Terceros (CRM)
                        .requestMatchers(HttpMethod.GET, "/api/v1/terceros/**")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR", "ALMACENERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/terceros/**")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/terceros/**")
                        .hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/terceros/**")
                        .hasAnyRole("ADMIN", "SUPERVISOR")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    private Collection<GrantedAuthority> extraerAuthoritiesDesdeKeycloak(Jwt jwt) {
        Set<String> rolesCrudos = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        agregarRoles(realmAccess != null ? realmAccess.get("roles") : null, rolesCrudos);

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object clienteObj : resourceAccess.values()) {
                if (clienteObj instanceof Map<?, ?> clienteMap) {
                    agregarRoles(clienteMap.get("roles"), rolesCrudos);
                }
            }
        }

        return rolesCrudos.stream()
                .map(rol -> {
                    String normalizado = rol.trim().toUpperCase();
                    String authority = normalizado.startsWith("ROLE_") ? normalizado : "ROLE_" + normalizado;
                    return (GrantedAuthority) new SimpleGrantedAuthority(authority);
                })
                .toList();
    }

    private void agregarRoles(Object rolesClaim, Set<String> destino) {
        if (rolesClaim instanceof Collection<?> lista) {
            lista.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(rol -> !rol.isBlank())
                    .forEach(destino::add);
        }
    }

    private AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) -> escribirProblemJson(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Autenticación requerida",
                "Se requiere un token JWT válido para acceder a este recurso"
        );
    }

    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> escribirProblemJson(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Permisos insuficientes",
                "No cuentas con los roles de Keycloak necesarios para ejecutar esta operación"
        );
    }

    private void escribirProblemJson(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (request != null && request.getRequestURI() != null && !request.getRequestURI().isBlank()) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("error", status.getReasonPhrase());
        problem.setProperty("message", detail);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            problem.setProperty("correlationId", correlationId);
        }

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(PROBLEM_JSON_MAPPER.writeValueAsString(problem));
    }
}
