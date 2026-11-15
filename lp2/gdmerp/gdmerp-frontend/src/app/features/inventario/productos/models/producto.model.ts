export type TipoProducto = 'MATERIA_PRIMA' | 'TERMINADO' | 'EMPAQUE' | 'EN_PROCESO' | 'CONSUMIBLE';

export interface CategoriaResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  activo?: boolean;
}

export interface AlmacenResponse {
  id: number;
  nombre: string;
  ubicacion?: string;
}

export interface ProductoResponse {
  id: number;
  sku: string;
  nombre: string;
  descripcion?: string;
  unidadMedida?: string;
  costoBase?: number;
  costoPromedio?: number;
  precio?: number;
  stock?: number;
  stockTotal?: number;
  stockActual?: number;
  stockMinimo?: number;
  almacen?: AlmacenResponse;
  categoria?: CategoriaResponse;
  tipoProducto?: TipoProducto | string;
  imagenUrl?: string;
}

export type Producto = ProductoResponse;

export interface ProductoRequest {
  sku: string;
  nombre: string;
  descripcion?: string;
  tipoProducto: TipoProducto | string;
  categoriaId: number;
  almacenId?: number | null;
  unidadMedida: string;
  precio?: number;
  costoBase?: number;
  costoPromedio?: number;
  stockMinimo: number;
}
