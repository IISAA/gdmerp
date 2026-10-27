package pe.edu.upeu.gdmerp.inventario.unidadmedida.service;

import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.SincronizacionEstandaresResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaRequest;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaResponse;

import java.util.List;

public interface UnidadMedidaService {

    /**
     * Lista todas las unidades de medida, opcionalmente filtrando sólo las activas.
     * Si la tabla tiene menos registros que el umbral mínimo definido en
     * {@link pe.edu.upeu.gdmerp.inventario.unidadmedida.CatalogoEstandarUnidades#UMBRAL_MINIMO},
     * dispara automáticamente la sincronización del catálogo estándar antes de retornar.
     */
    List<UnidadMedidaResponse> listar(Boolean soloActivos);

    UnidadMedidaResponse obtenerPorId(Long id);

    UnidadMedidaResponse obtenerPorCodigo(String codigo);

    UnidadMedidaResponse crear(UnidadMedidaRequest request);

    UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request);

    void cambiarEstado(Long id, boolean activo);

    /**
     * Sincronización idempotente del catálogo estándar SI / UN-ECE / SUNAT.
     * <p>
     * Itera unidad por unidad sobre {@link pe.edu.upeu.gdmerp.inventario.unidadmedida.CatalogoEstandarUnidades#getCatalogo()}
     * e inserta solo las que aún no existen en la tabla (verificación por código, case-insensitive).
     * Es seguro llamarlo repetidamente: nunca duplica ni modifica registros existentes.
     * </p>
     *
     * @return {@link SincronizacionEstandaresResponse} con el detalle de insertadas vs. omitidas.
     */
    SincronizacionEstandaresResponse sincronizarEstandares();
}
