package pe.edu.upeu.gdmerp.finanzas.cajachica.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.RendicionCajaChica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RendicionCajaChicaRepository extends JpaRepository<RendicionCajaChica, Long> {

    Optional<RendicionCajaChica> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<RendicionCajaChica> findByCajaChicaId(Long cajaChicaId);

    /**
     * 1. FILTROS COMBINADOS Y ORDENAMIENTO:
     * Consulta dinámica JPQL que permite combinar de 1 a varios filtros opcionales
     * (cajaChicaId, estado, responsable, rango de fechas, rango de montos) junto con
     * soporte para ordenamiento y paginación a través de Pageable.
     */
    @Query("SELECT r FROM RendicionCajaChica r WHERE " +
            "(:cajaChicaId IS NULL OR r.cajaChica.id = :cajaChicaId) AND " +
            "(:estado IS NULL OR r.estado = :estado) AND " +
            "(:responsable IS NULL OR LOWER(r.responsable) LIKE LOWER(CONCAT('%', :responsable, '%'))) AND " +
            "(:fechaInicio IS NULL OR r.fecha >= :fechaInicio) AND " +
            "(:fechaFin IS NULL OR r.fecha <= :fechaFin) AND " +
            "(:montoMin IS NULL OR r.montoTotal >= :montoMin) AND " +
            "(:montoMax IS NULL OR r.montoTotal <= :montoMax)")
    Page<RendicionCajaChica> buscarConFiltros(
            @Param("cajaChicaId") Long cajaChicaId,
            @Param("estado") EstadoRendicion estado,
            @Param("responsable") String responsable,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("montoMin") BigDecimal montoMin,
            @Param("montoMax") BigDecimal montoMax,
            Pageable pageable
    );

    /**
     * 2. PROYECCIÓN DE RESUMEN (Expresión de Constructor):
     * Proyección JPQL tipo DTO ('SELECT new ...') que instancia directamente RendicionResumenDto.
     * Solo recupera las columnas esenciales de cabecera y el conteo de detalles (SIZE),
     * optimizando el rendimiento de memoria y eliminando consultas N+1.
     */
    @Query("SELECT new pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionResumenDto(" +
            "r.id, r.codigo, r.responsable, r.fecha, r.montoTotal, r.estado, r.cajaChica.codigo, SIZE(r.detalles)) " +
            "FROM RendicionCajaChica r WHERE (:cajaChicaId IS NULL OR r.cajaChica.id = :cajaChicaId)")
    List<RendicionResumenDto> obtenerResumenProyectado(@Param("cajaChicaId") Long cajaChicaId);

    /**
     * 3. REPORTE AGREGADO (COALESCE para evitar null):
     * Consulta con funciones de agregación (SUM, AVG, MAX, MIN, COUNT).
     * En caso de no existir registros coincidentes, COALESCE previene retornos nulos
     * garantizando que el total y promedio devuelvan 0 / 0.00 y no null.
     */
    @Query("SELECT new pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto(" +
            "COALESCE(SUM(r.montoTotal), 0), " +
            "COALESCE(AVG(r.montoTotal), 0.0), " +
            "COALESCE(MAX(r.montoTotal), 0), " +
            "COALESCE(MIN(r.montoTotal), 0), " +
            "COUNT(r)) " +
            "FROM RendicionCajaChica r WHERE (:cajaChicaId IS NULL OR r.cajaChica.id = :cajaChicaId)")
    ReporteRendicionesAgregadoDto obtenerReporteAgregado(@Param("cajaChicaId") Long cajaChicaId);
}

