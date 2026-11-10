package pe.edu.upeu.gdmerp.finanzas.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.compras.event.CompraAnuladaEvent;
import pe.edu.upeu.gdmerp.compras.event.CompraRegistradaEvent;
import pe.edu.upeu.gdmerp.compras.event.PagoCompraRegistradoEvent;
import pe.edu.upeu.gdmerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanzasCompraEventListener {

    private final FinanzasApi finanzasApi;

    @ApplicationModuleListener
    public void onCompraRegistrada(CompraRegistradaEvent event) {
        log.info("Finanzas: procesando CompraRegistradaEvent para comprobante: {}", event.numeroComprobante());
        finanzasApi.registrarFacturaCompraPendiente(
                event.numeroComprobante(),
                event.proveedorRazonSocial(),
                event.total(),
                event.fechaEmision()
        );
    }

    @ApplicationModuleListener
    public void onPagoCompraRegistrado(PagoCompraRegistradoEvent event) {
        log.info("Finanzas: procesando PagoCompraRegistradoEvent para comprobante: {}", event.numeroComprobante());
        MetodoPago metodo = parseMetodoPago(event.metodoPago());
        finanzasApi.registrarPagoProveedorCompra(
                event.numeroComprobante(),
                event.proveedorRazonSocial(),
                event.montoPagado(),
                metodo,
                event.liquidadaTotalmente()
        );
    }

    @ApplicationModuleListener
    public void onCompraAnulada(CompraAnuladaEvent event) {
        log.info("Finanzas: procesando CompraAnuladaEvent para comprobante: {}", event.numeroComprobante());
        finanzasApi.anularFacturaCompra(event.numeroComprobante());
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
