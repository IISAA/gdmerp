export type TipoFinanciero = 'INGRESO' | 'EGRESO';
export type EstadoFinanciero = 'PENDIENTE' | 'PAGADO' | 'ANULADO';

export interface CategoriaFinanciera {
  id?: number;
  nombre: string;
  descripcion?: string;
  tipoDefault?: TipoFinanciero;
  activo?: boolean;
}

export interface CategoriaFinancieraResumen {
  id: number;
  nombre: string;
  tipoDefault?: TipoFinanciero;
}
