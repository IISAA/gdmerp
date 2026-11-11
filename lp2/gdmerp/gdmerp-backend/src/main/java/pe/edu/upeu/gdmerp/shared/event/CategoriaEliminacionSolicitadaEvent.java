package pe.edu.upeu.gdmerp.shared.event;

/**
 * Evento de integración emitido antes de eliminar una Categoría para validar en Inventario
 * que no existan productos asociados sin acoplar CategoriaServiceImpl a ProductoRepository.
 */
public record CategoriaEliminacionSolicitadaEvent(
        Long categoriaId,
        String nombreCategoria
) {}
