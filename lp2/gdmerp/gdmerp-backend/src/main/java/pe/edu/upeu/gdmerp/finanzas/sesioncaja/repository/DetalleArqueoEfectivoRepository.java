package pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivoId;

import java.util.List;

@Repository
public interface DetalleArqueoEfectivoRepository extends JpaRepository<DetalleArqueoEfectivo, DetalleArqueoEfectivoId> {

    List<DetalleArqueoEfectivo> findByIdSesionId(Long sesionId);

    @EntityGraph(attributePaths = "denominacionMoneda")
    List<DetalleArqueoEfectivo> findBySesionCajaId(Long sesionId);
}
