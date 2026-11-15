package pe.edu.upeu.gdmerp.shared.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.stock.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;
import pe.edu.upeu.gdmerp.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.gdmerp.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.MetodoPagoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.repository.VentaRepository;
import pe.edu.upeu.gdmerp.ventas.venta.service.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "VENDEDOR", "SUPERVISOR", "ALMACENERO"})
class TrianguloComercialIntegrationTest {

    @Autowired private VentaService ventaService;
    @Autowired private VentaRepository ventaRepository;
    @Autowired @Qualifier("catalogoProductoRepository") private ProductoRepository productoRepository;
    @Autowired private StockInventarioRepository stockRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private CuentaPorCobrarRepository cuentaRepository;
    @Autowired private TransaccionFinancieraRepository transaccionRepository;
    @Autowired private SesionCajaRepository sesionCajaRepository;

    private String sufijo;
    private final List<Long> ventasCreadas = new ArrayList<>();
    private final List<String> ticketsCreados = new ArrayList<>();
    private final List<Long> stocksCreados = new ArrayList<>();
    private final List<Long> productosCreados = new ArrayList<>();
    private final List<Long> almacenesCreados = new ArrayList<>();
    private final List<Long> categoriasCreadas = new ArrayList<>();
    private final List<Long> sesionesCreadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        for (String ticket : ticketsCreados) {
            transaccionRepository.findFirstByNumeroComprobante(ticket)
                    .ifPresent(transaccionRepository::delete);
            cuentaRepository.findByCodigo("CRE-" + ticket)
                    .ifPresent(cuentaRepository::delete);
        }
        ticketsCreados.clear();

        for (Long ventaId : ventasCreadas) {
            if (ventaRepository.existsById(ventaId)) {
                ventaRepository.deleteById(ventaId);
            }
        }
        ventasCreadas.clear();

        for (Long sesionId : sesionesCreadas) {
            if (sesionCajaRepository.existsById(sesionId)) {
                sesionCajaRepository.deleteById(sesionId);
            }
        }
        sesionesCreadas.clear();

        for (Long prodId : productosCreados) {
            movimientoRepository.deleteAll(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(prodId));
            loteRepository.deleteAll(loteRepository.findByProductoId(prodId));
        }
        for (Long stockId : stocksCreados) {
            if (stockRepository.existsById(stockId)) {
                stockRepository.deleteById(stockId);
            }
        }
        stocksCreados.clear();

        for (Long prodId : productosCreados) {
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
    void ventaACreditoSincronizaInventarioYFinanzasYAnulacionRevierteTodo() {
        Producto producto = crearProductoConLote("50.0000", 50);

        VentaRequest request = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.CREDITO)
                .clienteNombre("Distribuidora Comercial SAC")
                .operacionId("OP-TRI-" + sufijo)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 4)))
                .build();

        VentaResponse creada = ventaService.crear(request, "vendedor-triangulo");
        assertNotNull(creada.getId());
        ventasCreadas.add(creada.getId());
        ticketsCreados.add(creada.getNumeroTicket());

        assertEquals(EstadoVenta.REGISTRADA, creada.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(creada.getTotal()));

        // Inventario: stock, lote y Kardex actualizados en BEFORE_COMMIT
        StockInventario trasVenta = stockRepository.findFirstByProductoIdOrderByIdAsc(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("46.0000").compareTo(trasVenta.getStockActual()));
        Lote loteTrasVenta = loteRepository.findByProductoId(producto.getId()).get(0);
        assertEquals(46, loteTrasVenta.getCantidadActual());

        List<MovimientoInventario> movimientosVenta =
                movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(producto.getId());
        assertEquals(1, movimientosVenta.size());
        assertEquals("SALIDA_VENTA", movimientosVenta.get(0).getTipoMovimiento());
        assertEquals("VENTA:" + creada.getNumeroTicket(), movimientosVenta.get(0).getReferencia());

        // Finanzas: Cuenta por cobrar y transacción registradas en BEFORE_COMMIT
        CuentaPorCobrar cxc = cuentaRepository.findByCodigo("CRE-" + creada.getNumeroTicket()).orElseThrow();
        assertEquals(EstadoCredito.PENDIENTE, cxc.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(cxc.getMontoTotal()));

        TransaccionFinanciera trx = transaccionRepository.findFirstByNumeroComprobante(creada.getNumeroTicket())
                .orElseThrow();
        assertEquals(EstadoFinanciero.PENDIENTE, trx.getEstado());

        // Anulación: revierte inventario + lote + kardex + finanzas
        VentaResponse anulada = ventaService.anular(creada.getId(), "supervisor-triangulo");
        assertEquals(EstadoVenta.ANULADA, anulada.getEstado());

        StockInventario trasAnulacion = stockRepository.findFirstByProductoIdOrderByIdAsc(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.0000").compareTo(trasAnulacion.getStockActual()));
        Lote loteTrasAnulacion = loteRepository.findByProductoId(producto.getId()).get(0);
        assertEquals(50, loteTrasAnulacion.getCantidadActual());

        List<MovimientoInventario> movimientosPostAnulacion =
                movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(producto.getId());
        assertEquals(2, movimientosPostAnulacion.size());
        assertEquals("ENTRADA_ANULACION_VENTA", movimientosPostAnulacion.get(0).getTipoMovimiento());
        assertEquals("ANULACION:" + creada.getNumeroTicket(), movimientosPostAnulacion.get(0).getReferencia());

        assertFalse(cuentaRepository.findByCodigo("CRE-" + creada.getNumeroTicket()).isPresent());
        TransaccionFinanciera trxAnulada = transaccionRepository.findFirstByNumeroComprobante(creada.getNumeroTicket())
                .orElseThrow();
        assertEquals(EstadoFinanciero.ANULADO, trxAnulada.getEstado());
    }

    @Test
    void ventaConStockInsuficienteRevierteVentaYFinanzasAtomicamante() {
        Producto producto = crearProductoConLote("5.0000", 5);
        String opId = "OP-NOSTOCK-" + sufijo;
        long totalVentasAntes = ventaRepository.count();
        long totalCuentasAntes = cuentaRepository.count();

        VentaRequest request = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.CREDITO)
                .clienteNombre("Cliente Rollback Stock")
                .operacionId(opId)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 20)))
                .build();

        assertThrows(InsufficientStockException.class, () -> ventaService.crear(request, "vendedor-rollback"));

        assertTrue(ventaRepository.findByOperacionId(opId).isEmpty(),
                "La Venta debe revertirse completamente si Inventario falla por stock insuficiente");
        assertEquals(totalVentasAntes, ventaRepository.count());
        assertEquals(totalCuentasAntes, cuentaRepository.count(),
                "Finanzas tampoco debe persistir CuentaPorCobrar cuando Inventario hace rollback");

        StockInventario intacto = stockRepository.findFirstByProductoIdOrderByIdAsc(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5.0000").compareTo(intacto.getStockActual()));
        assertTrue(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(producto.getId()).isEmpty());
    }

    @Test
    void ventaEnEfectivoConSesionCajaCerradaRevierteDescuentoDeInventario() {
        Producto producto = crearProductoConLote("15.0000", 15);
        SesionCaja sesionCerrada = sesionCajaRepository.save(SesionCaja.builder()
                .codigoSesion("SC-CERR-" + sufijo)
                .usuarioId(999L)
                .cajero("cajero-test")
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .fechaCierre(LocalDateTime.now().minusHours(1))
                .saldoInicial(BigDecimal.ZERO)
                .estado(EstadoCaja.CERRADA)
                .build());
        sesionesCreadas.add(sesionCerrada.getId());

        String opId = "OP-FINFAIL-" + sufijo;
        VentaRequest request = VentaRequest.builder()
                .metodoPago(MetodoPagoVenta.EFECTIVO)
                .clienteNombre("Cliente Rollback Finanzas")
                .sesionCajaId(sesionCerrada.getId())
                .operacionId(opId)
                .detalles(List.of(new DetalleVentaRequest(producto.getId(), null, 3)))
                .build();

        assertThrows(BusinessConflictException.class, () -> ventaService.crear(request, "vendedor-rollback"));

        assertTrue(ventaRepository.findByOperacionId(opId).isEmpty(),
                "El fallo en Finanzas (BEFORE_COMMIT) debe revertir la Venta");
        StockInventario stockIntacto = stockRepository.findFirstByProductoIdOrderByIdAsc(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("15.0000").compareTo(stockIntacto.getStockActual()),
                "El fallo en Finanzas debe revertir también el descuento de stock/lote de Inventario");
        Lote loteIntacto = loteRepository.findByProductoId(producto.getId()).get(0);
        assertEquals(15, loteIntacto.getCantidadActual());
        assertTrue(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(producto.getId()).isEmpty());
    }

    private Producto crearProductoConLote(String stockInicial, int cantidadLote) {
        Almacen alm = new Almacen();
        alm.setNombre("ALM-TRI-" + sufijo + "-" + almacenesCreados.size());
        alm.setUbicacion("Planta Venta");
        alm = almacenRepository.save(alm);
        almacenesCreados.add(alm.getId());

        Categoria cat = new Categoria();
        cat.setNombre("CAT-TRI-" + sufijo + "-" + categoriasCreadas.size());
        cat.setDescripcion("Categoria triangulo comercial");
        cat = categoriaRepository.save(cat);
        categoriasCreadas.add(cat.getId());

        Producto prod = Producto.builder()
                .sku("SKU-TRI-" + sufijo + "-" + productosCreados.size())
                .nombre("Producto Comercial " + sufijo)
                .unidadMedida("unidad")
                .precio(new BigDecimal("25.00"))
                .costoBase(new BigDecimal("25.0000"))
                .tipoProducto(TipoProducto.TERMINADO)
                .stockMinimo(BigDecimal.ONE)
                .stock(0)
                .activo(Boolean.TRUE)
                .build();
        prod = productoRepository.save(prod);
        productosCreados.add(prod.getId());

        StockInventario stock = new StockInventario();
        stock.setProductoId(prod.getId());
        stock.setAlmacen(alm);
        stock.setCategoriaId(cat.getId());
        stock.setCostoPromedio(new BigDecimal("25.0000"));
        stock.setStockActual(new BigDecimal(stockInicial));
        stock.setStockMinimo(BigDecimal.ONE);
        stock = stockRepository.save(stock);
        stocksCreados.add(stock.getId());

        Lote lote = new Lote();
        lote.setProductoId(prod.getId());
        lote.setNumeroLote("LT-TRI-" + sufijo);
        lote.setCantidadActual(cantidadLote);
        lote.setFechaProduccion(LocalDate.now().minusDays(2));
        lote.setFechaVencimiento(LocalDate.now().plusMonths(8));
        loteRepository.save(lote);

        return prod;
    }
}
