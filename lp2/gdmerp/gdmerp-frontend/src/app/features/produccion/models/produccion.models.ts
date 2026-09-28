export enum EstadoOrdenProduccion {
  PLANIFICADO = 'PLANIFICADO',
  EN_PROCESO = 'EN_PROCESO',
  COMPLETADO = 'COMPLETADO',
  CANCELADO = 'CANCELADO'
}

export interface DetalleOrdenRequest {
  materiaPrimaId: number;
  cantidadRequerida: number;
}

export interface OrdenProduccionRequest {
  productoTerminadoId: number;
  cantidadPlanificada: number;
  estado: EstadoOrdenProduccion;
  centroTrabajoId: number;
  fechaPlanificadaInicio: string;
  fechaEstimadaFin: string;
  detalles: DetalleOrdenRequest[];
}

export interface DetalleOrdenResponse {
  id: number;
  materiaPrimaId: number;
  nombreMateriaPrima: string;
  cantidadRequerida: number;
  costoUnitario: number;
  subtotal: number;
}

export interface OrdenProduccionResponse {
  id: number;
  productoTerminadoId: number;
  producto: string;
  cantidadPlanificada: number;
  estado: EstadoOrdenProduccion;
  fechaPlanificadaInicio: string;
  fechaEstimadaFin: string;
  loteGenerado?: string | null;
  totalCostoInsumos: number;
  centroTrabajo: {
    id: number;
    nombre: string;
    capacidad: number;
  };
  detalles: DetalleOrdenResponse[];
}
