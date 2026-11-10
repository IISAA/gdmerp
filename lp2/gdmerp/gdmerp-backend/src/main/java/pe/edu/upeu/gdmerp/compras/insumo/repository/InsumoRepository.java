package pe.edu.upeu.gdmerp.compras.insumo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.compras.insumo.entity.Insumo;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    Optional<Insumo> findByCodigo(String codigo);
    List<Insumo> findByEstado(String estado);
}

