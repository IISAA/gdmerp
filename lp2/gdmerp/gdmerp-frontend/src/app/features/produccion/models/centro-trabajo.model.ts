export interface CentroTrabajo {
  id: number;
  nombre: string;
  capacidad?: number;
  codigo?: string;
  costoHora?: number;
  activo?: boolean;
}

export interface CentroTrabajoRequest {
  nombre: string;
  capacidad?: number;
  codigo?: string;
  costoHora?: number;
}
