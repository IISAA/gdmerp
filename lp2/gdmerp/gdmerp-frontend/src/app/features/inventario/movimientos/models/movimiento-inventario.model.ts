export type TipoMovimientoManual =
    | 'ENTRADA'
    | 'SALIDA'
    | 'AJUSTE_POSITIVO'
    | 'AJUSTE_NEGATIVO';

export interface MovimientoInventarioRequest {
    productoId: number;
    tipoMovimiento: TipoMovimientoManual;
    cantidad: number;
    costoUnitario?: number | null;
    referencia: string;
}

export interface MovimientoInventarioResponse {
    id: number;
    productoId: number;
    productoSku: string;
    productoNombre: string;
    unidadMedida: string;
    tipoMovimiento: string;
    cantidad: number;
    stockAnterior: number;
    stockResultante: number;
    costoUnitario: number;
    fechaMovimiento: string; // ISO LocalDateTime desde Spring Boot
    referencia: string;
}

export interface KardexFiltro {
    productoId?: number | null;
    tipoMovimiento?: string | null;
    desde?: string | null; // Formato YYYY-MM-DD
    hasta?: string | null; // Formato YYYY-MM-DD
}