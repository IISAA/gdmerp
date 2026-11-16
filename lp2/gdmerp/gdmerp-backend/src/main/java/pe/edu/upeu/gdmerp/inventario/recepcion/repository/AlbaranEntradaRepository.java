package pe.edu.upeu.gdmerp.inventario.recepcion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.AlbaranEntrada;
import pe.edu.upeu.gdmerp.inventario.recepcion.entity.EstadoAlbaran;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbaranEntradaRepository extends JpaRepository<AlbaranEntrada, Long> {
    Optional<AlbaranEntrada> findByOrdenCompraId(Long ordenCompraId);
    List<AlbaranEntrada> findByEstado(EstadoAlbaran estado);
}
