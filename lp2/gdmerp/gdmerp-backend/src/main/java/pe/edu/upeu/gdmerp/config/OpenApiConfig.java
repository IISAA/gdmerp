package pe.edu.upeu.gdmerp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración OpenAPI / Swagger con esquema de autenticación Bearer JWT (Keycloak).
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI gdmErpOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GdmERP API")
                        .version("v1")
                        .description("API REST del monolito modular ERP (Inventario, Producción, Compras, Ventas, Finanzas, Terceros)")
                        .contact(new Contact().name("Equipo Arquitectura GdmERP")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido por Keycloak (realm gdmerp)")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
