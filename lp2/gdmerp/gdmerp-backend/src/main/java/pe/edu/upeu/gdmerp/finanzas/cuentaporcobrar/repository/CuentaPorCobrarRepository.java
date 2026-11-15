package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    Optional<CuentaPorCobrar> findByCodigo(String codigo);

    Optional<CuentaPorCobrar> findFirstByVentaId(Long ventaId);

    boolean existsByCodigo(String codigo);

    List<CuentaPorCobrar> findAllByOrderByFechaEmisionDesc();

    List<CuentaPorCobrar> findByEstadoOrderByFechaEmisionDesc(EstadoCredito estado);

    @EntityGraph(attributePaths = "abonos")
    Optional<CuentaPorCobrar> findWithAbonosById(Long id);

    @Query("SELECT a.cuentaPorCobrar.id, COUNT(a) FROM AbonoCuentaPorCobrar a GROUP BY a.cuentaPorCobrar.id")
    List<Object[]> contarAbonosPorCuenta();

    @Query("""
        SELECT COALESCE(SUM(c.saldoPendiente), 0)
        FROM CuentaPorCobrar c
        WHERE c.estado IN (
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito.PENDIENTE,
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito.PAGADO_PARCIAL
        )
        """)
    BigDecimal sumarSaldoPendienteTotal();
}
