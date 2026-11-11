package pe.edu.upeu.gdmerp.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void accessDeniedRetornaProblemDetail403() throws Exception {
        mockMvc.perform(get("/test-errors/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Permisos insuficientes"))
                .andExpect(jsonPath("$.instance").value("/test-errors/denied"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validationErrorRetornaProblemDetail400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.errors[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errors[0].field").value("nombre"));
    }

    @Test
    void oracleUniqueConstraintRetornaProblemDetail409Especifico() throws Exception {
        mockMvc.perform(get("/test-errors/ora-unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Violación de integridad de datos"))
                .andExpect(jsonPath("$.detail").value(
                        "No se puede completar la operación: ya existe un registro con el mismo identificador o valor único"));
    }

    @Test
    void stockInsuficienteRetornaProblemDetail400() throws Exception {
        mockMvc.perform(get("/test-errors/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Stock insuficiente"))
                .andExpect(jsonPath("$.detail").value("Stock agotado para item demo"));
    }

    @RestController
    @RequestMapping("/test-errors")
    static class TestExceptionController {

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("Requiere ROLE_ADMIN");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody DummyPayload payload) {
        }

        @GetMapping("/ora-unique")
        void oraUnique() {
            throw new DataIntegrityViolationException(
                    "could not execute statement",
                    new SQLException("ORA-00001: unique constraint (GDM_CATALOGO.UK_SKU) violated", "23000", 1)
            );
        }

        @GetMapping("/stock")
        void stock() {
            throw new InsufficientStockException("Stock agotado para item demo");
        }
    }

    record DummyPayload(@NotBlank(message = "El nombre es obligatorio") String nombre) {}
}
