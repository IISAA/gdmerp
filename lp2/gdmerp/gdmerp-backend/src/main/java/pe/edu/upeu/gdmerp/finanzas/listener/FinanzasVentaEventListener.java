package pe.edu.upeu.gdmerp.finanzas.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.shared.event.DeliveryEntregadoEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaCreadaEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanzasVentaEventListener {

    private final FinanzasApi finanzasApi;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onVentaCreada(VentaCreadaEvent event) {
        log.info("Finanzas: procesando VentaCreadaEvent para ticket: {}", event.numeroTicket());
        if ("CREDITO".equalsIgnoreCase(event.metodoPago())) {
            finanzasApi.registrarVentaACredito(
                    event.numeroTicket(),
                    event.clienteNombre(),
                    null,
                    event.total(),
                    30
            );
        } else {
            MetodoPago metodo = parseMetodoPago(event.metodoPago());
            finanzasApi.registrarIngresoVentaPos(
                    event.total(),
                    metodo,
                    event.numeroTicket(),
                    event.clienteNombre(),
                    event.sesionCajaId(),
                    event.vendedorId()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onVentaAnulada(VentaAnuladaEvent event) {
        log.info("Finanzas: procesando VentaAnuladaEvent para ticket: {}", event.numeroTicket());
        boolean eraCredito = "CREDITO".equalsIgnoreCase(event.metodoPago());
        MetodoPago metodo = eraCredito ? null : parseMetodoPago(event.metodoPago());
        finanzasApi.revertirVentaPos(
                event.numeroTicket(),
                event.total(),
                metodo,
                eraCredito,
                event.sesionCajaId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onDeliveryEntregado(DeliveryEntregadoEvent event) {
        log.info("Finanzas: procesando DeliveryEntregadoEvent para entrega: {}", event.codigo());
        MetodoPago metodo = parseMetodoPago(event.metodoPago());
        finanzasApi.registrarIngresoVentaPos(
                event.montoTotal(),
                metodo,
                event.codigo(),
                "Cliente Delivery",
                null,
                null
        );
    }

    private MetodoPago parseMetodoPago(String metodo) {
        if (metodo == null) return MetodoPago.EFECTIVO;
        try {
            return MetodoPago.valueOf(metodo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MetodoPago.EFECTIVO;
        }
    }
}
