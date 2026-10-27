export interface Categoria {
  id: number;
  nombre: string;
  descripcion?: string;
  activo?: boolean;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion?: string;
}
