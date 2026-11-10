package pe.edu.upeu.gdmerp.finanzas.cajachica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.DetalleRendicionCajaChica;

import java.util.List;

@Repository
public interface DetalleRendicionCajaChicaRepository extends JpaRepository<DetalleRendicionCajaChica, Long> {
    List<DetalleRendicionCajaChica> findByRendicionId(Long rendicionId);
}
