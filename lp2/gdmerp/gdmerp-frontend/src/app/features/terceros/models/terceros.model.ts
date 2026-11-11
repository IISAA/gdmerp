export type TipoTercero = 'CLIENTE' | 'PROVEEDOR' | 'AMBOS';

export interface Tercero {
  id: number;
  tipoDocumento: string;
  numeroDocumento: string;
  razonSocial: string;
  tipoTercero: TipoTercero;
  telefono?: string;
  direccion?: string;
  correo?: string;
  activo: boolean;
}

export interface TerceroCreateRequest {
  tipoDocumento: string;
  numeroDocumento: string;
  razonSocial: string;
  tipoTercero: TipoTercero;
  telefono?: string;
  direccion?: string;
  correo?: string;
}

export interface ConsultaDocumentoResponse {
  exito: boolean;
  mensaje?: string;
  razonSocial: string;
  direccion?: string;
  estado?: string;
  condicion?: string;
  distrito?: string;
  provincia?: string;
}
