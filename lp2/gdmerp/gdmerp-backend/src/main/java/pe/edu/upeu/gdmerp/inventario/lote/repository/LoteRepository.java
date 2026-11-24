package pe.edu.upeu.gdmerp.inventario.lote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;

import java.math.BigDecimal;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByProductoId(Long productoId);

    // Ordenado por fecha de vencimiento más próxima (FEFO)
    List<Lote> findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
            Long productoId, BigDecimal cantidadMinima);

    List<Lote> findByProductoIdOrderByFechaVencimientoAsc(Long productoId);

    boolean existsByNumeroLoteIgnoreCase(String numeroLote);

    boolean existsByNumeroLoteIgnoreCaseAndProductoId(String numeroLote, Long productoId);

    long countByNumeroLoteStartingWith(String prefijo);
}