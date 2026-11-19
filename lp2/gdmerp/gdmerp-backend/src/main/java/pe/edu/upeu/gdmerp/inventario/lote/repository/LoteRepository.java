package pe.edu.upeu.gdmerp.inventario.lote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByProductoId(Long productoId);

    // Ordenado por fecha de vencimiento más próxima (FEFO)
    List<Lote> findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
            Long productoId, BigDecimal cantidadMinima);

    List<Lote> findByProductoIdOrderByFechaVencimientoAsc(Long productoId);

    boolean existsByNumeroLoteIgnoreCaseAndProductoId(String numeroLote, Long productoId);

    @Query(value = """
            SELECT NUMERO_LOTE
            FROM (
                SELECT NUMERO_LOTE
                FROM GDM_INVENTARIO.LOTES
                WHERE NUMERO_LOTE LIKE :prefijo || '%'
                ORDER BY NUMERO_LOTE DESC
            )
            WHERE ROWNUM = 1
            """, nativeQuery = true)
    Optional<String> findTopNumeroLoteByPrefijo(@Param("prefijo") String prefijo);
}