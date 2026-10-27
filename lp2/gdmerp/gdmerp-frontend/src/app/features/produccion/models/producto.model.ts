export type TipoProducto = 'MATERIA_PRIMA' | 'TERMINADO' | 'EMPAQUE' | 'EN_PROCESO' | 'CONSUMIBLE';

export interface Producto {
  id: number;
  sku?: string;
  nombre: string;
  tipoProducto?: TipoProducto | string;
  unidadMedida?: string;
  costoPromedio?: number;
  stockTotal?: number;
  stockMinimo?: number;
}
