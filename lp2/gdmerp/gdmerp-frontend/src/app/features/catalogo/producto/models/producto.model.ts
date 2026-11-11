import { CategoriaResumen } from '../../categoria/models/categoria.model';

export interface Producto {
  id: number;
  nombre: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  categoria: CategoriaResumen;
}

export interface ProductoRequest {
  nombre: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  categoriaId: number;
}
