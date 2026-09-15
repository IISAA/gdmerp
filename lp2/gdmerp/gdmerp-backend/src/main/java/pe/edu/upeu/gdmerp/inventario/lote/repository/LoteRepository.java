package pe.edu.upeu.gdmerp.inventario.lote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    // Todos los lotes de un producto (sin filtrar por stock)
    List<Lote> findByProductoId(Long productoId);

    // Busca los lotes de un producto que tengan stock, ordenados por vencimiento (FEFO)
    List<Lote> findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(Long productoId, Integer cantidadMinima);
}