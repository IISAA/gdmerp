package pe.edu.upeu.gdmerp.finanzas.api;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API pública del módulo Finanzas expuesta a otros módulos del Monolito Modular (Spring Modulith).
 */
public interface FinanzasApi {

    Long registrarCobroVenta(Long usuarioId, BigDecimal monto, MetodoPago metodoPago, String referenciaVenta, Long categoriaId);

    default Long registrarIngresoVentaPos(BigDecimal monto, MetodoPago metodoPago, String numeroTicket, String clienteNombre) {
        return registrarIngresoVentaPos(null, monto, metodoPago, numeroTicket, clienteNombre, null, null);
    }

    default Long registrarIngresoVentaPos(BigDecimal monto, MetodoPago metodoPago, String numeroTicket, String clienteNombre, Long sesionCajaId, String cajeroId) {
        return registrarIngresoVentaPos(null, monto, metodoPago, numeroTicket, clienteNombre, sesionCajaId, cajeroId);
    }

    Long registrarIngresoVentaPos(Long ventaId, BigDecimal monto, MetodoPago metodoPago, String numeroTicket, String clienteNombre, Long sesionCajaId, String cajeroId);

    default Long registrarVentaACredito(
            String numeroTicket,
            String clienteNombre,
            String clienteDocumento,
            BigDecimal montoTotal,
            Integer diasCredito) {
        return registrarVentaACredito(null, null, numeroTicket, clienteNombre, clienteDocumento, montoTotal, diasCredito);
    }

    Long registrarVentaACredito(
            Long ventaId,
            Long clienteId,
            String numeroTicket,
            String clienteNombre,
            String clienteDocumento,
            BigDecimal montoTotal,
            Integer diasCredito
    );

    default void revertirVentaPos(String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito) {
        revertirVentaPos(null, numeroTicket, monto, metodoPago, eraCredito, null);
    }

    default void revertirVentaPos(String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito, Long sesionCajaId) {
        revertirVentaPos(null, numeroTicket, monto, metodoPago, eraCredito, sesionCajaId);
    }

    void revertirVentaPos(Long ventaId, String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito, Long sesionCajaId);

    default void registrarFacturaCompraPendiente(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoTotal,
            LocalDate fechaEmision) {
        registrarFacturaCompraPendiente(null, null, numeroComprobante, razonSocialProveedor, montoTotal, fechaEmision);
    }

    void registrarFacturaCompraPendiente(
            Long compraId,
            Long proveedorId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoTotal,
            LocalDate fechaEmision
    );

    void confirmarRecepcionCompra(Long compraId, String numeroComprobante);

    default void registrarPagoProveedorCompra(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            boolean liquidadaTotalmente) {
        registrarPagoProveedorCompra(null, numeroComprobante, razonSocialProveedor, montoPagado, metodoPago, liquidadaTotalmente);
    }

    void registrarPagoProveedorCompra(
            Long compraId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            boolean liquidadaTotalmente
    );

    default void anularFacturaCompra(String numeroComprobante) {
        anularFacturaCompra(null, numeroComprobante);
    }

    void anularFacturaCompra(Long compraId, String numeroComprobante);

    boolean tieneSesionAbierta(Long usuarioId);

    Long obtenerIdSesionActiva(Long usuarioId);
}
