package pe.edu.upeu.gdmerp.finanzas.cajachica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionCajaCentroCostoRepository extends JpaRepository<AsignacionCajaCentroCosto, AsignacionCajaCentroCostoId> {

    List<AsignacionCajaCentroCosto> findByCajaChicaId(Long cajaChicaId);

    List<AsignacionCajaCentroCosto> findByCajaChicaIdAndActivoTrue(Long cajaChicaId);

    Optional<AsignacionCajaCentroCosto> findByIdCajaChicaIdAndIdCentroCostoId(Long cajaChicaId, Long centroCostoId);
}
