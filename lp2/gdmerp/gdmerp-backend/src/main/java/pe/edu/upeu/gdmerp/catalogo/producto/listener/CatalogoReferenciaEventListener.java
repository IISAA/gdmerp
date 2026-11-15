package pe.edu.upeu.gdmerp.catalogo.producto.listener;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.event.ValidacionProductoSolicitadaEvent;

@Component
public class CatalogoReferenciaEventListener {

    private final ProductoRepository productoRepository;

    public CatalogoReferenciaEventListener(
            @Qualifier("catalogoProductoRepository") ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onValidacionProductoSolicitada(ValidacionProductoSolicitadaEvent event) {
        if (event.productoId() == null || !productoRepository.existsById(event.productoId())) {
            throw new ResourceNotFoundException(
                    "El producto con id " + event.productoId() + " no existe en el catálogo");
        }
    }
}
