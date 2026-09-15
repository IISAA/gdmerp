package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el mecanismo de bloqueo optimista (@Version) que protege el stock:
 * 1) Una transacción que intenta grabar sobre una versión ya superada por otra
 *    transacción debe fallar con OptimisticLockingFailureException.
 * 2) Dos hilos compitiendo por el MISMO stock de un producto: exactamente UNO
 *    gana (no hay sobreventa/stock negativo).
 */
@SpringBootTest
class StockConcurrenciaIntegrationTest {

    @Autowired private InventarioOperacionService inventarioService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

    private Almacen almacen;
    private Categoria categoria;
    private Producto producto;
    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    // ---------- (1) PRUEBA DETERMINISTA: el UPDATE con versión vieja debe fallar ----------
    // Transacción A lee el producto (versión V1) y se mantiene abierta; otra transacción B
    // modifica y COMMITEA el mismo producto (versión V2). Cuando A intenta commitear su
    // escritura basada en V1, Hibernate emite "UPDATE ... WHERE VERSION = V1", no afecta
    // ninguna fila y lanza OptimisticLockingFailureException, con rollback automático.
    @Test
    void commitConVersionObsoletaLanzaOptimisticLockingFailure() throws Exception {
        crearProductoConStock(50, lote(50, LocalDate.of(2027, 12, 31)));
        assertEquals(0L, producto.getVersion(), "un producto recién insertado inicia en versión 0");

        TransactionStatus txA = transactionManager.getTransaction(new DefaultTransactionDefinition());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Producto manejadoEnA = productoRepository.findById(producto.getId()).orElseThrow();
            assertEquals(0L, manejadoEnA.getVersion());

            // Transacción B en OTRO hilo: incrementa stock del mismo producto y COMMITEA
            // (versión -> 1). No debe unirse a txA: por eso corre en hilo aparte.
            pool.submit(() -> txTemplate.executeWithoutResult(tx -> {
                Producto p = productoRepository.findById(producto.getId()).orElseThrow();
                p.setStockTotal(p.getStockTotal() + 1);
            })).get(30, TimeUnit.SECONDS);

            // A modifica su copia con la versión vieja y espera COMMIT -> debe colisionar.
            manejadoEnA.setStockTotal(manejadoEnA.getStockTotal() - 1);
            assertThrows(OptimisticLockingFailureException.class,
                    () -> transactionManager.commit(txA),
                    "el commit sobre una versión superada debe lanzar OptimisticLockingFailureException");
        } finally {
            pool.shutdownNow();
            // Si el commit llegó a fallar, la infraestructura ya hizo rollback y limpieza.
            if (txA != null && !txA.isCompleted()) {
                transactionManager.rollback(txA);
            }
        }

        Producto finalBd = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(51, finalBd.getStockTotal(), "solo el incremento de B debe persistir");
        assertEquals(1L, finalBd.getVersion(), "la versión en BD debe quedar en 1");
    }

    // ---------- (2) PRUEBA DE CONCURRENCIA REAL: dos hilos por el mismo stock ----------
    // Ambos hilos parten a la vez (CyclicBarrier) y piden el 100% del stock: el que
    // logre commitear consume; el otro colisiona por @Version (o ya no encuentra stock)
    // y queda fuera. Invariante: exactamente 1 éxito y stock final en cero.
    @Test
    void dosHilosCompitiendoPorElMismoStockPermitenUnSoloGanador() throws Exception {
        int stockTotal = 100;
        crearProductoConStock(stockTotal, lote(60, LocalDate.of(2027, 12, 31)));
        loteRepository.save(loteCon(producto, 40, LocalDate.of(2028, 6, 30)));

        CyclicBarrier barrera = new CyclicBarrier(2);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicReference<Throwable> errorDelPerdedor = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Runnable tarea = () -> {
            try {
                barrera.await(15, TimeUnit.SECONDS);
                inventarioService.consumirStockFefo(producto.getId(), stockTotal);
                exitosos.incrementAndGet();
            } catch (Throwable th) {
                errorDelPerdedor.set(th);
            }
        };

        Future<?> f1 = pool.submit(tarea);
        Future<?> f2 = pool.submit(tarea);
        f1.get(60, TimeUnit.SECONDS);
        f2.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, exitosos.get(), "exactamente UN hilo debe ganar el consumo");
        assertNotNull(errorDelPerdedor.get(), "el hilo perdedor debe terminar con una excepción");

        Producto finalBd = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(0, finalBd.getStockTotal(), "el stock total debe quedar en cero (sin sobreventa)");
        int lotesConStock = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(producto.getId(), 0).size();
        assertEquals(0, lotesConStock, "ningún lote puede conservar stock tras consumir el 100%");
    }

    // ---------- Helpers ----------

    private void crearProductoConStock(int stockTotal, Lote lote) {
        garantizarAlmacenYCategoria();
        producto = new Producto();
        producto.setSku("SKU-C-" + sufijo);
        producto.setNombre("Producto-Concurrencia-" + sufijo);
        producto.setUnidadMedida("kg");
        producto.setCostoPromedio(new java.math.BigDecimal("10.00"));
        producto.setStockTotal(stockTotal);
        producto.setStockMinimo(1);
        producto.setAlmacen(almacen);
        producto.setCategoria(categoria);
        producto = productoRepository.save(producto);

        lote.setProducto(producto);
        loteRepository.save(lote);
    }

    private Lote lote(int cantidad, LocalDate vencimiento) {
        return loteCon(null, cantidad, vencimiento);
    }

    private Lote loteCon(Producto prod, int cantidad, LocalDate vencimiento) {
        Lote l = new Lote();
        l.setProducto(prod);
        l.setNumeroLote("LT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        l.setCantidadActual(cantidad);
        l.setFechaProduccion(LocalDate.now());
        l.setFechaVencimiento(vencimiento);
        return l;
    }

    private void garantizarAlmacenYCategoria() {
        almacen = new Almacen();
        almacen.setNombre("ALM-C-" + sufijo);
        almacen.setUbicacion("Lima");
        almacen = almacenRepository.save(almacen);

        categoria = new Categoria();
        categoria.setNombre("CAT-C-" + sufijo);
        categoria.setDescripcion("Categoria de test de concurrencia");
        categoria = categoriaRepository.save(categoria);
    }

    @AfterEach
    void limpiarDatosDeTest() {
        if (producto != null) {
            loteRepository.deleteAll(loteRepository.findByProductoId(producto.getId()));
            // deleteById recarga la versión actual en vez de borrar con la que quedó
            // capturada en memoria (evita StaleObjectStateException tras consumos concurrentes).
            productoRepository.deleteById(producto.getId());
            producto = null;
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
}