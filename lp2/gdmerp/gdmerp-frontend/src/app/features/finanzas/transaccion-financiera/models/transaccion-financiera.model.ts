import {
  CategoriaFinancieraResumen,
  EstadoFinanciero,
  TipoFinanciero,
} from '../../categoria-financiera/models/categoria-financiera.model';

export interface TransaccionFinanciera {
  id: number;
  codigo: string;
  concepto: string;
  monto: number;
  fechaTransaccion: string;
  tipo: TipoFinanciero;
  estado: EstadoFinanciero;
  tipoComprobante?: string;
  numeroComprobante?: string;
  categoriaFinanciera: CategoriaFinancieraResumen;
}

export interface TransaccionFinancieraRequest {
  codigo: string;
  concepto: string;
  monto: number;
  fechaTransaccion?: string;
  tipo: TipoFinanciero;
  estado?: EstadoFinanciero;
  tipoComprobante?: string;
  numeroComprobante?: string;
  categoriaFinancieraId: number;
}
