export interface DetalleListaMaterialesDto {
  id?: number;
  insumoId: number;
  nombreInsumo?: string;
  skuInsumo?: string;
  unidadMedidaInsumo?: string;
  cantidadRequerida: number;
  costoPromedioInsumo?: number;
}

export interface ListaMaterialesDto {
  id?: number;
  codigo: string;
  productoTerminadoId: number;
  nombreProductoTerminado?: string;
  skuProductoTerminado?: string;
  unidadMedidaProductoTerminado?: string;
  cantidadBase: number;
  detalles: DetalleListaMaterialesDto[];
}

export interface DetalleListaMaterialesRequest {
  insumoId: number;
  cantidadRequerida: number;
}

export interface ListaMaterialesRequest {
  codigo: string;
  productoTerminadoId: number;
  cantidadBase: number;
  detalles: DetalleListaMaterialesRequest[];
}

export interface DetalleListaMaterialesResponse {
  id: number;
  insumoId: number;
  nombreInsumo: string;
  skuInsumo: string;
  unidadMedidaInsumo: string;
  cantidadRequerida: number;
  costoPromedioInsumo?: number;
}

export interface ListaMaterialesResponse {
  id: number;
  codigo: string;
  productoTerminadoId: number;
  nombreProductoTerminado: string;
  skuProductoTerminado: string;
  unidadMedidaProductoTerminado?: string;
  cantidadBase: number;
  detalles: DetalleListaMaterialesResponse[];
}
