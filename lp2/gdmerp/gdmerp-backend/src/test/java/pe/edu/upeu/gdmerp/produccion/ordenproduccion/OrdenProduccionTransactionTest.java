package pe.edu.upeu.gdmerp.produccion.ordenproduccion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
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
@WithMockUser(roles = { "ADMIN", "JEFE_PRODUCCION", "ALMACENERO" })
class OrdenProduccionTransactionTest {

    @Autowired
    private OrdenProduccionService ordenService;
    @Autowired
    private OrdenProduccionRepository ordenRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private LoteRepository loteRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private CentroTrabajoRepository centroRepository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

    private Almacen almacen;
    private Categoria categoria;
    private CentroTrabajo centro;
    private Producto insumo;
    private Producto productoT;

    @Test
    void crearOrdenDescuentaStockYCalculaTotales() {
        long ordenesAntes = ordenRepository.count();

        BigDecimal costoInsumo = new BigDecimal("10.00");
        int stockInsumoAntes = 8;
        productoConStock("Insumo-Principal", costoInsumo, stockInsumoAntes,
                lote(5, LocalDate.of(2026, 12, 1)),
                lote(3, LocalDate.of(2027, 3, 1)));
        productoT = producto("Producto-Terminado", new BigDecimal("50.00"), 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-" + sufijo);
        centro.setCapacidad(BigDecimal.valueOf(10));
        centro.setCostoHora(new BigDecimal("25.00"));
        centro = centroRepository.save(centro);

        int cantidadInsumo = 6;
        int cantidadPlanes = 4;

        OrdenProduccionResponse resp = ordenService
                .crear(ordenRequest(productoT.getId(), cantidadPlanes, cantidadInsumo));

        assertNotNull(resp.id());
        assertNotNull(resp.loteGenerado());
        assertEquals(1, resp.detalles().size());
        assertBigDecimalEquals(costoInsumo, resp.detalles().get(0).costoUnitario());
        assertBigDecimalEquals(new BigDecimal("60.00"), resp.detalles().get(0).subtotal());
        assertBigDecimalEquals(new BigDecimal("60.00"), resp.totalCostoInsumos());

        Producto insumoBd = productoRepository.findById(insumo.getId()).orElseThrow();
        assertBigDecimalEquals(BigDecimal.valueOf(2), insumoBd.getStockTotal());

        List<Lote> lotesInsumo = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(insumo.getId(),
                        BigDecimal.ZERO);
        assertEquals(1, lotesInsumo.size(), "FEFO deja solo el lote de vencimiento posterior con resto");
        assertEquals(LocalDate.of(2027, 3, 1), lotesInsumo.get(0).getFechaVencimiento());
        assertBigDecimalEquals(BigDecimal.valueOf(2), lotesInsumo.get(0).getCantidadActual());

        Producto productoTBd = productoRepository.findById(productoT.getId()).orElseThrow();
        assertBigDecimalEquals(BigDecimal.valueOf(cantidadPlanes), productoTBd.getStockTotal());
        assertEquals(1, loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                productoT.getId(), BigDecimal.ZERO).size());

        assertEquals(ordenesAntes + 1, ordenRepository.count());
    }

    @Test
    void stockInsuficienteProduceRollbackTotal() {
        long ordenesAntes = ordenRepository.count();
        int stockInsuficiente = 2;
        int cantidadPedida = 5;
        productoConStock("Insumo-Escaso", new BigDecimal("10.00"), stockInsuficiente,
                lote(stockInsuficiente, LocalDate.of(2026, 12, 1)));
        productoT = producto("Producto-Terminado-RB", new BigDecimal("50.00"), 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-RB-" + sufijo);
        centro.setCapacidad(BigDecimal.valueOf(10));
        centro.setCostoHora(new BigDecimal("25.00"));
        centro = centroRepository.save(centro);

        Producto insumoAntes = productoRepository.findById(insumo.getId()).orElseThrow();
        Lote loteInsumoAntes = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), BigDecimal.ZERO).get(0);
        BigDecimal stockAntes = insumoAntes.getStockTotal();
        BigDecimal cantidadLoteAntes = loteInsumoAntes.getCantidadActual();
        BigDecimal stockPtoAntes = productoRepository.findById(productoT.getId()).orElseThrow().getStockTotal();
        int lotesPtoAntes = loteRepository.findAll().size();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ordenService.crear(ordenRequest(productoT.getId(), 4, cantidadPedida)));
        assertTrue(ex.getReason() != null && ex.getReason().contains("Stock insuficiente"), ex.getMessage());

        Producto insumoDespues = productoRepository.findById(insumo.getId()).orElseThrow();
        assertBigDecimalEquals(stockAntes, insumoDespues.getStockTotal());

        Lote loteDespues = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), BigDecimal.ZERO).get(0);
        assertBigDecimalEquals(cantidadLoteAntes, loteDespues.getCantidadActual());

        assertBigDecimalEquals(stockPtoAntes,
                productoRepository.findById(productoT.getId()).orElseThrow().getStockTotal());
        assertEquals(lotesPtoAntes, loteRepository.findAll().size(), "no debe crearse lote de ingreso");

        assertEquals(ordenesAntes, ordenRepository.count(), "no debe persistirse la cabecera (ni el detalle)");
    }

    private OrdenProduccionRequest ordenRequest(Long productoTerminadoId, int cantidadPlanificada,
            int cantidadRequerida) {
        return new OrdenProduccionRequest(
                productoTerminadoId,
                cantidadPlanificada,
                EstadoOrdenProduccion.PLANIFICADO,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2),
                centro.getId(),
                List.of(new DetalleOrdenRequest(insumo.getId(), cantidadRequerida)));
    }

    private void productoConStock(String nombreProducto, BigDecimal costo, int stockTotal, Lote... lotes) {
        insumo = producto(nombreProducto, costo, stockTotal);
        for (Lote lote : lotes) {
            lote.setProducto(insumo);
            loteRepository.save(lote);
        }
    }

    private Producto producto(String nombreProducto, BigDecimal costo, int stockTotal) {
        garantizarAlmacenYCategoria();

        Producto p = new Producto();
        p.setSku("SKU-" + nombreProducto.substring(0, 1).toUpperCase() + "-" + sufijo);
        p.setNombre(nombreProducto + "-" + sufijo);
        p.setUnidadMedida("kg");
        p.setCostoPromedio(costo);
        p.setStockTotal(stockTotal);
        p.setStockMinimo(1);
        p.setAlmacen(almacen);
        p.setCategoria(categoria);
        return productoRepository.save(p);
    }

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
        l.setCantidadActual(BigDecimal.valueOf(cantidad));
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
            loteRepository.deleteAll(loteRepository.findByProductoId(productoT.getId()));
            productoRepository.deleteById(productoT.getId());
            productoT = null;
        }
        if (insumo != null) {
            loteRepository.deleteAll(loteRepository.findByProductoId(insumo.getId()));
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