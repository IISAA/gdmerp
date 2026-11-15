package pe.edu.upeu.gdmerp.inventario.stock.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent.ProductoVentaSnapshot;
import pe.edu.upeu.gdmerp.shared.event.VentaAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaCreadaEvent;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listener de integración en Inventario para completar el triángulo comercial (Ventas -> Inventario).
 * Consulta los datos maestros comerciales en Catálogo vía {@code @NamedInterface} y ejecuta
 * descuento/reposición de stock en {@link TransactionPhase#BEFORE_COMMIT}.
 */
@Slf4j
@Component
public class InventarioVentaEventListener {

    private final InventarioOperacionService inventarioOperacionService;
    private final StockInventarioRepository stockRepository;
    private final ProductoService catalogoProductoService;

    public InventarioVentaEventListener(
            InventarioOperacionService inventarioOperacionService,
            StockInventarioRepository stockRepository,
            @Qualifier("catalogoProductoService") ProductoService catalogoProductoService) {
        this.inventarioOperacionService = inventarioOperacionService;
        this.stockRepository = stockRepository;
        this.catalogoProductoService = catalogoProductoService;
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onConsultaProductosVenta(ConsultaProductosVentaEvent event) {
        List<StockInventario> stocks = (event.productoIds() == null || event.productoIds().isEmpty())
                ? stockRepository.findAll()
                : stockRepository.findByProductoIdIn(event.productoIds());

        Map<Long, BigDecimal> stockPorProducto = new LinkedHashMap<>();
        Map<Long, BigDecimal> costoPorProducto = new LinkedHashMap<>();
        for (StockInventario stock : stocks) {
            stockPorProducto.merge(stock.getProductoId(), stock.obtenerStockSeguro(), BigDecimal::add);
            costoPorProducto.putIfAbsent(stock.getProductoId(), stock.obtenerCostoPromedioSeguro());
        }

        Set<Long> idsAConsultar = (event.productoIds() == null || event.productoIds().isEmpty())
                ? stockPorProducto.keySet()
                : event.productoIds().stream().collect(Collectors.toSet());

        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(idsAConsultar);

        for (Long productoId : stockPorProducto.keySet()) {
            ProductoResponse catalogo = catalogos.get(productoId);
            String nombre = catalogo != null && catalogo.getNombre() != null
                    ? catalogo.getNombre()
                    : "Producto #" + productoId;
            BigDecimal precioCatalogo = catalogo != null ? catalogo.getPrecio() : null;
            BigDecimal costoPromedio = costoPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
            BigDecimal precioUnitario = (precioCatalogo != null && precioCatalogo.compareTo(BigDecimal.ZERO) > 0)
                    ? precioCatalogo
                    : costoPromedio;

            event.resultados().put(
                    productoId,
                    new ProductoVentaSnapshot(
                            productoId,
                            nombre,
                            precioUnitario,
                            stockPorProducto.get(productoId)
                    )
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onVentaCreada(VentaCreadaEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            return;
        }
        log.info("Inventario: descontando stock de {} ítem(s) por Venta {} en fase BEFORE_COMMIT",
                event.items().size(), event.numeroTicket());
        for (VentaCreadaEvent.ItemVentaEvent item : event.items()) {
            inventarioOperacionService.descontarStockVenta(
                    item.productoId(),
                    item.loteId(),
                    item.cantidad(),
                    event.numeroTicket()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onVentaAnulada(VentaAnuladaEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            return;
        }
        log.info("Inventario: reponiendo stock de {} ítem(s) por anulación de Venta {} en fase BEFORE_COMMIT",
                event.items().size(), event.numeroTicket());
        for (VentaAnuladaEvent.ItemVentaAnuladaEvent item : event.items()) {
            inventarioOperacionService.reponerStockVenta(
                    item.productoId(),
                    item.cantidad(),
                    event.numeroTicket()
            );
        }
    }
}
