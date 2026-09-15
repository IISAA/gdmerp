package pe.edu.upeu.gdmerp;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bomErpOpenApi() {
        return new OpenAPI().info(new Info()
                .title("GdmERP API")
                .version("v1")
                .description("Contrato REST del backend unico de LP2"));
    }
}