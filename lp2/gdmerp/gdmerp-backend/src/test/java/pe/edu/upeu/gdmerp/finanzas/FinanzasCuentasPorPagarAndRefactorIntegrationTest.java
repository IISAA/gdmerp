package pe.edu.upeu.gdmerp.finanzas;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.mapper.CajaChicaMapper;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CentroCostoRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.RendicionCajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.service.RendicionCajaChicaService;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.mapper.CuentaPorCobrarMapper;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.CuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.mapper.CuentaPorPagarMapper;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.repository.CuentaPorPagarRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.mapper.SesionCajaMapper;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.shared.event.CompraAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.CompraRegistradaEvent;
import pe.edu.upeu.gdmerp.shared.event.PagoCompraRegistradoEvent;
import pe.edu.upeu.gdmerp.shared.event.RecepcionCompraEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaCreadaEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "CAJERO", "TESORERO"})
class FinanzasCuentasPorPagarAndRefactorIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Autowired private CuentaPorCobrarRepository cuentaPorCobrarRepository;
    @Autowired private TransaccionFinancieraRepository transaccionRepository;
    @Autowired private CentroCostoRepository centroCostoRepository;
    @Autowired private CajaChicaRepository cajaChicaRepository;
    @Autowired private RendicionCajaChicaRepository rendicionRepository;
    @Autowired private RendicionCajaChicaService rendicionService;
    @Autowired private SesionCajaRepository sesionCajaRepository;

    @Autowired private CajaChicaMapper cajaChicaMapper;
    @Autowired private CuentaPorCobrarMapper cuentaPorCobrarMapper;
    @Autowired private CuentaPorPagarMapper cuentaPorPagarMapper;
    @Autowired private SesionCajaMapper sesionCajaMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate txTemplate;
    private String sufijo;

    private final List<Long> cuentasPorPagarCreadas = new ArrayList<>();
    private final List<Long> cuentasPorCobrarCreadas = new ArrayList<>();
    private final List<Long> transaccionesCreadas = new ArrayList<>();
    private final List<Long> rendicionesCreadas = new ArrayList<>();
    private final List<Long> cajasCreadas = new ArrayList<>();
    private final List<Long> centrosCostoCreados = new ArrayList<>();
    private final List<Long> sesionesCreadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        for (Long id : cuentasPorPagarCreadas) {
            if (cuentaPorPagarRepository.existsById(id)) {
                cuentaPorPagarRepository.deleteById(id);
            }
        }
        cuentasPorPagarCreadas.clear();

        for (Long id : cuentasPorCobrarCreadas) {
            if (cuentaPorCobrarRepository.existsById(id)) {
                cuentaPorCobrarRepository.deleteById(id);
            }
        }
        cuentasPorCobrarCreadas.clear();

        for (Long id : transaccionesCreadas) {
            if (transaccionRepository.existsById(id)) {
                transaccionRepository.deleteById(id);
            }
        }
        transaccionesCreadas.clear();

        for (Long id : rendicionesCreadas) {
            if (rendicionRepository.existsById(id)) {
                rendicionRepository.deleteById(id);
            }
        }
        rendicionesCreadas.clear();

        for (Long id : cajasCreadas) {
            if (cajaChicaRepository.existsById(id)) {
                cajaChicaRepository.deleteById(id);
            }
        }
        cajasCreadas.clear();

        for (Long id : centrosCostoCreados) {
            if (centroCostoRepository.existsById(id)) {
                centroCostoRepository.deleteById(id);
            }
        }
        centrosCostoCreados.clear();

        for (Long id : sesionesCreadas) {
            if (sesionCajaRepository.existsById(id)) {
                sesionCajaRepository.deleteById(id);
            }
        }
        sesionesCreadas.clear();
    }

    @Test
    void cicloCompletoCuentaPorPagarPorEventosComprasYAnulacion() {
        Long compraId = Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000L + 500_000L;
        String comprobante = "F001-" + sufijo;
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new CompraRegistradaEvent(
                compraId,
                comprobante,
                "Proveedor Insumos SAC",
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00"),
                hoy,
                ahora,
                List.of()
        )));

        CuentaPorPagar cxpCreada = cuentaPorPagarRepository.findFirstByCompraId(compraId).orElseThrow();
        cuentasPorPagarCreadas.add(cxpCreada.getId());
        assertEquals(EstadoCuentaPorPagar.PENDIENTE, cxpCreada.getEstado());
        assertEquals(Boolean.FALSE, cxpCreada.getRecepcionConfirmada());
        assertEquals(0, new BigDecimal("118.00").compareTo(cxpCreada.getMontoTotal()));
        assertEquals(0, new BigDecimal("118.00").compareTo(cxpCreada.getSaldoPendiente()));

        TransaccionFinanciera trxCompra = transaccionRepository.findFirstByCompraId(compraId).orElseThrow();
        transaccionesCreadas.add(trxCompra.getId());
        assertEquals(TipoFinanciero.EGRESO, trxCompra.getTipo());
        assertEquals(EstadoFinanciero.PENDIENTE, trxCompra.getEstado());

        // Recepción idempotente para compra sin obligación previa tampoco falla
        assertDoesNotThrow(() -> txTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new RecepcionCompraEvent(
                        987_654_321L, "NO-EXISTE-" + sufijo, ahora, List.of()
                ))
        ));

        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new RecepcionCompraEvent(
                compraId, comprobante, ahora, List.of()
        )));
        CuentaPorPagar cxpRecepcionada = cuentaPorPagarRepository.findFirstByCompraId(compraId).orElseThrow();
        assertEquals(Boolean.TRUE, cxpRecepcionada.getRecepcionConfirmada());

        // Primer pago parcial
        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new PagoCompraRegistradoEvent(
                compraId, comprobante, "Proveedor Insumos SAC", new BigDecimal("50.00"), "TRANSFERENCIA", false, hoy
        )));
        CuentaPorPagar cxpParcial = cuentaPorPagarRepository.findWithAbonosById(cxpCreada.getId()).orElseThrow();
        assertEquals(EstadoCuentaPorPagar.PAGADO_PARCIAL, cxpParcial.getEstado());
        assertEquals(0, new BigDecimal("68.00").compareTo(cxpParcial.getSaldoPendiente()));
        assertEquals(1, cxpParcial.getAbonos().size());

        // Liquidación total
        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new PagoCompraRegistradoEvent(
                compraId, comprobante, "Proveedor Insumos SAC", new BigDecimal("68.00"), "EFECTIVO", true, hoy
        )));
        CuentaPorPagar cxpLiquidada = cuentaPorPagarRepository.findWithAbonosById(cxpCreada.getId()).orElseThrow();
        assertEquals(EstadoCuentaPorPagar.PAGADO_TOTAL, cxpLiquidada.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(cxpLiquidada.getSaldoPendiente()));
        assertEquals(2, cxpLiquidada.getAbonos().size());

        TransaccionFinanciera trxPagada = transaccionRepository.findById(trxCompra.getId()).orElseThrow();
        assertEquals(EstadoFinanciero.PAGADO, trxPagada.getEstado());

        // Anulación de segunda compra
        Long compraAnularId = compraId + 1L;
        String compAnular = "F002-" + sufijo;
        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new CompraRegistradaEvent(
                compraAnularId, compAnular, "Proveedor Anulable SAC",
                new BigDecimal("200.00"), new BigDecimal("36.00"), new BigDecimal("236.00"),
                hoy, ahora, List.of()
        )));
        CuentaPorPagar cxpPorAnular = cuentaPorPagarRepository.findFirstByCompraId(compraAnularId).orElseThrow();
        cuentasPorPagarCreadas.add(cxpPorAnular.getId());
        TransaccionFinanciera trxPorAnular = transaccionRepository.findFirstByCompraId(compraAnularId).orElseThrow();
        transaccionesCreadas.add(trxPorAnular.getId());

        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new CompraAnuladaEvent(
                compraAnularId, compAnular, LocalDateTime.now()
        )));
        CuentaPorPagar cxpAnulada = cuentaPorPagarRepository.findById(cxpPorAnular.getId()).orElseThrow();
        assertEquals(EstadoCuentaPorPagar.ANULADO, cxpAnulada.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(cxpAnulada.getSaldoPendiente()));
        assertEquals(EstadoFinanciero.ANULADO, transaccionRepository.findById(trxPorAnular.getId()).orElseThrow().getEstado());
    }

    @Test
    void ventaACreditoEventGeneraCuentaPorCobrarConSoftLinkYRollbackBeforeCommit() {
        Long ventaId = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 1_000_000L + 700_000L;
        String ticket = "TKT-FIN-" + sufijo;

        txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new VentaCreadaEvent(
                ventaId,
                ticket,
                "Cliente Crédito Finanzas",
                "vendedor-fin",
                new BigDecimal("157.20"),
                new BigDecimal("28.30"),
                new BigDecimal("185.50"),
                null,
                "CREDITO",
                LocalDateTime.now(),
                List.of()
        )));

        CuentaPorCobrar cxc = cuentaPorCobrarRepository.findByCodigo("CRE-" + ticket).orElseThrow();
        cuentasPorCobrarCreadas.add(cxc.getId());
        assertEquals(ventaId, cxc.getVentaId());
        assertEquals(EstadoCredito.PENDIENTE, cxc.getEstado());
        assertEquals(0, new BigDecimal("185.50").compareTo(cxc.getMontoTotal()));
        assertEquals(0, new BigDecimal("185.50").compareTo(cxc.getSaldoPendiente()));

        TransaccionFinanciera trxCxc = transaccionRepository.findFirstByVentaId(ventaId).orElseThrow();
        transaccionesCreadas.add(trxCxc.getId());
        assertEquals(TipoFinanciero.INGRESO, trxCxc.getTipo());
        assertEquals(EstadoFinanciero.PENDIENTE, trxCxc.getEstado());

        // Rollback ACID en BEFORE_COMMIT cuando Finanzas falla durante VentaCreadaEvent en EFECTIVO contra sesión cerrada
        SesionCaja sesionCerrada = sesionCajaRepository.save(SesionCaja.builder()
                .codigoSesion("SC-RB-" + sufijo)
                .usuarioId(998L)
                .cajero("cajero-rb")
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .fechaCierre(LocalDateTime.now().minusHours(1))
                .saldoInicial(BigDecimal.ZERO)
                .estado(EstadoCaja.CERRADA)
                .build());
        sesionesCreadas.add(sesionCerrada.getId());

        String codigoCcRollback = "CC-RB-" + sufijo;
        assertThrows(BusinessConflictException.class, () -> txTemplate.executeWithoutResult(status -> {
            centroCostoRepository.save(CentroCosto.builder()
                    .codigoCc(codigoCcRollback)
                    .nombreArea("Área Rollback")
                    .activo(true)
                    .build());

            eventPublisher.publishEvent(new VentaCreadaEvent(
                    ventaId + 1L,
                    "TKT-FAIL-" + sufijo,
                    "Cliente Fallo",
                    "vendedor-rb",
                    new BigDecimal("50.00"),
                    new BigDecimal("9.00"),
                    new BigDecimal("59.00"),
                    sesionCerrada.getId(),
                    "EFECTIVO",
                    LocalDateTime.now(),
                    List.of()
            ));
        }));

        assertTrue(centroCostoRepository.findByCodigoCc(codigoCcRollback).isEmpty(),
                "La falla en @TransactionalEventListener(BEFORE_COMMIT) debe revertir toda la transacción");
    }

    @Test
    void cajaChicaPersisteCentroCostoDirectoYRendicionAplicaFallback() {
        CentroCosto centroCosto = centroCostoRepository.save(CentroCosto.builder()
                .codigoCc("CC-DIR-" + sufijo)
                .nombreArea("Planta Lácteos " + sufijo)
                .activo(true)
                .build());
        centrosCostoCreados.add(centroCosto.getId());

        CajaChica caja = cajaChicaRepository.save(CajaChica.builder()
                .codigo("CCH-DIR-" + sufijo)
                .responsable("Custodio Directo")
                .saldoDisponible(new BigDecimal("600.00"))
                .limiteMaximo(new BigDecimal("1000.00"))
                .centroCostoId(centroCosto.getId())
                .build());
        cajasCreadas.add(caja.getId());

        assertEquals(centroCosto.getId(), cajaChicaRepository.findById(caja.getId()).orElseThrow().getCentroCostoId());

        RendicionCajaChicaRequest req = RendicionCajaChicaRequest.builder()
                .codigo("REND-FB-" + sufijo)
                .responsable("Custodio Directo")
                .cajaChicaId(caja.getId())
                .detalles(List.of(new DetalleRendicionRequest("Útiles de limpieza", 2, new BigDecimal("45.00"), null, null)))
                .build();

        RendicionCajaChicaResponse resp = rendicionService.registrarRendicion(req);
        rendicionesCreadas.add(resp.getId());

        assertEquals(1, resp.getDetalles().size());
        assertEquals(centroCosto.getId(), resp.getDetalles().get(0).getCentroCostoId());
        assertEquals(centroCosto.getCodigoCc(), resp.getDetalles().get(0).getCentroCostoCodigo());
        assertEquals(centroCosto.getNombreArea(), resp.getDetalles().get(0).getCentroCostoNombre());

        CajaChica cajaActualizada = cajaChicaRepository.findById(caja.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("510.00").compareTo(cajaActualizada.getSaldoDisponible()));
    }

    @Test
    void mappersListaIgnoranColeccionesLazyParaEvitarNPlusOne() {
        CajaChica caja = cajaChicaRepository.save(CajaChica.builder()
                .codigo("CCH-MAP-" + sufijo)
                .responsable("Responsable Mapper")
                .saldoDisponible(new BigDecimal("300.00"))
                .limiteMaximo(new BigDecimal("500.00"))
                .build());
        cajasCreadas.add(caja.getId());

        CuentaPorCobrar cxc = cuentaPorCobrarRepository.save(CuentaPorCobrar.builder()
                .codigo("CXC-MAP-" + sufijo)
                .ventaId(800_001L)
                .clienteNombre("Cliente Mapper")
                .montoTotal(new BigDecimal("100.00"))
                .saldoPendiente(new BigDecimal("100.00"))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .build());
        cuentasPorCobrarCreadas.add(cxc.getId());

        CuentaPorPagar cxp = cuentaPorPagarRepository.save(CuentaPorPagar.builder()
                .codigo("CXP-MAP-" + sufijo)
                .compraId(800_002L)
                .proveedorRazonSocial("Proveedor Mapper")
                .montoTotal(new BigDecimal("200.00"))
                .saldoPendiente(new BigDecimal("200.00"))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build());
        cuentasPorPagarCreadas.add(cxp.getId());

        SesionCaja sesion = sesionCajaRepository.save(SesionCaja.builder()
                .codigoSesion("SC-MAP-" + sufijo)
                .usuarioId(997L)
                .cajero("cajero-mapper")
                .fechaApertura(LocalDateTime.now().minusHours(2))
                .saldoInicial(new BigDecimal("100.00"))
                .estado(EstadoCaja.CERRADA)
                .build());
        sesionesCreadas.add(sesion.getId());

        txTemplate.executeWithoutResult(status -> {
            entityManager.clear();
            PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();

            CajaChica cajaLazy = cajaChicaRepository.findById(caja.getId()).orElseThrow();
            List<CajaChicaResponse> cajasDto = cajaChicaMapper.toCajaChicaResponseList(List.of(cajaLazy));
            assertFalse(persistenceUtil.isLoaded(cajaLazy, "custodiosAutorizados"),
                    "CajaChicaMapper.toCajaChicaResponseList no debe inicializar custodiosAutorizados");
            assertNull(cajasDto.get(0).getCustodiosAutorizados());

            CuentaPorCobrar cxcLazy = cuentaPorCobrarRepository.findById(cxc.getId()).orElseThrow();
            List<CuentaPorCobrarResponse> cxcDtos = cuentaPorCobrarMapper.toResponseList(List.of(cxcLazy), Map.of());
            CuentaPorCobrarDetalleResponse cxcListDetalle = cuentaPorCobrarMapper.toListDetalleResponse(cxcLazy);
            assertEquals(1, cxcDtos.size());
            assertFalse(persistenceUtil.isLoaded(cxcLazy, "abonos"),
                    "CuentaPorCobrarMapper no debe inicializar la colección lazy abonos en listados");
            assertNull(cxcListDetalle.getAbonos());

            CuentaPorPagar cxpLazy = cuentaPorPagarRepository.findById(cxp.getId()).orElseThrow();
            List<CuentaPorPagarResponse> cxpDtos = cuentaPorPagarMapper.toResponseList(List.of(cxpLazy), Map.of());
            CuentaPorPagarDetalleResponse cxpListDetalle = cuentaPorPagarMapper.toListDetalleResponse(cxpLazy);
            assertEquals(1, cxpDtos.size());
            assertFalse(persistenceUtil.isLoaded(cxpLazy, "abonos"),
                    "CuentaPorPagarMapper no debe inicializar la colección lazy abonos en listados");
            assertNull(cxpListDetalle.getAbonos());

            SesionCaja sesionLazy = sesionCajaRepository.findById(sesion.getId()).orElseThrow();
            List<SesionCajaResponse> sesionDtos = sesionCajaMapper.toResponseList(List.of(sesionLazy), Map.of());
            SesionCajaDetalleResponse sesionListDetalle = sesionCajaMapper.toListDetalleResponse(sesionLazy);
            assertEquals(1, sesionDtos.size());
            assertFalse(persistenceUtil.isLoaded(sesionLazy, "movimientos"),
                    "SesionCajaMapper no debe inicializar movimientos en listados");
            assertNull(sesionListDetalle.getMovimientos());
        });
    }
}
