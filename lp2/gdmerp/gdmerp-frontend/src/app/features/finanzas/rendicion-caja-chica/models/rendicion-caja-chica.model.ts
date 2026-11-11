export type EstadoRendicion = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ANULADO';

export interface CajaChica {
  id: number;
  codigo: string;
  responsable: string;
  limiteMaximo: number;
  saldoDisponible: number;
}

export interface CentroCosto {
  id: number;
  codigoCc: string;
  nombreArea: string;
  activo: boolean;
}

export interface AsignacionCajaCentroCosto {
  cajaChicaId: number;
  centroCostoId: number;
  codigoCc: string;
  nombreArea: string;
  presupuestoAsignado: number;
  saldoDisponible: number;
  fechaAsignacion: string;
  activo: boolean;
}

export interface AsignarPresupuestoRequest {
  centroCostoId: number;
  montoPresupuesto: number;
}

export interface DetalleRendicion {
  id?: number;
  concepto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
  productoId?: number;
  centroCostoId?: number;
  centroCostoCodigo?: string;
  centroCostoNombre?: string;
}

export interface DetalleRendicionRequest {
  concepto: string;
  cantidad: number;
  precioUnitario: number;
  productoId?: number;
  centroCostoId?: number;
}

export interface RendicionCajaChica {
  id: number;
  codigo: string;
  responsable: string;
  fecha: string;
  montoTotal: number;
  estado: EstadoRendicion;
  cajaChicaId: number;
  detalles: DetalleRendicion[];
}

export interface RendicionCajaChicaRequest {
  codigo: string;
  responsable: string;
  cajaChicaId: number;
  detalles: DetalleRendicionRequest[];
}

export interface ReporteRendicionesAgregado {
  totalMonto: number;
  promedioMonto: number;
  montoMaximo: number;
  montoMinimo: number;
  cantidadRendiciones: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
