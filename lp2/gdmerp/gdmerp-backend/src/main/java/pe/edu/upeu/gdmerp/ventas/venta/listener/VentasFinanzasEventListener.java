package pe.edu.upeu.gdmerp.ventas.venta.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.VentaYaAnuladaException;
import pe.edu.upeu.gdmerp.shared.event.ConsultaVentaPorTicketEvent;
import pe.edu.upeu.gdmerp.shared.event.PagoVentaRegistradoEvent;
import pe.edu.upeu.gdmerp.ventas.auditoria.service.AuditoriaService;
import pe.edu.upeu.gdmerp.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.Venta;
import pe.edu.upeu.gdmerp.ventas.venta.repository.VentaRepository;

import java.math.BigDecimal;

/**
 * Listener de integración en Ventas para el cierre del ciclo de ingresos (Finanzas -> Ventas).
 * Se ejecuta en {@link TransactionPhase#BEFORE_COMMIT} con fallbackExecution = true para
 * garantizar atomicidad ACID compartida en la base de datos Oracle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VentasFinanzasEventListener {

    private final VentaRepository ventaRepository;
    private final AuditoriaService auditoriaService;

    @EventListener
    @Transactional(readOnly = true)
    public void onConsultaVentaPorTicket(ConsultaVentaPorTicketEvent event) {
        if (event == null || event.numeroTicket() == null || event.numeroTicket().isBlank()) {
            return;
        }
        ventaRepository.findByNumeroTicket(event.numeroTicket().trim())
                .ifPresent(venta -> event.ventaIdEncontrado().set(venta.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onPagoVentaRegistrado(PagoVentaRegistradoEvent event) {
        if (event == null || event.ventaId() == null) {
            return;
        }
        if (event.montoPagado() == null || event.montoPagado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto pagado debe ser mayor a cero para liquidar la venta ID: " + event.ventaId());
        }

        Venta venta = ventaRepository.findByIdForUpdate(event.ventaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venta no encontrada con ID: " + event.ventaId()));

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException(
                    "No se puede marcar como PAGADA una venta ANULADA (ID: " + event.ventaId() + ")");
        }

        venta.setEstado(EstadoVenta.PAGADA);
        ventaRepository.save(venta);
        auditoriaService.registrar(
                "FINANZAS",
                "PAGAR",
                "VENTA",
                venta.getId(),
                "transaccionFinancieraId=" + event.transaccionFinancieraId() + "; monto=" + event.montoPagado()
        );

        log.info("Ventas: Venta #{} ({}) actualizada a estado PAGADA por transacción financiera #{} (S/ {}) en fase BEFORE_COMMIT",
                venta.getId(), venta.getNumeroTicket(), event.transaccionFinancieraId(), event.montoPagado());
    }
}
