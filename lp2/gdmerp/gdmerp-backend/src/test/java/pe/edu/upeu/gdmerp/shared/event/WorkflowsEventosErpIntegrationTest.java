package pe.edu.upeu.gdmerp.shared.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.exception.VentaYaAnuladaException;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.RegistrarAbonoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.service.CuentaPorCobrarService;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.service.FinanzasService;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;
import pe.edu.upeu.gdmerp.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.gdmerp.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.MetodoPagoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.Venta;
import pe.edu.upeu.gdmerp.ventas.venta.repository.VentaRepository;
import pe.edu.upeu.gdmerp.ventas.venta.service.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "JEFE_PRODUCCION", "OPERARIO", "ALMACENERO", "VENDEDOR", "SUPERVISOR", "CAJERO"})
class WorkflowsEventosErpIntegrationTest {

    @Autowired private OrdenProduccionService ordenProduccionService;
    @Autowired private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired private CentroTrabajoRepository centroTrabajoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private InventarioOperacionService inventarioOperacionService;
    @Autowired private FinanzasService finanzasService;
    @Autowired private CuentaPorCobrarService cuentaPorCobrarService;
    @Autowired private CuentaPorCobrarRepository cuentaRepository;
    @Autowired private TransaccionFinancieraRepository transaccionRepository;
    @Autowired private CategoriaFinancieraRepository categoriaFinancieraRepository;
    @Autowired private VentaService ventaService;
    @Autowired private VentaRepository ventaRepository;

    private String sufijo;
    private final List<Long> ordenesCreadas = new ArrayList<>();
    private final List<Long> centrosCreados = new ArrayList<>();
    private final List<Long> ventasCreadas = new ArrayList<>();
    private final List<String> ticketsCreados = new ArrayList<>();
    private final List<String> codigosTransaccion = new ArrayList<>();
    private final List<String> comprobantesCreados = new ArrayList<>();
    private final List<Long> categoriasFinancierasCreadas = new ArrayList<>();
    private final List<Long> productosCreados = new ArrayList<>();
    private final List<Long> almacenesCreados = new ArrayList<>();
    private final List<Long> categoriasCreadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        for (Long ordenId : ordenesCreadas) {
            if (ordenProduccionRepository.existsById(ordenId)) {
                ordenProduccionRepository.deleteById(ordenId);
            }
        }
        ordenesCreadas.clear();

        for (Long centroId : centrosCreados) {
            if (centroTrabajoRepository.existsById(centroId)) {
                centroTrabajoRepository.deleteById(centroId);
            }
        }
        centrosCreados.clear();

        for (String codigo : codigosTransaccion) {
            transaccionRepository.findByCodigo(codigo).ifPresent(transaccionRepository::delete);
        }
        codigosTransaccion.clear();

        for (String comp : comprobantesCreados) {
            transaccionRepository.findFirstByNumeroComprobante(comp).ifPresent(transaccionRepository::delete);
        }
        comprobantesCreados.clear();

        for (String ticket : ticketsCreados) {
            transaccionRepository.findFirstByNumeroComprobante(ticket).ifPresent(transaccionRepository::delete);
            cuentaRepository.findByCodigo("CRE-" + ticket).ifPresent(cuentaRepository::delete);
        }
        ticketsCreados.clear();

        for (Long ventaId : ventasCreadas) {
            if (ventaRepository.existsById(ventaId)) {
                ventaRepository.deleteById(ventaId);
            }
        }
        ventasCreadas.clear();

        for (Long catFinId : categoriasFinancierasCreadas) {
            if (categoriaFinancieraRepository.existsById(catFinId)) {
                categoriaFinancieraRepository.deleteById(catFinId);
            }
        }
        categoriasFinancierasCreadas.clear();

        for (Long prodId : productosCreados) {
            movimientoRepository.deleteAll(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(prodId));
            loteRepository.deleteAll(loteRepository.findByProductoId(prodId));
            if (productoRepository.existsById(prodId)) {
                productoRepository.deleteById(prodId);
            }
        }
        productosCreados.clear();

        for (Long almId : almacenesCreados) {
            if (almacenRepository.existsById(almId)) {
                almacenRepository.deleteById(almId);
            }
        }
        almacenesCreados.clear();

        for (Long catId : categoriasCreadas) {
            if (categoriaRepository.existsById(catId)) {
                categoriaRepository.deleteById(catId);
            }
        }
        categoriasCreadas.clear();
    }

    @Test
    void workflowProduccionAInventarioConsumeAlIniciarEIngresaAlFinalizar() {
        Producto insumo = crearProducto("50.0000", 50, TipoProducto.MATERIA_PRIMA, "10.00");
        Producto terminado = crearProducto("0.0000", 0, TipoProducto.TERMINADO, "35.00");
        CentroTrabajo centro = crearCentroTrabajo();

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProductoTerminadoId(terminado.getId());
        orden.setProducto(terminado.getNombre());
        orden.setCantidadPlanificada(new BigDecimal("10.0000"));
        orden.setEstado(EstadoOrdenProduccion.PLANIFICADO);
        orden.setCentroTrabajo(centro);

        DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
        detalle.setMateriaPrimaId(insumo.getId());
        detalle.setNombreProducto(insumo.getNombre());
        detalle.setCantidadRequerida(new BigDecimal("20.0000"));
        detalle.setCostoUnitario(new BigDecimal("10.00"));
        orden.addDetalle(detalle);

        orden = ordenProduccionRepository.save(orden);
        ordenesCreadas.add(orden.getId());

        // 1. Iniciar producción -> EN_PROCESO (emite OrdenProduccionAprobadaEvent)
        OrdenProduccionResponse iniciada = ordenProduccionService.procesarTransicionEstado(
                orden.getId(), EstadoOrdenProduccion.EN_PROCESO);
        assertEquals(EstadoOrdenProduccion.EN_PROCESO, iniciada.estado());

        Producto insumoPostInicio = productoRepository.findById(insumo.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("30.0000").compareTo(insumoPostInicio.getStockTotal()));
        List<MovimientoInventario> movsInsumo = movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(insumo.getId());
        assertEquals(1, movsInsumo.size());
        assertEquals("SALIDA_PRODUCCION", movsInsumo.get(0).getTipoMovimiento());

        // 2. Finalizar producción -> FINALIZADA (emite OrdenProduccionCompletadaEvent)
        OrdenProduccionResponse finalizada = ordenProduccionService.procesarTransicionEstado(
                orden.getId(), EstadoOrdenProduccion.FINALIZADA);
        assertEquals(EstadoOrdenProduccion.FINALIZADA, finalizada.estado());
        assertNotNull(finalizada.loteGenerado());

        Producto terminadoPostFin = productoRepository.findById(terminado.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("10.0000").compareTo(terminadoPostFin.getStockTotal()));
        List<Lote> lotesTerminado = loteRepository.findByProductoId(terminado.getId());
        assertEquals(1, lotesTerminado.size());
        assertEquals(10, lotesTerminado.get(0).getCantidadActual());
        List<MovimientoInventario> movsTerminado = movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(terminado.getId());
        assertEquals(1, movsTerminado.size());
        assertEquals("ENTRADA_PRODUCCION", movsTerminado.get(0).getTipoMovimiento());
    }

    @Test
    void workflowProduccionAInventarioRevierteSiStockInsumoEsInsuficiente() {
        Producto insumo = crearProducto("5.0000", 5, TipoProducto.MATERIA_PRIMA, "8.00");
        Producto terminado = crearProducto("0.0000", 0, TipoProducto.TERMINADO, "20.00");
        CentroTrabajo centro = crearCentroTrabajo();

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProductoTerminadoId(terminado.getId());
        orden.setProducto(terminado.getNombre());
        orden.setCantidadPlanificada(new BigDecimal("15.0000"));
        orden.setEstado(EstadoOrdenProduccion.PLANIFICADO);
        orden.setCentroTrabajo(centro);

        DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
        detalle.setMateriaPrimaId(insumo.getId());
        detalle.setNombreProducto(insumo.getNombre());
        detalle.setCantidadRequerida(new BigDecimal("30.0000"));
        detalle.setCostoUnitario(new BigDecimal("8.00"));
        orden.addDetalle(detalle);

        orden = ordenProduccionRepository.save(orden);
        ordenesCreadas.add(orden.getId());

        Long ordenId = orden.getId();
        assertThrows(ResponseStatusException.class, () ->
                ordenProduccionService.procesarTransicionEstado(ordenId, EstadoOrdenProduccion.EN_PROCESO));

        OrdenProduccion trasFallo = ordenProduccionRepository.findById(ordenId).orElseThrow();
        assertEquals(EstadoOrdenProduccion.PLANIFICADO, trasFallo.getEstado(),
                "La falla en BEFORE_COMMIT del listener debe hacer rollback al estado PLANIFICADO");
        Producto insumoIntacto = productoRepository.findById(insumo.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5.0000").compareTo(insumoIntacto.getStockTotal()));
    }

    @Test
    void workflowFinanzasAVentasMarcaVentaComoPagadaPorTransaccionYCobroCredito() {
        Producto producto = crearProducto("30.0000", 30, TipoProducto.TERMINADO, "25.00");

        // Caso A: Pago directo vía FinanzasServiceImpl.crearTransaccion con ventaId
        VentaRequest reqDirecta = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.CREDITO)
                .clienteNombre("Cliente Pago Directo")
                .operacionId("OP-PAG-DIR-" + sufijo)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 2)))
                .build();
        VentaResponse ventaDirecta = ventaService.crear(reqDirecta, "vendedor-fin");
        ventasCreadas.add(ventaDirecta.getId());
        ticketsCreados.add(ventaDirecta.getNumeroTicket());
        assertEquals(EstadoVenta.REGISTRADA, ventaDirecta.getEstado());

        CategoriaFinanciera cat = new CategoriaFinanciera();
        cat.setNombre("Cobro Directo Test " + sufijo);
        cat.setTipoDefault(TipoFinanciero.INGRESO);
        cat = categoriaFinancieraRepository.save(cat);
        categoriasFinancierasCreadas.add(cat.getId());

        String codTrx = "TRX-PAG-DIR-" + sufijo;
        codigosTransaccion.add(codTrx);
        TransaccionFinancieraResponse trxResp = finanzasService.crearTransaccion(
                TransaccionFinancieraRequest.builder()
                        .codigo(codTrx)
                        .concepto("Cobro factura venta #" + ventaDirecta.getId())
                        .monto(new BigDecimal("50.00"))
                        .fechaTransaccion(LocalDate.now())
                        .tipo(TipoFinanciero.INGRESO)
                        .estado(EstadoFinanciero.PAGADO)
                        .categoriaFinancieraId(cat.getId())
                        .ventaId(ventaDirecta.getId())
                        .build()
        );
        assertNotNull(trxResp.getId());

        Venta ventaDirectaActualizada = ventaRepository.findById(ventaDirecta.getId()).orElseThrow();
        assertEquals(EstadoVenta.PAGADA, ventaDirectaActualizada.getEstado());

        // Caso B: Liquidación total desde CuentaPorCobrarServiceImpl.registrarAbono
        VentaRequest reqCredito = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.CREDITO)
                .clienteNombre("Cliente Abono Completo")
                .operacionId("OP-PAG-CRE-" + sufijo)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 4)))
                .build();
        VentaResponse ventaCredito = ventaService.crear(reqCredito, "vendedor-cre");
        ventasCreadas.add(ventaCredito.getId());
        ticketsCreados.add(ventaCredito.getNumeroTicket());

        CuentaPorCobrar cxc = cuentaRepository.findByCodigo("CRE-" + ventaCredito.getNumeroTicket()).orElseThrow();
        assertEquals(EstadoCredito.PENDIENTE, cxc.getEstado());

        cuentaPorCobrarService.registrarAbono(
                cxc.getId(),
                RegistrarAbonoRequest.builder()
                        .monto(cxc.getMontoTotal())
                        .metodoPago(MetodoPago.YAPE)
                        .nota("Liquidación total")
                        .build()
        );

        Venta ventaCreditoPagada = ventaRepository.findById(ventaCredito.getId()).orElseThrow();
        assertEquals(EstadoVenta.PAGADA, ventaCreditoPagada.getEstado());
        TransaccionFinanciera trxCredito = transaccionRepository.findFirstByNumeroComprobante(ventaCredito.getNumeroTicket())
                .orElseThrow();
        assertEquals(EstadoFinanciero.PAGADO, trxCredito.getEstado());

        // Caso C Rollback: Pagar una venta ANULADA debe revertir la transacción financiera
        VentaRequest reqAnulada = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.CREDITO)
                .clienteNombre("Cliente Venta Anulada")
                .operacionId("OP-PAG-ANU-" + sufijo)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 1)))
                .build();
        VentaResponse ventaAnular = ventaService.crear(reqAnulada, "vendedor-anu");
        ventasCreadas.add(ventaAnular.getId());
        ticketsCreados.add(ventaAnular.getNumeroTicket());
        ventaService.anular(ventaAnular.getId(), "supervisor-anu");

        String codTrxFail = "TRX-PAG-FAIL-" + sufijo;
        Long catId = cat.getId();
        assertThrows(VentaYaAnuladaException.class, () -> finanzasService.crearTransaccion(
                TransaccionFinancieraRequest.builder()
                        .codigo(codTrxFail)
                        .concepto("Intento cobro venta anulada")
                        .monto(new BigDecimal("25.00"))
                        .fechaTransaccion(LocalDate.now())
                        .tipo(TipoFinanciero.INGRESO)
                        .estado(EstadoFinanciero.PAGADO)
                        .categoriaFinancieraId(catId)
                        .ventaId(ventaAnular.getId())
                        .build()
        ));
        assertTrue(transaccionRepository.findByCodigo(codTrxFail).isEmpty(),
                "La transacción financiera debe revertirse si Ventas rechaza el pago por estar ANULADA");
    }

    @Test
    void workflowInventarioAFinanzasRegistraApuntesContablesDeMermasYAjustes() {
        Producto producto = crearProducto("40.0000", 40, TipoProducto.MATERIA_PRIMA, "12.50");

        // 1. Merma / ajuste negativo (-4 unidades * S/ 12.50 = EGRESO S/ 50.00)
        Long mermaId = inventarioOperacionService.registrarAjusteInventario(
                producto.getId(),
                new BigDecimal("-4"),
                "Merma por humedad en almacén"
        );
        assertNotNull(mermaId);
        comprobantesCreados.add("AJU-" + mermaId);

        Producto trasMerma = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("36.0000").compareTo(trasMerma.getStockTotal()));

        TransaccionFinanciera trxMerma = transaccionRepository.findFirstByNumeroComprobante("AJU-" + mermaId)
                .orElseThrow();
        assertEquals(TipoFinanciero.EGRESO, trxMerma.getTipo());
        assertEquals(EstadoFinanciero.PAGADO, trxMerma.getEstado());
        assertEquals(0, new BigDecimal("50.00").compareTo(trxMerma.getMonto()));
        assertEquals("MERMA_INVENTARIO", trxMerma.getTipoComprobante());
        assertTrue(trxMerma.getConcepto().contains("Merma por humedad"));

        // 2. Ajuste positivo (+2 unidades * S/ 12.50 = INGRESO S/ 25.00)
        Long gananciaId = inventarioOperacionService.registrarAjusteInventario(
                producto.getId(),
                new BigDecimal("2"),
                "Sobrante encontrado en inventario físico"
        );
        assertNotNull(gananciaId);
        comprobantesCreados.add("AJU-" + gananciaId);

        Producto trasAjustePos = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("38.0000").compareTo(trasAjustePos.getStockTotal()));

        TransaccionFinanciera trxGanancia = transaccionRepository.findFirstByNumeroComprobante("AJU-" + gananciaId)
                .orElseThrow();
        assertEquals(TipoFinanciero.INGRESO, trxGanancia.getTipo());
        assertEquals(EstadoFinanciero.PAGADO, trxGanancia.getEstado());
        assertEquals(0, new BigDecimal("25.00").compareTo(trxGanancia.getMonto()));
        assertEquals("AJUSTE_INVENTARIO", trxGanancia.getTipoComprobante());
    }

    private Producto crearProducto(String stock, int cantidadLote, TipoProducto tipoProducto, String costo) {
        Almacen alm = new Almacen();
        alm.setNombre("ALM-WF-" + sufijo + "-" + almacenesCreados.size());
        alm.setUbicacion("Planta Principal");
        alm = almacenRepository.save(alm);
        almacenesCreados.add(alm.getId());

        Categoria cat = new Categoria();
        cat.setNombre("CAT-WF-" + sufijo + "-" + categoriasCreadas.size());
        cat.setDescripcion("Categoría test workflows");
        cat = categoriaRepository.save(cat);
        categoriasCreadas.add(cat.getId());

        Producto prod = new Producto();
        prod.setSku("SKU-WF-" + sufijo + "-" + productosCreados.size());
        prod.setNombre("Producto Workflow " + sufijo + "-" + productosCreados.size());
        prod.setUnidadMedida("unidad");
        prod.setCostoPromedio(new BigDecimal(costo));
        prod.setStockTotal(new BigDecimal(stock));
        prod.setStockMinimo(BigDecimal.ONE);
        prod.setTipoProducto(tipoProducto);
        prod.setAlmacen(alm);
        prod.setCategoria(cat);
        prod = productoRepository.save(prod);
        productosCreados.add(prod.getId());

        if (cantidadLote > 0) {
            Lote lote = new Lote();
            lote.setProducto(prod);
            lote.setNumeroLote("LT-WF-" + sufijo + "-" + productosCreados.size());
            lote.setCantidadActual(cantidadLote);
            lote.setFechaProduccion(LocalDate.now().minusDays(1));
            lote.setFechaVencimiento(LocalDate.now().plusYears(1));
            loteRepository.save(lote);
        }

        return prod;
    }

    private CentroTrabajo crearCentroTrabajo() {
        CentroTrabajo centro = new CentroTrabajo();
        centro.setNombre("CT-WF-" + sufijo + "-" + centrosCreados.size());
        centro.setCodigo("CT-" + sufijo + "-" + centrosCreados.size());
        centro.setCostoHora(new BigDecimal("15.00"));
        centro = centroTrabajoRepository.save(centro);
        centrosCreados.add(centro.getId());
        return centro;
    }
}
