package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evento síncrono de consulta desacoplada de productos para el módulo Ventas.
 * Evita la inyección directa de servicios o repositorios de Inventario dentro de Ventas.
 */
public record ConsultaProductosVentaEvent(
        Collection<Long> productoIds,
        Map<Long, ProductoVentaSnapshot> resultados
) {

    public static ConsultaProductosVentaEvent paraIds(Collection<Long> productoIds) {
        return new ConsultaProductosVentaEvent(productoIds != null ? productoIds : List.of(), new LinkedHashMap<>());
    }

    public static ConsultaProductosVentaEvent todos() {
        return new ConsultaProductosVentaEvent(List.of(), new LinkedHashMap<>());
    }

    public record ProductoVentaSnapshot(
            Long id,
            String nombre,
            BigDecimal precioUnitario,
            BigDecimal stockDisponible
    ) {}
}
