package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

/**
 * Alias de estado de orden de producción alineado con las transiciones de Kardex.
 */
public enum EstadoOrden {
    PLANIFICADO,
    EN_PROCESO,
    COMPLETADO,
    FINALIZADA,
    CANCELADO;

    public EstadoOrdenProduccion toEstadoOrdenProduccion() {
        return EstadoOrdenProduccion.valueOf(this.name());
    }

    public static EstadoOrden from(EstadoOrdenProduccion estado) {
        return estado != null ? EstadoOrden.valueOf(estado.name()) : null;
    }
}
