package pe.edu.upeu.gdmerp.compras.ordencompra.entity;

/**
 * Ciclo de vida estricto de una Orden de Compra (basado en estándar Odoo y DDD):
 * 
 * BORRADOR   -> Cotización inicial / Negociación de precios y cantidades.
 * CONFIRMADA -> Acuerdo cerrado con proveedor; esperando recepción física en almacén.
 * RECIBIDA   -> Mercancía recibida e inspeccionada en almacén (Kardex y Lotes actualizados).
 * FACTURADA  -> Comprobante fiscal / Factura registrada en Finanzas y Cuenta por Pagar creada.
 * CANCELADA  -> Orden cancelada (solo permitida antes de recepcionar o facturar).
 */
public enum EstadoCompra {
    BORRADOR,
    CONFIRMADA,
    RECIBIDA,
    FACTURADA,
    CANCELADA,
    @Deprecated
    ANULADA;

    public boolean puedeConfirmar() {
        return this == BORRADOR;
    }

    public boolean puedeRecibir() {
        return this == CONFIRMADA;
    }

    public boolean puedeFacturar() {
        return this == RECIBIDA;
    }

    public boolean puedeCancelar() {
        return this == BORRADOR || this == CONFIRMADA;
    }

    @Deprecated
    public boolean puedeAnular() {
        return puedeCancelar();
    }
}
