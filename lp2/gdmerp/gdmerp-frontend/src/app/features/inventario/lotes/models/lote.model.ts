export interface LoteRequest {
    numeroLote: string;
    productoId: number;
    cantidadActual: number;
    fechaProduccion: string;   // Formato ISO: YYYY-MM-DD
    fechaVencimiento: string;  // Formato ISO: YYYY-MM-DD
}

export interface LoteResponse {
    id: number;
    numeroLote: string;
    productoId: number;
    productoNombre: string;
    cantidadActual: number;
    fechaProduccion: string;
    fechaVencimiento: string;
}