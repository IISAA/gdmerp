export interface AlmacenResponse {
  id: number;
  nombre: string;
  ubicacion?: string;
  capacidad?: number;
}

export type Almacen = AlmacenResponse;
export type AlmacenDTO = AlmacenResponse;

export interface AlmacenRequest {
  nombre: string;
  ubicacion?: string;
  capacidad?: number;
}
