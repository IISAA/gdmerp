export type CategoriaUnidadMedida = 'MASA' | 'LONGITUD' | 'VOLUMEN' | 'SUPERFICIE' | 'UNIDAD_EMPAQUE';

export interface UnidadMedida {
  id: number;
  codigo: string;
  codigoSunat?: string;
  nombre: string;
  simbolo: string;
  categoria: CategoriaUnidadMedida;
  factorConversion: number;
  factor?: number;
  permiteDecimales: boolean;
  activo: boolean;
}

export interface UnidadMedidaRequest {
  codigo: string;
  codigoSunat?: string;
  nombre: string;
  simbolo: string;
  categoria: CategoriaUnidadMedida;
  factorConversion: number;
  permiteDecimales?: boolean;
  activo?: boolean;
}

export interface GrupoUnidadMedida {
  categoria: CategoriaUnidadMedida;
  nombre: string;
  unidades: UnidadMedida[];
}
