package pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findBySesionCajaIdOrderByFechaHoraAsc(Long sesionCajaId);

    @Query("SELECT m FROM MovimientoCaja m WHERE m.fechaHora >= :desde AND m.fechaHora <= :hasta ORDER BY m.fechaHora DESC")
    List<MovimientoCaja> findByRangoFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
