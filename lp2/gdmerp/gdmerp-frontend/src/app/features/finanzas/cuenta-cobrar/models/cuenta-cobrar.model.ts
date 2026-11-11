export type EstadoCredito = 'PENDIENTE' | 'PAGADO_PARCIAL' | 'PAGADO_TOTAL';
export type MetodoPago = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';

export interface CuentaPorCobrar {
  id: number;
  codigo: string;
  clienteNombre: string;
  clienteDocumento?: string | null;
  montoTotal: number;
  saldoPendiente: number;
  montoPagado: number;
  porcentajePagado: number;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: EstadoCredito;
  cantidadAbonos: number;
}

export interface AbonoCredito {
  id: number;
  cuentaPorCobrarId: number;
  sesionCajaId?: number | null;
  monto: number;
  metodoPago: MetodoPago;
  fecha: string;
  nota?: string | null;
}

export interface CuentaPorCobrarDetalle extends CuentaPorCobrar {
  abonos: AbonoCredito[];
}

export interface CrearCreditoRequest {
  codigo?: string | null;
  clienteNombre: string;
  clienteDocumento?: string | null;
  montoTotal: number;
  fechaEmision?: string | null;
  fechaVencimiento?: string | null;
  nota?: string | null;
}

export interface RegistrarAbonoRequest {
  monto: number;
  metodoPago: MetodoPago;
  sesionCajaId?: number | null;
  nota?: string | null;
}
