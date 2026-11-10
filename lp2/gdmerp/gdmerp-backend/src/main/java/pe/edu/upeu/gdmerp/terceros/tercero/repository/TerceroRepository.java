package pe.edu.upeu.gdmerp.terceros.tercero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.terceros.tercero.entity.Tercero;

import java.util.List;
import java.util.Optional;

@Repository("terceroRepository")
public interface TerceroRepository extends JpaRepository<Tercero, Long> {
    List<Tercero> findByActivoTrueOrderByIdDesc();
    List<Tercero> findByTipoTerceroAndActivoTrueOrderByIdDesc(String tipoTercero);
    Optional<Tercero> findByNumeroDocumento(String numeroDocumento);
}
