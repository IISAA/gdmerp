package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {
    Optional<CuentaPorCobrar> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<CuentaPorCobrar> findAllByOrderByFechaEmisionDesc();
    List<CuentaPorCobrar> findByEstadoOrderByFechaEmisionDesc(EstadoCredito estado);
}
