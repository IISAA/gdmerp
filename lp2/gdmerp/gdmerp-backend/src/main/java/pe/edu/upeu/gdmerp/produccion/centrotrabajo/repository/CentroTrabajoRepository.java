package pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;

public interface CentroTrabajoRepository extends JpaRepository<CentroTrabajo, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}