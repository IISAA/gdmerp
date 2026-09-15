package pe.edu.upeu.gdmerp.produccion.ordenproduccion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrdenProduccionTransactionTest {

    @Autowired private OrdenProduccionService ordenService;
    @Autowired private OrdenProduccionRepository ordenRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private LoteRepository loteRepository;
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
        productoConStock("Insumo-Principal", costoInsumo, stockInsumoAntes,
                lote(5, LocalDate.of(2026, 12, 1)),
                lote(3, LocalDate.of(2027, 3, 1)));
        productoT = producto("Producto-Terminado", new BigDecimal("50.00"), 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-" + sufijo);
        centro.setCapacidad(10);
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
        Producto insumoBd = productoRepository.findById(insumo.getId()).orElseThrow();
        assertEquals(2, insumoBd.getStockTotal(), "stock insumo debe bajar de 8 a 2");

        List<Lote> lotesInsumo = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(insumo.getId(), 0);
        // FEFO: el lote que vence antes (2026-12-01) se consume primero por completo.
        assertEquals(1, lotesInsumo.size(), "FEFO deja solo el lote de vencimiento posterior con resto");
        assertEquals(LocalDate.of(2027, 3, 1), lotesInsumo.get(0).getFechaVencimiento());
        // 6 consumidos = 5 del lote viejo + 1 del nuevo -> el lote nuevo queda en 2.
        assertEquals(2, lotesInsumo.get(0).getCantidadActual());

        Producto productoTBd = productoRepository.findById(productoT.getId()).orElseThrow();
        assertEquals(cantidadPlanes, productoTBd.getStockTotal(), "ingreso de producto terminado");
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
        productoConStock("Insumo-Escaso", new BigDecimal("10.00"), stockInsuficiente,
                lote(stockInsuficiente, LocalDate.of(2026, 12, 1)));
        productoT = producto("Producto-Terminado-RB", new BigDecimal("50.00"), 0);
        centro = new CentroTrabajo();
        centro.setNombre("CT-RB-" + sufijo);
        centro.setCapacidad(10);
        centro = centroRepository.save(centro);

        Producto insumoAntes = productoRepository.findById(insumo.getId()).orElseThrow();
        Lote loteInsumoAntes = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), 0).get(0);
        int stockAntes = insumoAntes.getStockTotal();
        int cantidadLoteAntes = loteInsumoAntes.getCantidadActual();
        int stockPtoAntes = productoRepository.findById(productoT.getId()).orElseThrow().getStockTotal();
        int lotesPtoAntes = loteRepository.findAll().size();

        // La regla de negocio se activa: cantidad demánda (5) > stock disponible (2)
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenService.crear(ordenRequest(productoT.getId(), 4, cantidadPedida)));
        assertTrue(ex.getMessage().contains("Stock insuficiente"), ex.getMessage());

        // Estado DESPUÉS: nada debe haber cambiado -> rollback total de la transacción.
        Producto insumoDespues = productoRepository.findById(insumo.getId()).orElseThrow();
        assertEquals(stockAntes, insumoDespues.getStockTotal(), "stock del insumo no debe cambiar");

        Lote loteDespues = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
                insumo.getId(), 0).get(0);
        assertEquals(cantidadLoteAntes, loteDespues.getCantidadActual(), "ningún lote puede consumirse");

        assertEquals(stockPtoAntes, productoRepository.findById(productoT.getId()).orElseThrow().getStockTotal(),
                "no debe ingresar producto terminado");
        assertEquals(lotesPtoAntes, loteRepository.findAll().size(), "no debe crearse lote de ingreso");

        assertEquals(ordenesAntes, ordenRepository.count(), "no debe persistirse la cabecera (ni el detalle)");
    }

    // ---------- Helpers ----------

    private OrdenProduccionRequest ordenRequest(Long productoTerminadoId, int cantidadPlanificada, int cantidadRequerida) {
        return new OrdenProduccionRequest(
                productoTerminadoId,
                "Cacao Procesado",
                cantidadPlanificada,
                "FINALIZADA",
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
            // Borra cualquier orden de producción quedada para este centro en este caso
            // (aunque el test haya fallado antes de asignar el id de la orden).
            ordenRepository.deleteAll(ordenRepository.findByCentroTrabajoId(centro.getId()));
            centroRepository.delete(centro);
            centro = null;
        }
        if (productoT != null) {
            // Borrar primero los lotes por repositorio (evita cascadas/merge sobre el
            // producto detached) y luego el producto con su versión vigente.
            loteRepository.deleteAll(loteRepository.findByProductoId(productoT.getId()));
            productoRepository.deleteById(productoT.getId());
            productoT = null;
        }
        if (insumo != null) {
            loteRepository.deleteAll(loteRepository.findByProductoId(insumo.getId()));
            // El stock del insumo se consumió en el test: su @Version subió en la BD,
            // por lo que se borra recargando la fila actual (deleteById) y no la imagen
            // en memoria (delete), que lanzaría StaleObjectStateException.
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