package pe.edu.upeu.gdmerp.inventario.unidadmedida;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.SincronizacionEstandaresResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.service.UnidadMedidaService;

/**
 * Inicializador de arranque para el catálogo de Unidades de Medida.
 * <p>
 * Delega en {@link UnidadMedidaService#sincronizarEstandares()}, que aplica la
 * lógica idempotente unidad-por-unidad (existsByCodigoIgnoreCase) sin importar
 * cuántos registros existan ya en la tabla.
 * </p>
 * Se ejecuta con prioridad {@code @Order(5)}, después de los inicializadores
 * de Almacenes (1) y Categorías (2/3) para garantizar coherencia.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class UnidadMedidaDataInitializer implements CommandLineRunner {

    private final UnidadMedidaService unidadMedidaService;

    @Override
    public void run(String... args) {
        try {
            log.info("Iniciando sincronización idempotente del catálogo de Unidades de Medida (SI/UN-ECE/SUNAT)...");
            SincronizacionEstandaresResponse resultado = unidadMedidaService.sincronizarEstandares();
            log.info("Unidades de Medida — {}", resultado.mensaje());

            if (!resultado.codigosNuevos().isEmpty()) {
                log.info("Códigos insertados: {}", resultado.codigosNuevos());
            }
        } catch (Exception e) {
            // No bloquea el arranque; el endpoint POST /cargar-estandares sirve como fallback
            log.warn("Sincronización de unidades de medida omitida en arranque: {}", e.getMessage());
        }
    }
}
