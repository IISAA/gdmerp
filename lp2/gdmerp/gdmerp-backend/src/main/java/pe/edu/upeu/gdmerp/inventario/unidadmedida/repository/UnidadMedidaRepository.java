package pe.edu.upeu.gdmerp.inventario.unidadmedida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.CategoriaUnidadMedida;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.UnidadMedida;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    Optional<UnidadMedida> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<UnidadMedida> findByActivoTrue();

    List<UnidadMedida> findByCategoriaAndActivoTrue(CategoriaUnidadMedida categoria);
}
