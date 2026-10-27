package pe.edu.upeu.gdmerp.inventario.unidadmedida.dto;

import java.util.List;

/**
 * Resultado de la operación de sincronización del catálogo estándar.
 *
 * @param insertadas  Cantidad de unidades nuevas insertadas en esta ejecución.
 * @param omitidas    Cantidad de unidades que ya existían y fueron omitidas.
 * @param codigosNuevos Lista de códigos efectivamente insertados (vacío si ninguno).
 * @param mensaje     Descripción legible del resultado.
 */
public record SincronizacionEstandaresResponse(
        int insertadas,
        int omitidas,
        List<String> codigosNuevos,
        String mensaje
) {}
