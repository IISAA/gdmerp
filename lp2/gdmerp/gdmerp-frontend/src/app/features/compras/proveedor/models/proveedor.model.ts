export interface Proveedor {
  id: number;
  ruc: string;
  razonSocial: string;
  telefono?: string;
  email?: string;
  direccion?: string;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  telefono?: string;
  email?: string;
  direccion?: string;
}

export interface RucConsultaResponse {
  success?: boolean;
  message?: string;
  ruc: string;
  razonSocial: string;
  nombreComercial?: string;
  estado?: string;
  condicion?: string;
  direccion?: string;
  departamento?: string;
  provincia?: string;
  distrito?: string;
  ubigeo?: string;
}
