package pe.edu.upeu.gdmerp.inventario.stock.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "ALMACENERO"})
class StockConcurrenciaIntegrationTest {

    @Autowired
    private InventarioOperacionService inventarioOperacionService;

    @Autowired
    private StockInventarioRepository stockRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    @Qualifier("catalogoProductoRepository")
    private ProductoRepository catalogoProductoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;
    private String sufijo;
    private final List<Long> stocksCreados = new ArrayList<>();
    private final List<Long> productosCreados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
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
            if (catalogoProductoRepository.existsById(prodId)) {
                catalogoProductoRepository.deleteById(prodId);
            }
        }
        productosCreados.clear();
    }

    @Test
    void optimisticLockingEnStockInventarioDetectaConflictoYPreservaConsistencia() {
        StockInventario inicial = crearCatalogoYStock("50.0000", 0);

        // Transacción A lee la versión inicial
        StockInventario leidoEnA = txTemplate.execute(status ->
                stockRepository.findById(inicial.getId()).orElseThrow());

        // Transacción B modifica y confirma primero, incrementando @Version
        txTemplate.executeWithoutResult(status -> {
            StockInventario sB = stockRepository.findById(inicial.getId()).orElseThrow();
            sB.setStockActual(sB.getStockActual().add(BigDecimal.ONE));
            stockRepository.saveAndFlush(sB);
        });

        // Transacción A intenta guardar con la versión obsoleta
        assertThrows(OptimisticLockingFailureException.class, () ->
                txTemplate.executeWithoutResult(status -> {
                    leidoEnA.setStockActual(leidoEnA.getStockActual().subtract(BigDecimal.ONE));
                    stockRepository.saveAndFlush(leidoEnA);
                })
        );

        StockInventario finalBd = stockRepository.findById(inicial.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("51.0000").compareTo(finalBd.getStockActual()),
                "Solo el incremento de B debe persistir tras el rechazo por bloqueo optimista");
    }

    @Test
    void consumoFefoConcurrenteReintentaAnteColisionSinSobreventa()
            throws ExecutionException, InterruptedException, TimeoutException {
        StockInventario stock = crearCatalogoYStock("10.0000", 10);
        Long productoId = stock.getProductoId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Runnable tareaConsumo = () -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                } catch (BrokenBarrierException | TimeoutException e) {
                    throw new IllegalStateException(e);
                }
                inventarioOperacionService.consumirStockFefo(productoId, 5);
            };

            Future<?> f1 = pool.submit(tareaConsumo);
            Future<?> f2 = pool.submit(tareaConsumo);
            f1.get(15, TimeUnit.SECONDS);
            f2.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        StockInventario finalBd = stockRepository.findById(stock.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(finalBd.getStockActual()),
                "El stock total debe quedar exactamente en cero sin sobreventa ni pérdidas por colisión");

        List<Lote> lotes = loteRepository.findByProductoId(productoId);
        assertEquals(1, lotes.size());
        assertEquals(0, lotes.get(0).getCantidadActual());

        assertTrue(movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId).size() == 2);
    }

    private StockInventario crearCatalogoYStock(String stockInicial, int cantidadLote) {
        Producto prod = Producto.builder()
                .sku("SKU-CONC-" + sufijo + "-" + productosCreados.size())
                .nombre("Insumo Concurrencia " + sufijo)
                .unidadMedida("KG")
                .precio(new BigDecimal("20.00"))
                .costoBase(new BigDecimal("10.0000"))
                .tipoProducto(TipoProducto.MATERIA_PRIMA)
                .stockMinimo(BigDecimal.ONE)
                .stock(0)
                .activo(Boolean.TRUE)
                .build();
        prod = catalogoProductoRepository.save(prod);
        productosCreados.add(prod.getId());

        StockInventario stock = new StockInventario();
        stock.setProductoId(prod.getId());
        stock.setStockActual(new BigDecimal(stockInicial));
        stock.setStockMinimo(BigDecimal.ONE);
        stock.setCostoPromedio(new BigDecimal("10.0000"));
        stock = stockRepository.save(stock);
        stocksCreados.add(stock.getId());

        if (cantidadLote > 0) {
            Lote lote = new Lote();
            lote.setProductoId(prod.getId());
            lote.setNumeroLote("LT-CONC-" + sufijo + "-" + productosCreados.size());
            lote.setCantidadActual(cantidadLote);
            lote.setFechaProduccion(LocalDate.now().minusDays(1));
            lote.setFechaVencimiento(LocalDate.now().plusMonths(6));
            loteRepository.save(lote);
        }

        return stock;
    }
}
