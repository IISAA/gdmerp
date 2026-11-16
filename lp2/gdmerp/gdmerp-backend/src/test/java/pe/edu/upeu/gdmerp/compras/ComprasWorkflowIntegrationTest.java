package pe.edu.upeu.gdmerp.compras;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.OrdenCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.repository.OrdenCompraRepository;
import pe.edu.upeu.gdmerp.compras.ordencompra.service.ComprasService;
import pe.edu.upeu.gdmerp.exception.BusinessException;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.AlbaranEntrada;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.EstadoAlbaran;
import pe.edu.upeu.gdmerp.inventario.recepcion.repository.AlbaranEntradaRepository;
import pe.edu.upeu.gdmerp.shared.event.FacturaProveedorRegistradaEvent;
import pe.edu.upeu.gdmerp.shared.event.MercanciaRecibidaEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "COMPRADOR", "ALMACENERO", "CONTADOR"})
class ComprasWorkflowIntegrationTest {

    @Autowired
    private ComprasService comprasService;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private AlbaranEntradaRepository albaranEntradaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Workflow A: Confirmación de Orden genera Albarán de Entrada PENDIENTE en Inventario")
    void testConfirmarOrdenGeneraAlbaranPendiente() {
        // 1. Crear cotización BORRADOR
        OrdenCompra orden = comprasService.crearCotizacionBorrador(
                100L,
                LocalDate.now().plusDays(7),
                "CREDITO_30_DIAS",
                "Compra de cacao de alta pureza",
                List.of(
                        new ComprasService.LineaOrdenDto(501L, new BigDecimal("100.000"), new BigDecimal("15.50")),
                        new ComprasService.LineaOrdenDto(502L, new BigDecimal("50.000"), new BigDecimal("22.00"))
                )
        );

        assertNotNull(orden.getId());
        assertEquals(EstadoCompra.BORRADOR, orden.getEstado());
        assertEquals(0, new BigDecimal("2650.00").compareTo(orden.getSubtotalEstimado()));
        assertEquals(0, new BigDecimal("3127.00").compareTo(orden.getTotalEstimado()));

        // 2. Confirmar la orden -> debe disparar Workflow A hacia Inventario
        OrdenCompra confirmada = comprasService.confirmarOrden(orden.getId());
        assertEquals(EstadoCompra.CONFIRMADA, confirmada.getEstado());

        // 3. Verificar que InventarioCompraListener generó el Albarán de Entrada en PENDIENTE
        transactionTemplate.execute(status -> {
            AlbaranEntrada albaran = albaranEntradaRepository.findByOrdenCompraId(confirmada.getId())
                    .orElseThrow(() -> new AssertionError("El albarán de entrada debió ser generado por el listener"));

            assertEquals(EstadoAlbaran.PENDIENTE, albaran.getEstado());
            assertEquals(100L, albaran.getProveedorId());
            assertEquals(2, albaran.getDetalles().size());
            return null;
        });
    }

    @Test
    @DisplayName("Defensa de Estado: Intentar confirmar orden ya CONFIRMADA o ANULADA lanza BusinessException")
    void testDefensaEstadoTransicionesIlegales() {
        OrdenCompra orden = comprasService.crearCotizacionBorrador(
                101L,
                LocalDate.now().plusDays(5),
                "CONTADO",
                "Insumos de prueba",
                List.of(new ComprasService.LineaOrdenDto(601L, new BigDecimal("10.000"), new BigDecimal("5.00")))
        );

        // Confirmación válida
        comprasService.confirmarOrden(orden.getId());

        // Intento 1: Re-confirmar orden ya CONFIRMADA
        BusinessException ex1 = assertThrows(BusinessException.class, () -> comprasService.confirmarOrden(orden.getId()));
        assertTrue(ex1.getMessage().contains("ya se encuentra CONFIRMADA"));

        // Intento 2: Confirmar orden ANULADA
        OrdenCompra ordenParaAnular = comprasService.crearCotizacionBorrador(
                102L,
                LocalDate.now().plusDays(3),
                "CONTADO",
                "Orden a cancelar",
                List.of(new ComprasService.LineaOrdenDto(602L, new BigDecimal("5.000"), new BigDecimal("12.00")))
        );
        comprasService.anularOrden(ordenParaAnular.getId(), "Cancelado por cambio de presupuesto");

        BusinessException ex2 = assertThrows(BusinessException.class, () -> comprasService.confirmarOrden(ordenParaAnular.getId()));
        assertTrue(ex2.getMessage().contains("CANCELADA") || ex2.getMessage().contains("ANULADA"));
    }

    @Test
    @DisplayName("Workflows B y C: Recepción Física y Facturación con 3-Way Matching")
    void testCicloCompletoRecepcionYFacturacion() {
        // 1. Crear y confirmar orden
        OrdenCompra orden = comprasService.crearCotizacionBorrador(
                103L,
                LocalDate.now().plusDays(10),
                "CREDITO_15_DIAS",
                "Materia prima agroalimentaria",
                List.of(new ComprasService.LineaOrdenDto(701L, new BigDecimal("20.000"), new BigDecimal("50.00")))
        );
        comprasService.confirmarOrden(orden.getId());

        // 2. Intento de Facturación antes de Recepción Física (Violación 3-Way Matching)
        assertThrows(BusinessException.class, () -> {
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(new FacturaProveedorRegistradaEvent(
                        orden.getId(),
                        new BigDecimal("1000.00"),
                        "F001-0004523"
                ));
                return null;
            });
        });

        // 3. Workflow B: Inventario notifica recepción física completada
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new MercanciaRecibidaEvent(
                    orden.getId(),
                    LocalDateTime.now()
            ));
            return null;
        });

        OrdenCompra recibida = ordenCompraRepository.findById(orden.getId()).orElseThrow();
        assertEquals(EstadoCompra.RECIBIDA, recibida.getEstado());
        assertNotNull(recibida.getFechaRecepcion());

        // 4. Workflow C: Finanzas notifica registro de Factura y Cuenta por Pagar
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new FacturaProveedorRegistradaEvent(
                    orden.getId(),
                    new BigDecimal("1000.00"),
                    "F001-0004523"
            ));
            return null;
        });

        OrdenCompra facturada = ordenCompraRepository.findById(orden.getId()).orElseThrow();
        assertEquals(EstadoCompra.FACTURADA, facturada.getEstado());
        assertTrue(facturada.getNotas().contains("F001-0004523"));
    }
}
