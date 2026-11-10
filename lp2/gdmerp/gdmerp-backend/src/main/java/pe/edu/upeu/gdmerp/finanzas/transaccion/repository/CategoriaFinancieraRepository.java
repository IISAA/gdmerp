package pe.edu.upeu.gdmerp.finanzas.transaccion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaFinancieraRepository extends JpaRepository<CategoriaFinanciera, Long> {
    Optional<CategoriaFinanciera> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    List<CategoriaFinanciera> findByTipoDefault(TipoFinanciero tipoDefault);
}
