package pe.edu.upeu.gdmerp.inventario.producto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent.ProductoVentaSnapshot;
import pe.edu.upeu.gdmerp.shared.event.VentaAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaCreadaEvent;

import java.util.List;

/**
 * Listener de integración en Inventario para completar el triángulo comercial (Ventas -> Inventario).
 * Ejecuta descuento y reposición de stock en {@link TransactionPhase#BEFORE_COMMIT} para garantizar
 * rollback atómico compartido entre Ventas, Inventario y Finanzas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioVentaEventListener {

    private final InventarioOperacionService inventarioOperacionService;
    private final ProductoRepository productoRepository;

    @EventListener
    @Transactional(readOnly = true)
    public void onConsultaProductosVenta(ConsultaProductosVentaEvent event) {
        List<Producto> productos = (event.productoIds() == null || event.productoIds().isEmpty())
                ? productoRepository.findAll()
                : productoRepository.findAllById(event.productoIds());

        for (Producto producto : productos) {
            event.resultados().put(
                    producto.getId(),
                    new ProductoVentaSnapshot(
                            producto.getId(),
                            producto.getNombre(),
                            producto.getCostoPromedio(),
                            producto.getStockTotal()
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
