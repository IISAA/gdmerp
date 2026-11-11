package pe.edu.upeu.gdmerp.inventario.producto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.event.AlmacenEliminacionSolicitadaEvent;
import pe.edu.upeu.gdmerp.shared.event.CategoriaEliminacionSolicitadaEvent;
import pe.edu.upeu.gdmerp.shared.event.ValidacionProductoSolicitadaEvent;

/**
 * Escucha eventos de integridad y validación dirigidos al agregado Producto sin exponer
 * {@link ProductoRepository} ni {@code ProductoService} a otros submódulos o módulos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoReferenciaEventListener {

    private final ProductoRepository productoRepository;

    // Síncrono (@EventListener) dentro de la transacción activa: debe validar ANTES de que Hibernate
    // ejecute flush del DELETE sobre Almacén/Categoría y dispare DataIntegrityViolationException.
    @EventListener
    public void onAlmacenEliminacionSolicitada(AlmacenEliminacionSolicitadaEvent event) {
        if (productoRepository.existsByAlmacenId(event.almacenId())) {
            throw new IllegalStateException(
                    "No se puede eliminar el almacén con id " + event.almacenId() + " porque tiene productos asociados");
        }
    }

    @EventListener
    public void onCategoriaEliminacionSolicitada(CategoriaEliminacionSolicitadaEvent event) {
        if (productoRepository.existsByCategoriaId(event.categoriaId())) {
            throw new IllegalStateException(
                    "No se puede eliminar la categoría con id " + event.categoriaId() + " porque tiene productos asociados");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onValidacionProductoSolicitada(ValidacionProductoSolicitadaEvent event) {
        if (event.productoId() != null && !productoRepository.existsById(event.productoId())) {
            log.warn("Validación inter-módulo fallida desde {}: producto {} no existe",
                    event.moduloOrigen(), event.productoId());
            throw new ResourceNotFoundException("Producto no encontrado con id " + event.productoId());
        }
    }
}
