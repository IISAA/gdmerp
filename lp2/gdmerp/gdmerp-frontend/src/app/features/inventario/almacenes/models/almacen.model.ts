export interface Almacen {
  id: number;
  nombre: string;
  ubicacion?: string;
  capacidad?: number;
  activo?: boolean;
}

export interface AlmacenRequest {
  nombre: string;
  ubicacion?: string;
  capacidad?: number;
}
