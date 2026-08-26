package pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {
}