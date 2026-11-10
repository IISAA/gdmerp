package pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion;

import java.util.List;

@Repository
public interface DenominacionMonedaRepository extends JpaRepository<DenominacionMoneda, Long> {

    List<DenominacionMoneda> findByActivoTrueOrderByOrdenAsc();

    List<DenominacionMoneda> findByActivoTrueOrderByValorFacialDesc();

    List<DenominacionMoneda> findByTipoAndActivoTrueOrderByValorFacialDesc(TipoDenominacion tipo);
}
