package pe.edu.upeu.gdmerp.inventario.stock.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.AlbaranEntrada;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.DetalleAlbaranEntrada;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.EstadoAlbaran;
import pe.edu.upeu.gdmerp.inventario.recepcion.repository.AlbaranEntradaRepository;
import pe.edu.upeu.gdmerp.shared.event.CompraConfirmadaEvent;
import pe.edu.upeu.gdmerp.shared.event.InsumoEventDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow A: Listener en Inventario.
 * Escucha CompraConfirmadaEvent en fase BEFORE_COMMIT y genera un Albarán de Entrada en estado PENDIENTE.
 * Si ocurre cualquier excepción en este listener, la transacción de confirmación de compra hace rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioCompraListener {

    private final AlbaranEntradaRepository albaranEntradaRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCompraConfirmada(CompraConfirmadaEvent event) {
        log.info("[Inventario] Evento recibido: CompraConfirmadaEvent para Orden ID: {} y Proveedor ID: {}. Iniciando generación de Albarán de Entrada...",
                event.ordenId(), event.proveedorId());

        if (event.insumos() == null || event.insumos().isEmpty()) {
            log.warn("[Inventario] Evento descartado: La orden {} no incluye insumos.", event.ordenId());
            return;
        }

        // 1. Construir cabecera del Albarán de Entrada en estado PENDIENTE
        String codigoAlbaran = "ALB-IN-OC-" + event.ordenId();

        AlbaranEntrada albaran = AlbaranEntrada.builder()
                .codigo(codigoAlbaran)
                .ordenCompraId(event.ordenId())
                .proveedorId(event.proveedorId())
                .estado(EstadoAlbaran.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .detalles(new ArrayList<>())
                .build();

        // 2. Poblar detalles con cantidades esperadas (sin asignar lotes ni vencimientos aún)
        for (InsumoEventDto insumo : event.insumos()) {
            DetalleAlbaranEntrada detalle = DetalleAlbaranEntrada.builder()
                    .albaranEntrada(albaran)
                    .productoId(insumo.productoId())
                    .cantidadEsperada(insumo.cantidadSolicitada())
                    .build();
            albaran.getDetalles().add(detalle);
        }

        albaranEntradaRepository.save(albaran);
        log.info("[Inventario] Albarán de Entrada {} generado exitosamente en estado PENDIENTE para la orden ID: {}.",
                codigoAlbaran, event.ordenId());
    }
}
