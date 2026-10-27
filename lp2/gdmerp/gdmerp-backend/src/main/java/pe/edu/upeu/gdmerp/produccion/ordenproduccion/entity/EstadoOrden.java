package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

/**
 * Enumeración que define los estados permitidos en el ciclo de vida de una orden de producción.
 */
public enum EstadoOrden {
    PLANIFICADO,
    EN_PROCESO,
    COMPLETADO,
    CANCELADO;

    public EstadoOrdenProduccion toEstadoOrdenProduccion() {
        return EstadoOrdenProduccion.valueOf(this.name());
    }

    public static EstadoOrden from(EstadoOrdenProduccion estado) {
        return estado != null ? EstadoOrden.valueOf(estado.name()) : null;
    }
}
