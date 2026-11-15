package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.CuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    Optional<CuentaPorPagar> findByCodigo(String codigo);

    Optional<CuentaPorPagar> findFirstByCompraId(Long compraId);

    Optional<CuentaPorPagar> findFirstByNumeroComprobante(String numeroComprobante);

    List<CuentaPorPagar> findByEstadoOrderByFechaVencimientoAsc(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findAllByOrderByFechaVencimientoAsc();

    @EntityGraph(attributePaths = "abonos")
    Optional<CuentaPorPagar> findWithAbonosById(Long id);

    @Query("""
        SELECT COALESCE(SUM(c.saldoPendiente), 0)
        FROM CuentaPorPagar c
        WHERE c.estado IN (
            pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar.PENDIENTE,
            pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar.PAGADO_PARCIAL
        )
        """)
    BigDecimal sumarSaldoPendienteTotal();

    @Query("""
        SELECT a.cuentaPorPagar.id, COUNT(a)
        FROM AbonoCuentaPorPagar a
        GROUP BY a.cuentaPorPagar.id
        """)
    List<Object[]> contarAbonosPorCuenta();
}
