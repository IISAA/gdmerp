package pe.edu.upeu.gdmerp.produccion.listamateriales.dto;

import java.math.BigDecimal;
import java.util.List;

public record ListaMaterialesResponse(
    Long id,
    String codigo,
    Long productoTerminadoId,
    String nombreProductoTerminado,
    String skuProductoTerminado,
    String unidadMedidaProductoTerminado,
    BigDecimal cantidadBase,
    List<DetalleListaMaterialesResponse> detalles
) {
    public ListaMaterialesResponse(
        Long id,
        String codigo,
        Long productoTerminadoId,
        String nombreProductoTerminado,
        String skuProductoTerminado,
        BigDecimal cantidadBase,
        List<DetalleListaMaterialesResponse> detalles
    ) {
        this(id, codigo, productoTerminadoId, nombreProductoTerminado, skuProductoTerminado, null, cantidadBase, detalles);
    }
}
