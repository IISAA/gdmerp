package pe.edu.upeu.gdmerp.finanzas.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.shared.event.CompraAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.CompraRegistradaEvent;
import pe.edu.upeu.gdmerp.shared.event.PagoCompraRegistradoEvent;
import pe.edu.upeu.gdmerp.shared.event.RecepcionCompraEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanzasCompraEventListener {

    private final FinanzasApi finanzasApi;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCompraRegistrada(CompraRegistradaEvent event) {
        log.info("Finanzas: procesando CompraRegistradaEvent para comprobante: {}", event.numeroComprobante());
        finanzasApi.registrarFacturaCompraPendiente(
                event.compraId(),
                null,
                event.numeroComprobante(),
                event.proveedorRazonSocial(),
                event.total(),
                event.fechaEmision()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onRecepcionCompra(RecepcionCompraEvent event) {
        log.info("Finanzas: procesando RecepcionCompraEvent para comprobante: {}", event.numeroComprobante());
        finanzasApi.confirmarRecepcionCompra(event.compraId(), event.numeroComprobante());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onPagoCompraRegistrado(PagoCompraRegistradoEvent event) {
        log.info("Finanzas: procesando PagoCompraRegistradoEvent para comprobante: {}", event.numeroComprobante());
        MetodoPago metodo = parseMetodoPago(event.metodoPago());
        finanzasApi.registrarPagoProveedorCompra(
                event.compraId(),
                event.numeroComprobante(),
                event.proveedorRazonSocial(),
                event.montoPagado(),
                metodo,
                event.liquidadaTotalmente()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCompraAnulada(CompraAnuladaEvent event) {
        log.info("Finanzas: procesando CompraAnuladaEvent para comprobante: {}", event.numeroComprobante());
        finanzasApi.anularFacturaCompra(event.compraId(), event.numeroComprobante());
    }

    private MetodoPago parseMetodoPago(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return MetodoPago.TRANSFERENCIA;
        }
        try {
            return MetodoPago.valueOf(metodo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MetodoPago.TRANSFERENCIA;
        }
    }
}
