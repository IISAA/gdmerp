package pe.edu.upeu.gdmerp.produccion.ordenproduccion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "JEFE_PRODUCCION", "ALMACENERO"})
class OrdenProduccionTransactionTest {

    @Autowired private OrdenProduccionService ordenService;
    @Autowired private OrdenProduccionRepository ordenRepository;
    @Autowired @Qualifier("catalogoProductoRepository") private ProductoRepository productoRepository;
    @Autowired private StockInventarioRepository stockRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private CentroTrabajoRepository centroRepository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

    private Almacen almacen;
    private Categoria categoria;
    private CentroTrabajo centro;
    private Producto insumo;
    private Producto productoT;

    // ---------- CASO DE ÉXITO: la transacción persiste cabecera+detalle, descuenta stock
    // ---------- (FEFO) e ingresa el producto terminado, con totales calculados.
    @Test
    void crearOrdenDescuentaStockYCalculaTotales() {
        long ordenesAntes = ordenRepository.count();

        // Estado ANTES (datos que el test crea y deja listos antes de la operación)
        BigDecimal costoInsumo = new BigDecimal("10.00");
        int stockInsumoAntes = 8;
        productoConStock("Insumo-Principal", costoInsumo, TipoProducto.MATERIA_PRIMA, stockInsumoAntes,
                lote(5, LocalDate.of(2026, 12, 1)),
                lote(3, LocalDate.of(2027, 3, 1)));
        productoT = productoConFichaStock("Producto-Terminado", new BigDecimal("50.00"), TipoProducto.TERMINADO, 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-" + sufijo);
        centro.setCapacidad(BigDecimal.valueOf(10));
        centro.setCostoHora(new BigDecimal("25.00"));
        centro = centroRepository.save(centro);

        int cantidadInsumo = 6;
        int cantidadPlanes = 4;

        OrdenProduccionResponse resp = ordenService.crear(ordenRequest(productoT.getId(), cantidadPlanes, cantidadInsumo));

        // Validación del cálculo de totales (cabecera-detalle)
        assertNotNull(resp.id());
        assertNotNull(resp.loteGenerado());
        assertEquals(1, resp.detalles().size());
        assertBigDecimalEquals(costoInsumo, resp.detalles().get(0).costoUnitario());
        assertBigDecimalEquals(new BigDecimal("60.00"), resp.detalles().get(0).subtotal()); // 6 x 10
        assertBigDecimalEquals(new BigDecimal("60.00"), resp.totalCostoInsumos()); // suma de subtotales

        // Estado DESPUÉS: recarga fresca de la BD
        StockInventario insumoBd = stockRepository.findFirstByProductoIdOrderByIdAsc(insumo.getId()).orElseThrow();
        assertBigDecimalEquals(BigDecimal.valueOf(2), insumoBd.getStockActual());

        List<Lote> lotesInsumo = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(insumo.getId(), 0);
        // FEFO: el lote que vence antes (2026-12-01) se consume primero por completo.
        assertEquals(1, lotesInsumo.size(), "FEFO deja solo el lote de vencimiento posterior con resto");
        assertEquals(LocalDate.of(2027, 3, 1), lotesInsumo.get(0).getFechaVencimiento());
        // 6 consumidos = 5 del lote viejo + 1 del nuevo -> el lote nuevo queda en 2.
        assertEquals(2, lotesInsumo.get(0).getCantidadActual());

        StockInventario productoTBd = stockRepository.findFirstByProductoIdOrderByIdAsc(productoT.getId()).orElseThrow();
        assertBigDecimalEquals(BigDecimal.valueOf(cantidadPlanes), productoTBd.getStockActual());
        assertEquals(1, loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                productoT.getId(), 0).size());

        // La cabecera y sus detalles quedaron persistidos (probados ya por el response `resp`).
        assertEquals(ordenesAntes + 1, ordenRepository.count());
    }

    // ---------- CASO DE ROLLBACK: stock insuficiente => la transacción NO persiste nada.
    @Test
    void stockInsuficienteProduceRollbackTotal() {
        long ordenesAntes = ordenRepository.count();
        int stockInsuficiente = 2;
        int cantidadPedida = 5;
        productoConStock("Insumo-Escaso", new BigDecimal("10.00"), TipoProducto.MATERIA_PRIMA, stockInsuficiente,
                lote(stockInsuficiente, LocalDate.of(2026, 12, 1)));
        productoT = productoConFichaStock("Producto-Terminado-RB", new BigDecimal("50.00"), TipoProducto.TERMINADO, 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-RB-" + sufijo);
        centro.setCapacidad(BigDecimal.valueOf(10));
        centro.setCostoHora(new BigDecimal("25.00"));
        centro = centroRepository.save(centro);

        StockInventario insumoAntes = stockRepository.findFirstByProductoIdOrderByIdAsc(insumo.getId()).orElseThrow();
        Lote loteInsumoAntes = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), 0).get(0);
        BigDecimal stockAntes = insumoAntes.getStockActual();
        int cantidadLoteAntes = loteInsumoAntes.getCantidadActual();
        BigDecimal stockPtoAntes = stockRepository.findFirstByProductoIdOrderByIdAsc(productoT.getId())
                .orElseThrow().getStockActual();
        int lotesPtoAntes = loteRepository.findAll().size();

        // La regla de negocio se activa: cantidad demandada (5) > stock disponible (2)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ordenService.crear(ordenRequest(productoT.getId(), 4, cantidadPedida)));
        assertTrue(ex.getReason() != null && ex.getReason().contains("Stock insuficiente"), ex.getMessage());

        // Estado DESPUÉS: nada debe haber cambiado -> rollback total de la transacción.
        StockInventario insumoDespues = stockRepository.findFirstByProductoIdOrderByIdAsc(insumo.getId()).orElseThrow();
        assertBigDecimalEquals(stockAntes, insumoDespues.getStockActual());

        Lote loteDespues = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), 0).get(0);
        assertEquals(cantidadLoteAntes, loteDespues.getCantidadActual(), "ningún lote puede consumirse");

        assertBigDecimalEquals(stockPtoAntes,
                stockRepository.findFirstByProductoIdOrderByIdAsc(productoT.getId()).orElseThrow().getStockActual());
        assertEquals(lotesPtoAntes, loteRepository.findAll().size(), "no debe crearse lote de ingreso");

        assertEquals(ordenesAntes, ordenRepository.count(), "no debe persistirse la cabecera (ni el detalle)");
    }

    // ---------- Helpers ----------

    private OrdenProduccionRequest ordenRequest(Long productoTerminadoId, int cantidadPlanificada, int cantidadRequerida) {
        return new OrdenProduccionRequest(
                productoTerminadoId,
                cantidadPlanificada,
                EstadoOrdenProduccion.PLANIFICADO,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2),
                centro.getId(),
                List.of(new DetalleOrdenRequest(insumo.getId(), cantidadRequerida)));
    }

    private void productoConStock(String nombreProducto, BigDecimal costo, TipoProducto tipo, int stockTotal, Lote... lotes) {
        insumo = productoConFichaStock(nombreProducto, costo, tipo, stockTotal);
        for (Lote lote : lotes) {
            lote.setProductoId(insumo.getId());
            loteRepository.save(lote);
        }
    }

    private Producto productoConFichaStock(String nombreProducto, BigDecimal costo, TipoProducto tipo, int stockTotal) {
        garantizarAlmacenYCategoria();

        Producto p = Producto.builder()
                .sku("SKU-" + nombreProducto.substring(0, 1).toUpperCase() + "-" + sufijo)
                .nombre(nombreProducto + "-" + sufijo)
                .unidadMedida("kg")
                .precio(costo)
                .costoBase(costo)
                .tipoProducto(tipo)
                .stockMinimo(BigDecimal.ONE)
                .stock(0)
                .activo(Boolean.TRUE)
                .build();
        p = productoRepository.save(p);

        StockInventario stock = new StockInventario();
        stock.setProductoId(p.getId());
        stock.setAlmacen(almacen);
        stock.setCategoriaId(categoria.getId());
        stock.setCostoPromedio(costo);
        stock.setStockActual(BigDecimal.valueOf(stockTotal));
        stock.setStockMinimo(BigDecimal.ONE);
        stockRepository.save(stock);

        return p;
    }

    // Almacén y categoría se crean UNA sola vez por caso de prueba y se reutilizan
    // por todos los productos del caso (evita violar la restricción única de NOMBRE).
    private void garantizarAlmacenYCategoria() {
        if (almacen != null && categoria != null) {
            return;
        }
        almacen = new Almacen();
        almacen.setNombre("ALM-" + sufijo);
        almacen.setUbicacion("Lima");
        almacen = almacenRepository.save(almacen);

        categoria = new Categoria();
        categoria.setNombre("CAT-" + sufijo);
        categoria.setDescripcion("Creacion de test");
        categoria = categoriaRepository.save(categoria);
    }

    private Lote lote(int cantidad, LocalDate vencimiento) {
        Lote l = new Lote();
        l.setNumeroLote("LT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        l.setCantidadActual(cantidad);
        l.setFechaProduccion(LocalDate.now());
        l.setFechaVencimiento(vencimiento);
        return l;
    }

    @AfterEach
    void limpiarDatosDeTest() {
        if (centro != null) {
            ordenRepository.deleteAll(ordenRepository.findByCentroTrabajoId(centro.getId()));
            centroRepository.delete(centro);
            centro = null;
        }
        if (productoT != null) {
            movimientoRepository.deleteAll(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoT.getId()));
            loteRepository.deleteAll(loteRepository.findByProductoId(productoT.getId()));
            stockRepository.findFirstByProductoIdOrderByIdAsc(productoT.getId()).ifPresent(stockRepository::delete);
            productoRepository.deleteById(productoT.getId());
            productoT = null;
        }
        if (insumo != null) {
            movimientoRepository.deleteAll(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(insumo.getId()));
            loteRepository.deleteAll(loteRepository.findByProductoId(insumo.getId()));
            stockRepository.findFirstByProductoIdOrderByIdAsc(insumo.getId()).ifPresent(stockRepository::delete);
            productoRepository.deleteById(insumo.getId());
            insumo = null;
        }
        if (almacen != null) {
            almacenRepository.delete(almacen);
            almacen = null;
        }
        if (categoria != null) {
            categoriaRepository.delete(categoria);
            categoria = null;
        }
    }

    private static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual, "Valor BigDecimal no debe ser nulo");
        assertTrue(expected.compareTo(actual) == 0,
                "Se esperaba " + expected + " pero fue " + actual);
    }
}