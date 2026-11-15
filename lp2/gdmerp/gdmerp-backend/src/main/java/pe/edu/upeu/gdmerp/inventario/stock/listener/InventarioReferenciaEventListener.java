package pe.edu.upeu.gdmerp.inventario.stock.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;
import pe.edu.upeu.gdmerp.shared.event.AlmacenEliminacionSolicitadaEvent;
import pe.edu.upeu.gdmerp.shared.event.CategoriaEliminacionSolicitadaEvent;

@Component
@RequiredArgsConstructor
public class InventarioReferenciaEventListener {

    private final StockInventarioRepository stockRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAlmacenEliminacionSolicitada(AlmacenEliminacionSolicitadaEvent event) {
        if (event.almacenId() != null && stockRepository.existsByAlmacenId(event.almacenId())) {
            throw new IllegalStateException(
                    "No se puede eliminar el almacén con id " + event.almacenId()
                            + " porque tiene existencias de inventario asociadas");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCategoriaEliminacionSolicitada(CategoriaEliminacionSolicitadaEvent event) {
        if (event.categoriaId() != null && stockRepository.existsByCategoriaId(event.categoriaId())) {
            throw new IllegalStateException(
                    "No se puede eliminar la categoría con id " + event.categoriaId()
                            + " porque tiene registros de inventario asociados");
        }
    }
}
